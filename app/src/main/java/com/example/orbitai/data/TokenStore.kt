package com.example.orbitai.data

import android.content.Context

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("orbitai_prefs", Context.MODE_PRIVATE)

    var huggingFaceToken: String
        get() = prefs.getString("hf_token", "") ?: ""
        set(value) = prefs.edit().putString("hf_token", value.trim()).apply()

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value.trim()).apply()

    var geminiModelName: String
        get() = prefs.getString("gemini_model_name", "") ?: ""
        set(value) = prefs.edit().putString("gemini_model_name", value.trim().lowercase()).apply()

    var lastSelectedModelId: String
        get() = prefs.getString("last_selected_model_id", "") ?: ""
        set(value) = prefs.edit().putString("last_selected_model_id", value.trim()).apply()

    fun hasToken() = huggingFaceToken.isNotBlank()

    fun hasGeminiConfig(): Boolean {
        return geminiApiKey.isNotBlank() &&
            geminiModelName.isNotBlank()
    }
}
