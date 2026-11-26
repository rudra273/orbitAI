package com.example.orbitai.feature.chat

import java.util.UUID

enum class Role { USER, ASSISTANT }

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val modeName: String? = null,
    val imageUris: List<String> = emptyList(),
    val isStreaming: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis(),
    /** Memories that were sent with this reply (shown as "Memory used"). */
    val usedMemories: List<String> = emptyList(),
    /** Spaces excerpts sent with this reply, numbered as the model saw them. */
    val sources: List<MessageSource> = emptyList(),
)

/** A document excerpt an answer was given (f033). */
data class MessageSource(
    val rank: Int,
    val chunkId: String,
    val docId: String,
    val docName: String,
    val page: Int?,
    val excerpt: String,
    /** False once the document was removed from its Space. */
    val available: Boolean = true,
)

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val messages: List<Message> = emptyList(),
    val modelId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** Lightweight chat list entry (no message bodies except a preview of the newest one). */
data class ChatSummary(
    val id: String,
    val title: String,
    val modelId: String,
    val createdAt: Long,
    val lastMessagePreview: String?,
    val lastMessageAt: Long?,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
)
