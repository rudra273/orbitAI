package com.example.orbitai.feature.chat

import android.content.Context
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.engine.InferenceInput
import com.example.orbitai.core.engine.LlmInferenceEngine
import com.example.orbitai.core.engine.LlmInferenceEngineFactory
import com.example.orbitai.core.model.LlmModel
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

    fun generateResponseStream(input: InferenceInput, maxDecodedTokens: Int): Flow<String> {
        val activeEngine = engine ?: throw IllegalStateException("No model loaded.")
        return activeEngine.generateResponseStream(input, maxDecodedTokens)
    }

    fun isModelLoaded(modelId: String, settings: InferenceSettings) =
        currentModelId == modelId && currentSettings == settings && engine != null

    fun close() {
        engine?.close()
        engine = null
        currentModelId = null
    }
}