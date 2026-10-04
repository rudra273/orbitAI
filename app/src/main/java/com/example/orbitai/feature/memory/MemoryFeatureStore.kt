package com.example.orbitai.feature.memory

import android.content.Context
import androidx.core.content.edit

class MemoryFeatureStore(context: Context) {
    private val prefs = context.getSharedPreferences("orbitai_prefs", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("memory_feature_enabled", true)
        set(value) {
            prefs.edit().putBoolean("memory_feature_enabled", value).apply()
        }

    /** Offer to remember personal facts found in chat messages. */
    var suggestionsEnabled: Boolean
        get() = prefs.getBoolean("memory_suggestions_enabled", true)
        set(value) {
            prefs.edit { putBoolean("memory_suggestions_enabled", value) }
        }
}
