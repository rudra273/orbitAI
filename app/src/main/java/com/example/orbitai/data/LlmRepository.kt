package com.example.orbitai.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class LlmRepository(private val context: Context) {

    private var engine: LlmInferenceEngine? = null
    private var currentModelId: String? = null
    private var currentSettings: InferenceSettings? = null

    fun loadModel(model: LlmModel, settings: InferenceSettings) {
        if (currentModelId == model.id && currentSettings == settings) return

        engine?.close()
        engine = LlmInferenceEngineFactory.create(context, model, settings)

        currentModelId = model.id
        currentSettings = settings
    }

    fun generateResponseStream(prompt: String, maxDecodedTokens: Int): Flow<String> {
        val activeEngine = engine ?: throw IllegalStateException("No model loaded.")
        return activeEngine.generateResponseStream(prompt, maxDecodedTokens)
    }

    fun isModelLoaded(modelId: String, settings: InferenceSettings) =
        currentModelId == modelId && currentSettings == settings && engine != null

    fun close() {
        engine?.close()
        engine = null
        currentModelId = null
    }
}