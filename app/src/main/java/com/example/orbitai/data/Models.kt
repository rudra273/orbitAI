package com.example.orbitai.data

import java.util.UUID

// ── LLM Model descriptor ──────────────────────────────────────────────────────

data class LlmModel(
    val id: String,
    val displayName: String,
    val fileName: String,          // file name inside /data/local/tmp/llm/ or assets
    val description: String,
    val paramCount: String,        // e.g. "1.5B"
)

/** Models bundled / sideloaded via ADB or downloaded by the user. */
val AVAILABLE_MODELS = listOf(
    LlmModel(
        id = "gemma3-1.5b",
        displayName = "Gemma 3 1.5B",
        fileName = "gemma3-1.5b-it-int4.task",
        description = "Fast, lightweight • best for quick tasks",
        paramCount = "1.5B"
    ),
    LlmModel(
        id = "gemma3-4b",
        displayName = "Gemma 3 4B",
        fileName = "gemma3-4b-it-int4.task",
        description = "Balanced quality & speed",
        paramCount = "4B"
    ),
    LlmModel(
        id = "gemma2-2b",
        displayName = "Gemma 2 2B",
        fileName = "gemma2-2b-it-cpu-int8.task",
        description = "Stable CPU model • wide device support",
        paramCount = "2B"
    ),
)

// ── Embedding Model descriptor ────────────────────────────────────────────────

data class EmbeddingModelConfig(
    val id: String,
    val displayName: String,
    val fileName: String,
    val description: String,
    val tokenizerFileName: String? = null,
)

val GECKO_EMBEDDING = EmbeddingModelConfig(
    id               = "gecko-embedding",
    displayName      = "Universal Sentence Encoder",
    fileName         = "universal_sentence_encoder.tflite",
    tokenizerFileName = null,
    description      = "On-device semantic search for RAG · Google USE (~100 MB)",
)

val AVAILABLE_EMBEDDING_MODELS = listOf(GECKO_EMBEDDING)

val EMBEDDING_DOWNLOAD_URLS = mapOf(
    "gecko-embedding" to "https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite",
)

val EMBEDDING_TOKENIZER_URLS = mapOf<String, String>()

// ── Message & Chat ────────────────────────────────────────────────────────────

enum class Role { USER, ASSISTANT }

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val isStreaming: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis(),
)

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val messages: List<Message> = emptyList(),
    val modelId: String = AVAILABLE_MODELS.first().id,
    val createdAt: Long = System.currentTimeMillis(),
)