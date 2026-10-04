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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class ChatRepository(context: Context) {

    private val db         = AppDatabase.getInstance(context)
    private val chatDao    = db.chatDao()
    private val messageDao = db.messageDao()
    private val modelDownloader = ModelDownloader(context)
    private val tokenStore = TokenStore(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val streamingMessageIds = MutableStateFlow<Set<String>>(emptySet())

    /** Latest text of replies still streaming; persisted to Room at most every [STREAM_PERSIST_INTERVAL_MS]. */
    private val streamingContent = MutableStateFlow<Map<String, String>>(emptyMap())
    private val lastPersistedAt = ConcurrentHashMap<String, Long>()

    /** Temporary chats live only in memory: never written to Room, gone when discarded or the process dies. */
    private val temporaryChats = MutableStateFlow<Map<String, Chat>>(emptyMap())

    private fun updateTemporaryChat(chatId: String, transform: (Chat) -> Chat) {
        temporaryChats.update { chats -> chats[chatId]?.let { chats + (chatId to transform(it)) } ?: chats }
    }

    private fun temporaryChatIdForMessage(messageId: String): String? =
        temporaryChats.value.values.firstOrNull { chat -> chat.messages.any { it.id == messageId } }?.id

    fun createTemporaryChat(modelId: String = tokenStore.lastSelectedModelId): Chat {
        val chat = Chat(id = TEMPORARY_PREFIX + java.util.UUID.randomUUID(), title = "Temporary chat", modelId = modelId)
        temporaryChats.update { it + (chat.id to chat) }
        return chat
    }

    fun discardTemporaryChat(chatId: String) {
        temporaryChats.update { it - chatId }
    }

    /** Chat list rows; reads only each chat's newest message. */
    val chatSummaries: StateFlow<List<ChatSummary>> = chatDao.observeChatSummaries()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** One chat with its messages, with the streaming reply overlaid while it is generated. */
    fun observeChat(chatId: String): Flow<Chat?> = if (isTemporaryChat(chatId)) {
        combine(temporaryChats, streamingMessageIds) { chats, streamingIds ->
            chats[chatId]?.let { chat -> chat.copy(messages = chat.messages.map { it.copy(isStreaming = it.id in streamingIds) }) }
        }
    } else combine(
        chatDao.observeChat(chatId),
        messageDao.observeMessages(chatId),
        streamingMessageIds,
        streamingContent,
    ) { entity, messages, streamingIds, liveContent ->
        entity?.toDomain(messages)?.let { chat ->
            chat.copy(messages = chat.messages.map {
                it.copy(content = liveContent[it.id] ?: it.content, isStreaming = it.id in streamingIds)
            })
        }
    }

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
        if (isTemporaryChat(chatId)) return@withContext temporaryChats.value[chatId]
        val entity = chatDao.getChatById(chatId) ?: return@withContext null
        val msgs   = messageDao.getMessages(chatId)
        entity.toDomain(msgs)
    }

    suspend fun addMessage(chatId: String, message: Message) = withContext(Dispatchers.IO) {
        if (isTemporaryChat(chatId)) {
            updateTemporaryChat(chatId) { it.copy(messages = it.messages + message.copy(isStreaming = false)) }
            if (message.isStreaming) streamingMessageIds.update { it + message.id }
            return@withContext
        }
        messageDao.insertMessage(message.toEntity(chatId))
        if (message.isStreaming) streamingMessageIds.update { it + message.id }
        // Auto-title from first user message
        if (message.role == Role.USER) {
            val chat = chatDao.getChatById(chatId)
            if (chat != null && chat.title == "New Chat" && !chat.isTitleLocked) {
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
            temporaryChatIdForMessage(messageId)?.let { chatId ->
                updateTemporaryChat(chatId) { chat ->
                    chat.copy(messages = chat.messages.map { if (it.id == messageId) it.copy(content = newContent) else it })
                }
                streamingMessageIds.update { if (isStreaming) it + messageId else it - messageId }
                return@withContext
            }
            if (isStreaming) {
                streamingMessageIds.update { it + messageId }
                streamingContent.update { it + (messageId to newContent) }
                val now = System.currentTimeMillis()
                if (now - (lastPersistedAt[messageId] ?: 0L) >= STREAM_PERSIST_INTERVAL_MS) {
                    lastPersistedAt[messageId] = now
                    messageDao.updateMessageContentById(messageId, newContent)
                }
            } else {
                // Persist first so the list never shows stale text between the write and the overlay removal.
                messageDao.updateMessageContentById(messageId, newContent)
                lastPersistedAt.remove(messageId)
                streamingContent.update { it - messageId }
                streamingMessageIds.update { it - messageId }
            }
        }

    suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        temporaryChatIdForMessage(messageId)?.let { chatId ->
            updateTemporaryChat(chatId) { chat -> chat.copy(messages = chat.messages.filterNot { it.id == messageId }) }
            return@withContext
        }
        messageDao.deleteMessage(messageId)
    }

    /** Removes [message] and every later message in the chat (used by regenerate and edit & resend). */
    suspend fun deleteMessagesFrom(chatId: String, message: Message) = withContext(Dispatchers.IO) {
        if (isTemporaryChat(chatId)) {
            updateTemporaryChat(chatId) { chat -> chat.copy(messages = chat.messages.filter { it.timestampMs < message.timestampMs }) }
            return@withContext
        }
        messageDao.deleteMessagesFrom(chatId, message.timestampMs)
    }

    /** Removes every message after [message] in the chat, keeping [message] itself. */
    suspend fun deleteMessagesAfter(chatId: String, message: Message) = withContext(Dispatchers.IO) {
        if (isTemporaryChat(chatId)) {
            updateTemporaryChat(chatId) { chat -> chat.copy(messages = chat.messages.filter { it.timestampMs <= message.timestampMs }) }
            return@withContext
        }
        messageDao.deleteMessagesFrom(chatId, message.timestampMs + 1)
    }

    suspend fun updateMessageContent(messageId: String, content: String) = withContext(Dispatchers.IO) {
        temporaryChatIdForMessage(messageId)?.let { chatId ->
            updateTemporaryChat(chatId) { chat ->
                chat.copy(messages = chat.messages.map { if (it.id == messageId) it.copy(content = content) else it })
            }
            return@withContext
        }
        messageDao.updateMessageContentById(messageId, content)
    }

    /** Renames a chat; later automatic titles won't replace the name. */
    suspend fun renameChat(chatId: String, title: String) = withContext(Dispatchers.IO) {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty() && !isTemporaryChat(chatId)) chatDao.rename(chatId, trimmed)
    }

    suspend fun setPinned(chatIds: List<String>, pinned: Boolean) = withContext(Dispatchers.IO) {
        chatDao.setPinned(chatIds, pinned)
    }

    suspend fun setArchived(chatIds: List<String>, archived: Boolean) = withContext(Dispatchers.IO) {
        chatDao.setArchived(chatIds, archived)
    }

    /** Full-text search over all saved messages, newest first. */
    suspend fun searchMessages(query: String, limit: Int = 50): List<MessageSearchHit> = withContext(Dispatchers.IO) {
        val match = ChatSearch.matchQuery(query) ?: return@withContext emptyList()
        chatDao.searchMessages(match, limit).map {
            MessageSearchHit(it.messageId, it.chatId, it.chatTitle, it.snippet, it.timestampMs)
        }
    }

    suspend fun deleteChat(chatId: String) = withContext(Dispatchers.IO) {
        if (isTemporaryChat(chatId)) {
            discardTemporaryChat(chatId)
            return@withContext
        }
        chatDao.deleteChat(chatId) // CASCADE deletes messages too
    }

    suspend fun updateChatModel(chatId: String, modelId: String) = withContext(Dispatchers.IO) {
        if (isTemporaryChat(chatId)) {
            updateTemporaryChat(chatId) { it.copy(modelId = modelId) }
            return@withContext
        }
        chatDao.updateModelId(chatId, modelId)
    }

    companion object {
        private const val STREAM_PERSIST_INTERVAL_MS = 500L
        private const val TEMPORARY_PREFIX = "temp-"

        fun isTemporaryChat(chatId: String): Boolean = chatId.startsWith(TEMPORARY_PREFIX)
    }
}
