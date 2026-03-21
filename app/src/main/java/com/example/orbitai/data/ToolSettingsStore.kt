package com.example.orbitai.data

import android.content.Context

class ToolSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("orbitai_tool_prefs", Context.MODE_PRIVATE)

    var isAutomationExecutionEnabled: Boolean
        get() = prefs.getBoolean("automation_execution_enabled", false)
        set(value) {
            prefs.edit().putBoolean("automation_execution_enabled", value).apply()
        }

    var isFloatingBubbleEnabled: Boolean
        get() = prefs.getBoolean("floating_bubble_enabled", false)
        set(value) {
            prefs.edit().putBoolean("floating_bubble_enabled", value).apply()
        }

    var bubbleSizeDp: Int
        get() = prefs.getInt("bubble_size_dp", 64)
        set(value) {
            prefs.edit().putInt("bubble_size_dp", value).apply()
        }

    var bubbleResultsInOverlay: Boolean
        get() = prefs.getBoolean("bubble_results_in_overlay", false)
        set(value) {
            prefs.edit().putBoolean("bubble_results_in_overlay", value).apply()
        }

    var bubbleResponseHeightDp: Int
        get() = prefs.getInt("bubble_response_height_dp", 220)
        set(value) {
            prefs.edit().putInt("bubble_response_height_dp", value).apply()
        }

    var bubbleIdleAlphaPercent: Int
        get() = prefs.getInt("bubble_idle_alpha_percent", 42)
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
}
