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
    /** Conservative prompt + output token budget used to trim history (see PromptBudget). */
    val contextTokens: Int = 2048,
    /** Phone chips (Build.SOC_MODEL, e.g. "SM8750") this file is compiled for; empty = no NPU build. */
    val npuSocModels: Set<String> = emptySet(),
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
        id = "gemma3-1b-litertlm",
        displayName = "Gemma 3 1B · LiteRT",
        fileName = "gemma3-1b-it-int4.litertlm",
        description = "Compact text model for chat and Orbit",
        paramCount = "1B",
        format = ModelFormat.LITERTLM,
    ),
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
        contextTokens = 4096,
    ),
    LlmModel(
        id = "gemma3-2b",
        displayName = "Gemma 3n E2B",
        fileName = "gemma-3n-E2B-it-int4.litertlm",
        description = "Fast experimental model with vision support",
        paramCount = "2B",
        format = ModelFormat.LITERTLM,
        supportsVision = true,
        contextTokens = 4096,
    ),
    LlmModel(
        id = "gemma4-e2b",
        displayName = "Gemma 4 E2B",
        fileName = "gemma-4-E2B-it-int4.litertlm",
        description = "Gemma 4 · compact multimodal model",
        paramCount = "2B",
        format = ModelFormat.LITERTLM,
        supportsVision = true,
        contextTokens = 4096,
    ),
    LlmModel(
        id = "gemma4-e4b",
        displayName = "Gemma 4 E4B",
        fileName = "gemma-4-E4B-it-int4.litertlm",
        description = "Gemma 4 · balanced multimodal model",
        paramCount = "4B",
        format = ModelFormat.LITERTLM,
        supportsVision = true,
        contextTokens = 4096,
    ),
    LlmModel(
        id = "qwen35-0.8b",
        displayName = "Qwen 3.5 0.8B",
        fileName = "Qwen3.5-0.8B_mixed48_h1024.litertlm",
        description = "Qwen 3.5 · very small and fast, text only",
        paramCount = "0.8B",
        format = ModelFormat.LITERTLM,
        promptStyle = PromptStyle.QWEN,
        contextTokens = 4096,
    ),
    LlmModel(
        id = "qwen35-0.8b-vl",
        displayName = "Qwen 3.5 0.8B Vision",
        fileName = "Qwen3.5-0.8B-VL_int8.litertlm",
        description = "Qwen 3.5 · small model that also understands images",
        paramCount = "0.8B",
        format = ModelFormat.LITERTLM,
        promptStyle = PromptStyle.QWEN,
        supportsVision = true,
        contextTokens = 4096,
    ),
    LlmModel(
        id = "qwen35-2b",
        displayName = "Qwen 3.5 2B",
        fileName = "Qwen3.5-2B_int8.litertlm",
        description = "Qwen 3.5 · balanced size and quality, text only",
        paramCount = "2B",
        format = ModelFormat.LITERTLM,
        promptStyle = PromptStyle.QWEN,
        contextTokens = 4096,
    ),
    LlmModel(
        id = "qwen35-2b-vl",
        displayName = "Qwen 3.5 2B Vision",
        fileName = "Qwen3.5-2B-VL_int8.litertlm",
        description = "Qwen 3.5 · balanced model that also understands images",
        paramCount = "2B",
        format = ModelFormat.LITERTLM,
        promptStyle = PromptStyle.QWEN,
        supportsVision = true,
        contextTokens = 4096,
    ),
    LlmModel(
        id = "qwen35-4b",
        displayName = "Qwen 3.5 4B",
        fileName = "Qwen3.5-4B_mixed_int4_gpu_composite_v0200.litertlm",
        description = "Qwen 3.5 · strongest Qwen for phones, text only",
        paramCount = "4B",
        format = ModelFormat.LITERTLM,
        promptStyle = PromptStyle.QWEN,
        contextTokens = 4096,
    ),
    LlmModel(
        id = "gemma2-2b",
        displayName = "Gemma 2 2B",
        fileName = "gemma2-2b-it-cpu-int8.task",
        description = "Stable CPU model • wide device support",
        paramCount = "2B",
        contextTokens = 1280,  // KV cache size from the file name (ekv1280)
    ),
    LlmModel(
        id = "onnx-gemma3-4b-it",
        displayName = "Gemma 3 4B-IT",
        fileName = "onnx/gemma-3-4b-it",
        description = "ONNX GenAI · multimodal Gemma 3 for text + image chat",
        paramCount = "4B",
        format = ModelFormat.ONNX_GENAI,
        supportsVision = true,
        contextTokens = 4096,
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
        contextTokens = 4096,
    ),
    LlmModel(
        id = "onnx-phi4-mini",
        displayName = "Phi-4 Mini",
        fileName = "onnx/phi-4-mini-instruct",
        description = "ONNX GenAI · mobile-optimized Phi-4 Mini",
        paramCount = "3.8B",
        format = ModelFormat.ONNX_GENAI,
        promptStyle = PromptStyle.PHI,
        contextTokens = 4096,
    ),
    LlmModel(
        id = "onnx-phi4",
        displayName = "Phi-4",
        fileName = "onnx/phi-4",
        description = "ONNX GenAI · higher quality Phi-4 mobile build",
        paramCount = "14B",
        format = ModelFormat.ONNX_GENAI,
        promptStyle = PromptStyle.PHI,
        contextTokens = 4096,
    ),
    LlmModel(
        id = "onnx-llama3.2-3b",
        displayName = "Llama 3.2 3B",
        fileName = "onnx/llama-3.2-3b-instruct",
        description = "ONNX GenAI · public Llama 3.2 mobile build",
        paramCount = "3B",
        format = ModelFormat.ONNX_GENAI,
        promptStyle = PromptStyle.LLAMA3,
        contextTokens = 4096,
    ),
)

// ── Embedding Model descriptor ────────────────────────────────────────────────

data class EmbeddingModelConfig(
    val id: String,
    val displayName: String,
    val fileName: String,
    val description: String,
    val tokenizerFileName: String? = null,
    val sizeBytes: Long? = null,
)

val GECKO_EMBEDDING = EmbeddingModelConfig(
    id               = "gecko-embedding",
    sizeBytes        = 6_120_274L,
    displayName      = "Universal Sentence Encoder",
    fileName         = "universal_sentence_encoder.tflite",
    tokenizerFileName = null,
    description      = "On-device semantic search for RAG · Google USE",
)

/** multilingual-e5-small: ungated (MIT), multilingual, int8 ONNX. Files pinned to a verified revision. */
val E5_SMALL_EMBEDDING = EmbeddingModelConfig(
    id                = "multilingual-e5-small",
    sizeBytes         = 118_054_593L + 17_082_730L,
    displayName       = "Multilingual E5 Small",
    fileName          = "multilingual-e5-small-int8.onnx",
    tokenizerFileName = "multilingual-e5-small-tokenizer.json",
    description       = "Better search for Spaces and Memory, includes Hindi · MIT license, no sign-in",
)

/** EmbeddingGemma 2 Text 270M: ungated (Apache-2.0), 100+ languages, LiteRT-LM. File pinned to a verified revision. */
val EMBEDDINGGEMMA2_EMBEDDING = EmbeddingModelConfig(
    id          = "embeddinggemma-2-270m",
    sizeBytes   = 164_626_432L,
    displayName = "EmbeddingGemma 2",
    fileName    = "embeddinggemma-2-text-270m.litertlm",
    description = "Best search for Spaces and Memory, 100+ languages incl. Hindi · Apache-2.0, no sign-in",
)

val AVAILABLE_EMBEDDING_MODELS = listOf(GECKO_EMBEDDING, E5_SMALL_EMBEDDING, EMBEDDINGGEMMA2_EMBEDDING)

val EMBEDDING_DOWNLOAD_URLS = mapOf(
    "embeddinggemma-2-270m" to "https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm/resolve/9be6e8b90982095dc05c2bd162e4b954ee4dbac7/embeddinggemma-2-text-270m.litertlm",
    "gecko-embedding" to "https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite",
    "multilingual-e5-small" to "https://huggingface.co/Xenova/multilingual-e5-small/resolve/761b726dd34fb83930e26aab4e9ac3899aa1fa78/onnx/model_int8.onnx",
)

val EMBEDDING_TOKENIZER_URLS = mapOf(
    "multilingual-e5-small" to "https://huggingface.co/Xenova/multilingual-e5-small/resolve/761b726dd34fb83930e26aab4e9ac3899aa1fa78/tokenizer.json",
)
