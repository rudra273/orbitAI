package com.example.orbitai.core.engine

import android.content.Context
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.model.LlmModel
import kotlinx.coroutines.flow.Flow

/**
 * Single repository for model loading and inference, shared by the main app and the bubble
 * through the app container, so at most one model is resident per process.
 * Supports both simple streaming ([LlmInferenceEngine]) and multi-turn conversations ([LlmConversationEngine]).
 *
 * Load/close are serialized. Callers that run inference wrap it in [beginUse]/[endUse] so
 * [releaseIfIdle] never frees a model another feature is using.
 */
class LlmRepository(private val context: Context) {

    private var engine: LlmInferenceEngine? = null
    private var conversationEngine: LlmConversationEngine? = null
    private var currentModelId: String? = null
    private var currentSettings: InferenceSettings? = null
    private var activeUses = 0

    @Synchronized
    fun loadModel(model: LlmModel, settings: InferenceSettings) {
        if (currentModelId == model.id && currentSettings == settings && engine != null) return

        engine?.close()
        // If the engine we just closed was the same object as conversationEngine, avoid double-close.
        if (conversationEngine != null && conversationEngine !== engine) {
            conversationEngine?.close()
        }
        engine = null
        conversationEngine = null
        currentModelId = null
        currentSettings = null

        val created = LlmInferenceEngineFactory.create(context, model, settings)
        engine = created

        // If the engine also supports conversations (LiteRT-LM), expose it.
        conversationEngine = created as? LlmConversationEngine

        currentModelId = model.id
        currentSettings = settings
    }

    fun generateResponseStream(input: InferenceInput, maxDecodedTokens: Int): Flow<String> {
        val activeEngine = synchronized(this) { engine } ?: throw IllegalStateException("No model loaded.")
        return activeEngine.generateResponseStream(input, maxDecodedTokens)
    }

    /**
     * Returns the conversation engine if the currently loaded model supports it (LiteRT-LM).
     * Returns null for Gemini API and MediaPipe Task models.
     */
    @Synchronized
    fun conversationEngine(): LlmConversationEngine? = conversationEngine

    /** Whether the currently loaded model supports multi-turn conversations. */
    @Synchronized
    fun supportsConversations(): Boolean = conversationEngine != null

    @Synchronized
    fun isModelLoaded(modelId: String, settings: InferenceSettings) =
        currentModelId == modelId && currentSettings == settings && engine != null

    @Synchronized
    fun beginUse() {
        activeUses++
    }

    @Synchronized
    fun endUse() {
        activeUses = (activeUses - 1).coerceAtLeast(0)
    }

    /** Frees the model when no chat or bubble run is using it. Returns true if nothing is loaded afterwards. */
    @Synchronized
    fun releaseIfIdle(): Boolean {
        if (activeUses > 0) return false
        close()
        return true
    }

    @Synchronized
    fun close() {
        engine?.close()
        // conversationEngine is the same object as engine for LiteRT, no double-close needed.
        engine = null
        conversationEngine = null
        currentModelId = null
        currentSettings = null
    }
}
