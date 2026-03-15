package com.example.orbitai.data

import android.content.Context

data class InferenceSettings(
    val temperature: Float = 0.8f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val maxDecodedTokens: Int = 512,
)

class InferenceSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("inference_prefs", Context.MODE_PRIVATE)

    var temperature: Float
        get() = prefs.getFloat("temperature", 0.8f)
        set(v) { prefs.edit().putFloat("temperature", v).apply() }

    var topK: Int
        get() = prefs.getInt("topK", 40)
        set(v) { prefs.edit().putInt("topK", v).apply() }

    var topP: Float
        get() = prefs.getFloat("topP", 0.95f)
        set(v) { prefs.edit().putFloat("topP", v).apply() }

    var maxDecodedTokens: Int
        get() = prefs.getInt("maxDecodedTokens", 512)
        set(v) { prefs.edit().putInt("maxDecodedTokens", v).apply() }

    fun get() = InferenceSettings(temperature, topK, topP, maxDecodedTokens)

    fun save(settings: InferenceSettings) {
        temperature = settings.temperature
        topK = settings.topK
        topP = settings.topP
        maxDecodedTokens = settings.maxDecodedTokens
    }
}
