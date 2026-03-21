package com.example.orbitai.data

import android.content.Context

class ModeInferenceSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("mode_inference_prefs", Context.MODE_PRIVATE)

    private fun key(modeId: String, field: String): String = "${modeId}_$field"

    fun get(modeId: String): InferenceSettings = InferenceSettings(
        temperature = prefs.getFloat(key(modeId, "temperature"), 0.8f),
        topK = prefs.getInt(key(modeId, "topK"), 40),
        topP = prefs.getFloat(key(modeId, "topP"), 0.95f),
        maxDecodedTokens = prefs.getInt(key(modeId, "maxDecodedTokens"), 512),
    )

    fun save(modeId: String, settings: InferenceSettings) {
        prefs.edit()
            .putFloat(key(modeId, "temperature"), settings.temperature)
            .putInt(key(modeId, "topK"), settings.topK)
            .putFloat(key(modeId, "topP"), settings.topP)
            .putInt(key(modeId, "maxDecodedTokens"), settings.maxDecodedTokens)
            .apply()
    }
}