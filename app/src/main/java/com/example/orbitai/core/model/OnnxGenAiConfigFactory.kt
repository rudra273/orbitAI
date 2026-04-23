package com.example.orbitai.core.model

import java.io.File

object OnnxGenAiConfigFactory {

    fun ensureConfig(model: LlmModel, modelRoot: File) {
        if (model.format != ModelFormat.ONNX_GENAI) return

        val genAiConfig = File(modelRoot, "genai_config.json")
        require(genAiConfig.exists()) {
            "Missing genai_config.json in ${modelRoot.absolutePath}"
        }
    }
}
