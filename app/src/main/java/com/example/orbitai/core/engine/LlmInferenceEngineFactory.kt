package com.example.orbitai.core.engine

import android.content.Context
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.ModelFormat
import com.example.orbitai.core.model.ModelProvider
import java.io.File

object LlmInferenceEngineFactory {

    fun create(context: Context, model: LlmModel, settings: InferenceSettings): LlmInferenceEngine {
        if (model.provider == ModelProvider.GEMINI) {
            return GeminiApiEngine(context, settings)
        }

        val modelPath = File(context.getExternalFilesDir(null), "models/${model.fileName}").absolutePath
        return when (model.format) {
            ModelFormat.TASK -> MediaPipeTaskEngine(context, modelPath, settings)
            ModelFormat.LITERTLM -> LiteRtLmEngine(context, modelPath, settings)
        }
    }

    /**
     * Creates a [LlmConversationEngine] for multi-turn conversations with tool calling.
     * Currently only available for LiteRT-LM models. Returns null for unsupported formats.
     */
    fun createConversationEngine(
        context: Context,
        model: LlmModel,
        settings: InferenceSettings,
    ): LlmConversationEngine? {
        if (model.provider != ModelProvider.LOCAL || model.format != ModelFormat.LITERTLM) {
            return null
        }
        val modelPath = File(context.getExternalFilesDir(null), "models/${model.fileName}").absolutePath
        return LiteRtLmEngine(context, modelPath, settings)
    }
}
