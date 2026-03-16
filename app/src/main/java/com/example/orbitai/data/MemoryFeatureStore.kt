package com.example.orbitai.data

import android.content.Context

class MemoryFeatureStore(context: Context) {
    private val prefs = context.getSharedPreferences("orbitai_prefs", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("memory_feature_enabled", true)
        set(value) {
            prefs.edit().putBoolean("memory_feature_enabled", value).apply()
        }
}