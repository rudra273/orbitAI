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
)

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val messages: List<Message> = emptyList(),
    val modelId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
