package com.example.orbitai.core.engine

import android.content.Context
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.MODEL_DOWNLOAD_SPECS
import com.example.orbitai.core.model.formatModelSize
import kotlinx.coroutines.flow.Flow

/**
 * Single repository for model loading and inference, shared by the main app and the bubble
 * through the app container, so at most one model is resident per process.
 * Supports both simple streaming ([LlmInferenceEngine]) and multi-turn conversations ([LlmConversationEngine]).
 *
 * Load/close are serialized. Callers that run inference wrap it in [beginUse]/[endUse] so
 * [releaseIfIdle] never frees a model another feature is using.
 */
class LlmRepository(
    private val context: Context,
    /** Receives "loading <id>", "<id> on <backend>" or null, for diagnostics reports. */
    private val onModelStateChanged: (String?) -> Unit = {},
) {

    private var engine: LlmInferenceEngine? = null
    private var conversationEngine: LlmConversationEngine? = null
    private var currentModelId: String? = null
    private var currentSettings: InferenceSettings? = null
    private var currentBackendKey: String? = null
    private val backendSettings = BackendSettingsStore(context)
    private var activeUses = 0
    private val beforeEngineClose = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()

    /** Runs [listener] just before the loaded engine is closed, so open conversations can be closed first. */
    fun addBeforeEngineCloseListener(listener: () -> Unit) {
        beforeEngineClose += listener
    }

    fun removeBeforeEngineCloseListener(listener: () -> Unit) {
        beforeEngineClose -= listener
    }

    private fun notifyEngineClosing() {
        beforeEngineClose.forEach { runCatching(it) }
    }

    /** Loads [model], reloading when the model, settings or its "Run on" choice changed. [forced] is for the speed test. */
    @Synchronized
    fun loadModel(model: LlmModel, settings: InferenceSettings, forced: HardwareBackend? = null) {
        val backendKey = forced?.let { "forced:${it.name}" } ?: backendSettings.loadKey(model.id)
        if (currentModelId == model.id && currentSettings == settings && currentBackendKey == backendKey && engine != null) return

        if (engine != null) notifyEngineClosing()
        engine?.close()
        // If the engine we just closed was the same object as conversationEngine, avoid double-close.
        if (conversationEngine != null && conversationEngine !== engine) {
            conversationEngine?.close()
        }
        engine = null
        conversationEngine = null
        currentModelId = null
        currentSettings = null
        currentBackendKey = null

        val size = MODEL_DOWNLOAD_SPECS[model.id]?.totalSizeBytes?.let { " (${formatModelSize(it)})" }.orEmpty()
        onModelStateChanged("loading ${model.id}$size")
        val created = LlmInferenceEngineFactory.create(context, model, settings, forced)
        engine = created
        onModelStateChanged("${model.id}$size on ${activeBackendLabel()}")

        // If the engine also supports conversations (LiteRT-LM), expose it.
        conversationEngine = created as? LlmConversationEngine

        currentModelId = model.id
        currentSettings = settings
        currentBackendKey = backendKey
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
        currentModelId == modelId && currentSettings == settings && engine != null &&
            currentBackendKey == backendSettings.loadKey(modelId)

    @Synchronized
    fun beginUse() {
        activeUses++
    }

    @Synchronized
    fun endUse() {
        activeUses = (activeUses - 1).coerceAtLeast(0)
    }

    /** True while a chat or bubble run holds the model. */
    @Synchronized
    fun isInUse(): Boolean = activeUses > 0

    /** Frees the model when no chat or bubble run is using it. Returns true if nothing is loaded afterwards. */
    @Synchronized
    fun releaseIfIdle(): Boolean {
        if (activeUses > 0) return false
        close()
        return true
    }

    /** Hardware backend of the loaded engine (e.g. "GPU", "CPU"), or the engine type for non-LiteRT models. */
    @Synchronized
    fun activeBackendLabel(): String? = when (val current = engine) {
        null -> null
        is LiteRtLmEngine -> current.activeBackend
        is MediaPipeTaskEngine -> current.activeBackend
        is GeminiApiEngine -> "Cloud"
        else -> "CPU"
    }

    @Synchronized
    fun close() {
        if (engine != null) {
            onModelStateChanged(null)
            notifyEngineClosing()
        }
        engine?.close()
        // conversationEngine is the same object as engine for LiteRT, no double-close needed.
        engine = null
        conversationEngine = null
        currentModelId = null
        currentSettings = null
        currentBackendKey = null
    }
}
