package com.example.orbitai.data

import android.content.Context
import java.io.File

object LlmInferenceEngineFactory {

    fun create(context: Context, model: LlmModel, settings: InferenceSettings): LlmInferenceEngine {
        val modelPath = File(context.getExternalFilesDir(null), "models/${model.fileName}").absolutePath
        return when (model.format) {
            ModelFormat.TASK -> MediaPipeTaskEngine(context, modelPath, settings)
            ModelFormat.LITERTLM -> LiteRtLmEngine(context, modelPath, settings)
        }
    }
}
