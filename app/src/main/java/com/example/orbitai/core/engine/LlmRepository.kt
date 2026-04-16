package com.example.orbitai.core.engine

import android.content.Context
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.ModelFormat
import com.example.orbitai.core.model.ModelProvider
import kotlinx.coroutines.flow.Flow

/**
 * Single repository for model loading and inference, used by both the main app and bubble.
 * Supports both simple streaming ([LlmInferenceEngine]) and multi-turn conversations ([LlmConversationEngine]).
 */
class LlmRepository(private val context: Context) {

    private var engine: LlmInferenceEngine? = null
    private var conversationEngine: LlmConversationEngine? = null
    private var currentModelId: String? = null
    private var currentSettings: InferenceSettings? = null

    fun loadModel(model: LlmModel, settings: InferenceSettings) {
        if (currentModelId == model.id && currentSettings == settings) return

        engine?.close()
        // If the engine we just closed was the same object as conversationEngine, avoid double-close.
        if (conversationEngine != null && conversationEngine !== engine) {
            conversationEngine?.close()
        }

        val created = LlmInferenceEngineFactory.create(context, model, settings)
        engine = created

        // If the engine also supports conversations (LiteRT-LM), expose it.
        conversationEngine = created as? LlmConversationEngine

        currentModelId = model.id
        currentSettings = settings
    }

    fun generateResponseStream(input: InferenceInput, maxDecodedTokens: Int): Flow<String> {
        val activeEngine = engine ?: throw IllegalStateException("No model loaded.")
        return activeEngine.generateResponseStream(input, maxDecodedTokens)
    }

    /**
     * Returns the conversation engine if the currently loaded model supports it (LiteRT-LM).
     * Returns null for Gemini API and MediaPipe Task models.
     */
    fun conversationEngine(): LlmConversationEngine? = conversationEngine

    /** Whether the currently loaded model supports multi-turn conversations. */
    fun supportsConversations(): Boolean = conversationEngine != null

    fun isModelLoaded(modelId: String, settings: InferenceSettings) =
        currentModelId == modelId && currentSettings == settings && engine != null

    fun close() {
        engine?.close()
        // conversationEngine is the same object as engine for LiteRT, no double-close needed.
        engine = null
        conversationEngine = null
        currentModelId = null
    }
}