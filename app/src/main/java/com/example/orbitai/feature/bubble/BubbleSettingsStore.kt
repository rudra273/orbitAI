package com.example.orbitai.feature.bubble

import android.content.Context

class BubbleSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("orbitai_tool_prefs", Context.MODE_PRIVATE)

    var isFloatingBubbleEnabled: Boolean
        get() = prefs.getBoolean("floating_bubble_enabled", false)
        set(value) {
            prefs.edit().putBoolean("floating_bubble_enabled", value).apply()
        }

    var bubbleSizeDp: Int
        get() = prefs.getInt("bubble_size_dp", 48)
        set(value) {
            prefs.edit().putInt("bubble_size_dp", value).apply()
        }

    var bubbleResultsInOverlay: Boolean
        get() = prefs.getBoolean("bubble_results_in_overlay", true)
        set(value) {
            prefs.edit().putBoolean("bubble_results_in_overlay", value).apply()
        }

    var bubbleResponseHeightDp: Int
        get() = prefs.getInt("bubble_response_height_dp", 220)
        set(value) {
            prefs.edit().putInt("bubble_response_height_dp", value).apply()
        }

    var bubbleIdleAlphaPercent: Int
        get() = prefs.getInt("bubble_idle_alpha_percent", 20)
        set(value) {
            prefs.edit().putInt("bubble_idle_alpha_percent", value.coerceIn(20, 100)).apply()
        }

    var bubbleStyle: String
        get() = prefs.getString("bubble_style", "round") ?: "round"
        set(value) {
            prefs.edit().putString("bubble_style", value).apply()
        }

    var bubbleModelId: String
        get() = prefs.getString("bubble_model_id", "") ?: ""
        set(value) {
            prefs.edit().putString("bubble_model_id", value).apply()
        }

    var bubbleModeId: String
        get() = prefs.getString("bubble_mode_id", "bubble_default") ?: "bubble_default"
        set(value) {
            prefs.edit().putString("bubble_mode_id", value).apply()
        }

    var bubbleResponseAlphaPercent: Int
        get() = prefs.getInt("bubble_response_alpha_percent", 50)
        set(value) {
            prefs.edit().putInt("bubble_response_alpha_percent", value.coerceIn(10, 100)).apply()
        }

    var bubbleResponseTheme: String
        get() = prefs.getString("bubble_response_theme", "violet") ?: "violet"
        set(value) {
            prefs.edit().putString("bubble_response_theme", value).apply()
        }
}
