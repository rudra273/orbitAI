package com.example.orbitai.core.engine

import android.content.Context
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.ModelFormat
import com.example.orbitai.core.model.ModelProvider
import java.io.File

object LlmInferenceEngineFactory {

    /** [forced] runs on that backend only (speed test); otherwise the model's saved "Run on" choice applies. */
    fun create(context: Context, model: LlmModel, settings: InferenceSettings, forced: HardwareBackend? = null): LlmInferenceEngine {
        if (model.provider == ModelProvider.GEMINI) {
            return GeminiApiEngine(context, settings)
        }

        val modelPath = resolveModelPath(context, model)
        return when (model.format) {
            ModelFormat.TASK -> MediaPipeTaskEngine(context, modelPath, settings, plan(context, model, forced))
            ModelFormat.LITERTLM -> LiteRtLmEngine(context, modelPath, settings, plan(context, model, forced), model.supportsVision)
            ModelFormat.ONNX_GENAI -> OnnxGenAiEngine(context, model, settings)
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
        val modelPath = resolveModelPath(context, model)
        return LiteRtLmEngine(context, modelPath, settings, plan(context, model, null), model.supportsVision)
    }

    private fun plan(context: Context, model: LlmModel, forced: HardwareBackend?): BackendPlan =
        if (forced != null) BackendPlan(supportedBackends(model), BackendPreference.of(forced))
        else BackendSettingsStore(context).plan(model)

    private fun resolveModelPath(context: Context, model: LlmModel): String {
        val modelRoot = File(context.getExternalFilesDir(null), "models/${model.fileName}")
        if (model.format != ModelFormat.ONNX_GENAI && modelRoot.isDirectory) {
            val nestedLegacyFile = File(modelRoot, modelRoot.name)
            if (nestedLegacyFile.isFile) {
                return nestedLegacyFile.absolutePath
            }
            throw IllegalStateException(
                "Model path is a directory, not a file: ${modelRoot.absolutePath}. Delete and redownload ${model.displayName}."
            )
        }
        if (model.format != ModelFormat.ONNX_GENAI && !modelRoot.isFile) {
            throw IllegalStateException(
                "Model file missing: ${modelRoot.absolutePath}. Delete and redownload ${model.displayName}."
            )
        }
        return modelRoot.absolutePath
    }
}
