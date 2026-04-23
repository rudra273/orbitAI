package com.example.orbitai.core.model

// ── LLM Model descriptor ────────────────────────────────────────────────

data class LlmModel(
    val id: String,
    val displayName: String,
    val fileName: String,          // relative file or folder path inside externalFiles/models
    val description: String,
    val paramCount: String,        // e.g. "1.5B"
    val format: ModelFormat = ModelFormat.TASK,
    val provider: ModelProvider = ModelProvider.LOCAL,
    val promptStyle: PromptStyle = PromptStyle.GEMMA,
    val supportsVision: Boolean = false,
    val isChatSelectable: Boolean = true,
)

enum class ModelProvider {
    LOCAL,
    GEMINI,
}

enum class ModelFormat {
    TASK,
    LITERTLM,
    ONNX_GENAI,
}

enum class PromptStyle {
    GEMMA,
    PHI,
    LLAMA3,
    QWEN,
}

/** Models bundled / sideloaded via ADB or downloaded by the user. */
val AVAILABLE_MODELS = listOf(
    LlmModel(
        id = "gemma3-1b",
        displayName = "Gemma 3 1B",
        fileName = "gemma3-1b-it-int4.task",
        description = "Fast, lightweight • best for quick tasks",
        paramCount = "1B",
    ),
    LlmModel(
        id = "gemma3-4b",
        displayName = "Gemma 3n E4B",
        fileName = "gemma-3n-E4B-it-int4.litertlm",
        description = "Balanced quality & speed",
        paramCount = "4B",
        format = ModelFormat.LITERTLM,
    ),
    LlmModel(
        id = "gemma3-2b",
        displayName = "Gemma 3n E2B",
        fileName = "gemma-3n-E2B-it-int4.litertlm",
        description = "Fast experimental model with vision support",
        paramCount = "2B",
        format = ModelFormat.LITERTLM,
        supportsVision = true,
    ),
    LlmModel(
        id = "gemma4-e2b",
        displayName = "Gemma 4 E2B",
        fileName = "gemma-4-E2B-it-int4.litertlm",
        description = "Gemma 4 · compact multimodal model",
        paramCount = "2B",
        format = ModelFormat.LITERTLM,
        supportsVision = true,
    ),
    LlmModel(
        id = "gemma4-e4b",
        displayName = "Gemma 4 E4B",
        fileName = "gemma-4-E4B-it-int4.litertlm",
        description = "Gemma 4 · balanced multimodal model",
        paramCount = "4B",
        format = ModelFormat.LITERTLM,
        supportsVision = true,
    ),
    LlmModel(
        id = "gemma2-2b",
        displayName = "Gemma 2 2B",
        fileName = "gemma2-2b-it-cpu-int8.task",
        description = "Stable CPU model • wide device support",
        paramCount = "2B",
    ),
    LlmModel(
        id = "onnx-gemma3-4b-it",
        displayName = "Gemma 3 4B-IT",
        fileName = "onnx/gemma-3-4b-it",
        description = "ONNX GenAI · multimodal Gemma 3 for text + image chat",
        paramCount = "4B",
        format = ModelFormat.ONNX_GENAI,
        supportsVision = true,
    ),
    LlmModel(
        id = "onnx-gemma3-270m-it",
        displayName = "Gemma 3 270M",
        fileName = "onnx/gemma-3-270m-it",
        description = "ONNX GenAI · compact Android-ready Gemma 3 build",
        paramCount = "270M",
        format = ModelFormat.ONNX_GENAI,
    ),
    LlmModel(
        id = "onnx-phi3-mini-4k",
        displayName = "Phi-3 Mini 4K",
        fileName = "onnx/phi-3-mini-4k-instruct",
        description = "ONNX GenAI · compact Phi-3 instruction model",
        paramCount = "3.8B",
        format = ModelFormat.ONNX_GENAI,
        promptStyle = PromptStyle.PHI,
    ),
    LlmModel(
        id = "onnx-phi4-mini",
        displayName = "Phi-4 Mini",
        fileName = "onnx/phi-4-mini-instruct",
        description = "ONNX GenAI · mobile-optimized Phi-4 Mini",
        paramCount = "3.8B",
        format = ModelFormat.ONNX_GENAI,
        promptStyle = PromptStyle.PHI,
    ),
    LlmModel(
        id = "onnx-phi4",
        displayName = "Phi-4",
        fileName = "onnx/phi-4",
        description = "ONNX GenAI · higher quality Phi-4 mobile build",
        paramCount = "14B",
        format = ModelFormat.ONNX_GENAI,
        promptStyle = PromptStyle.PHI,
    ),
    LlmModel(
        id = "onnx-llama3.2-3b",
        displayName = "Llama 3.2 3B",
        fileName = "onnx/llama-3.2-3b-instruct",
        description = "ONNX GenAI · public Llama 3.2 mobile build",
        paramCount = "3B",
        format = ModelFormat.ONNX_GENAI,
        promptStyle = PromptStyle.LLAMA3,
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
