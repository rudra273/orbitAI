package com.example.orbitai.feature.chat

import android.content.Context
import com.example.orbitai.core.database.AppDatabase
import com.example.orbitai.core.database.toDomain
import com.example.orbitai.core.database.toEntity
import com.example.orbitai.core.common.TokenStore
import com.example.orbitai.core.model.ModelDownloader
import com.example.orbitai.core.model.availableChatModels
import com.example.orbitai.feature.chat.Chat
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class ChatRepository(context: Context) {

    private val db         = AppDatabase.getInstance(context)
    private val chatDao    = db.chatDao()
    private val messageDao = db.messageDao()
    private val modelDownloader = ModelDownloader(context)
    private val tokenStore = TokenStore(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Live list of all chats with messages.
     * Room's @Transaction + @Relation emits a new list whenever chats OR messages change.
     */
    private val streamingMessageIds = MutableStateFlow<Set<String>>(emptySet())

    val chats: StateFlow<List<Chat>> = combine(
        chatDao.observeAllChatsWithMessages(), streamingMessageIds,
    ) { list, streamingIds ->
        list.map { entity ->
            val chat = entity.toDomain()
            chat.copy(messages = chat.messages.map { it.copy(isStreaming = it.id in streamingIds) })
        }
    }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun createChat(modelId: String? = null): Chat =
        withContext(Dispatchers.IO) {
            val availableModels = availableChatModels(modelDownloader, tokenStore)
            val lastSelectedModelId = tokenStore.lastSelectedModelId
            val resolvedModelId = modelId
                ?: lastSelectedModelId.takeIf { preferredId ->
                    preferredId.isNotBlank() && availableModels.any { it.id == preferredId }
                }
                ?: ""

            val chat = Chat(modelId = resolvedModelId)
            chatDao.insertChat(chat.toEntity())
            chat
        }

    suspend fun findReusableEmptyChatId(): String? = withContext(Dispatchers.IO) {
        chatDao.getLatestEmptyChatWithoutUserMessages()?.id
    }

    suspend fun getChat(chatId: String): Chat? = withContext(Dispatchers.IO) {
        val entity = chatDao.getChatById(chatId) ?: return@withContext null
        val msgs   = messageDao.getMessages(chatId)
        entity.toDomain(msgs)
    }

    suspend fun addMessage(chatId: String, message: Message) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message.toEntity(chatId))
        if (message.isStreaming) streamingMessageIds.update { it + message.id }
        // Auto-title from first user message
        if (message.role == Role.USER) {
            val chat = chatDao.getChatById(chatId)
            if (chat != null && chat.title == "New Chat") {
                val title = message.content.take(40).let {
                    if (message.content.length > 40) "$it…" else it
                }
                chatDao.updateTitle(chatId, title)
            }
        }
    }

    suspend fun updateLastMessage(chatId: String, newContent: String, isStreaming: Boolean) =
        withContext(Dispatchers.IO) {
            messageDao.updateLastMessageContent(chatId, newContent)
        }

    suspend fun updateMessage(messageId: String, newContent: String, isStreaming: Boolean) =
        withContext(Dispatchers.IO) {
            messageDao.updateMessageContentById(messageId, newContent)
            streamingMessageIds.update { if (isStreaming) it + messageId else it - messageId }
        }

    suspend fun deleteChat(chatId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteChat(chatId) // CASCADE deletes messages too
    }

    suspend fun updateChatModel(chatId: String, modelId: String) = withContext(Dispatchers.IO) {
        chatDao.updateModelId(chatId, modelId)
    }
}
