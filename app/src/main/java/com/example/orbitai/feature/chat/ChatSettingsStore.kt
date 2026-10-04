package com.example.orbitai.feature.chat

import android.content.Context
import androidx.core.content.edit

/** Chat display and behaviour preferences (Settings > Chat). */
class ChatSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("orbitai_chat", Context.MODE_PRIVATE)

    /** Show time to first token, speed, backend and model under replies. */
    var showGenerationDetails: Boolean
        get() = prefs.getBoolean("show_generation_details", false)
        set(value) = prefs.edit { putBoolean("show_generation_details", value) }

    /** Ask the local model for a short title after the first reply. */
    var smartTitles: Boolean
        get() = prefs.getBoolean("smart_titles", true)
        set(value) = prefs.edit { putBoolean("smart_titles", value) }

    /** Target language for Translate in the text-selection menu. */
    var translateLanguage: String
        get() = prefs.getString("translate_language", null) ?: defaultTranslateLanguage()
        set(value) = prefs.edit { putString("translate_language", value) }

    private fun defaultTranslateLanguage(): String {
        val device = java.util.Locale.getDefault()
        return if (device.language == "en") "Hindi" else device.getDisplayLanguage(java.util.Locale.ENGLISH)
    }

    companion object {
        val TRANSLATE_LANGUAGES = listOf(
            "English", "Hindi", "Odia", "Bengali", "Tamil", "Telugu", "Kannada", "Malayalam", "Marathi", "Gujarati",
            "Punjabi", "Urdu", "Spanish", "French", "German", "Portuguese", "Japanese", "Chinese", "Korean", "Arabic",
        )
    }
}
