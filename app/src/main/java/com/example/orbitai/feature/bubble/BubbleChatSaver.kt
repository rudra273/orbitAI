package com.example.orbitai.feature.bubble

import android.content.Context
import com.example.orbitai.appContainer
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role

/** Writes a bubble session into chat history, as one chat that later exchanges are appended to. */
class BubbleChatSaver(private val context: Context) {

    private var chatId: String? = null
    private var savedCount = 0

    /** Saves the exchanges not yet stored and returns the chat's id, or null when there is nothing to save. */
    suspend fun save(exchanges: List<BubbleConversation.Exchange>, modelId: String): String? {
        if (exchanges.isEmpty()) return chatId
        val repo = context.appContainer.chatRepository
        val id = chatId ?: repo.createChat(modelId).also {
            repo.renameChat(it.id, "From Orbit Bubble: " + exchanges.first().question.replace(Regex("\\s+"), " ").take(40))
            chatId = it.id
        }.id
        exchanges.drop(savedCount).forEach {
            repo.addMessage(id, Message(role = Role.USER, content = it.question))
            repo.addMessage(id, Message(role = Role.ASSISTANT, content = it.answer))
        }
        savedCount = exchanges.size
        return id
    }

    /** A new bubble session starts a new chat. */
    fun reset() {
        chatId = null
        savedCount = 0
    }
}
