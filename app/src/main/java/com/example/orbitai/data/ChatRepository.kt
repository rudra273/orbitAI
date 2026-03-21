package com.example.orbitai.data

import android.content.Context
import com.example.orbitai.data.db.AppDatabase
import com.example.orbitai.data.db.toDomain
import com.example.orbitai.data.db.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class ChatRepository(context: Context) {

    private val db         = AppDatabase.getInstance(context)
    private val chatDao    = db.chatDao()
    private val messageDao = db.messageDao()
    private val modelDownloader = ModelDownloader(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Live list of all chats with messages.
     * Room's @Transaction + @Relation emits a new list whenever chats OR messages change.
     */
    val chats: StateFlow<List<Chat>> = chatDao.observeAllChatsWithMessages()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun createChat(modelId: String? = null): Chat =
        withContext(Dispatchers.IO) {
            val resolvedModelId = modelId
                ?: AVAILABLE_MODELS.firstOrNull { modelDownloader.isDownloaded(it) }?.id
                ?: AVAILABLE_MODELS.first().id

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
        }

    suspend fun deleteChat(chatId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteChat(chatId) // CASCADE deletes messages too
    }

    suspend fun updateChatModel(chatId: String, modelId: String) = withContext(Dispatchers.IO) {
        chatDao.updateModelId(chatId, modelId)
    }
}