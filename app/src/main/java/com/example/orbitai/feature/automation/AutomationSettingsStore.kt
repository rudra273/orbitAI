package com.example.orbitai.feature.automation

import android.content.Context

class AutomationSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("orbitai_tool_prefs", Context.MODE_PRIVATE)

    var isAutomationExecutionEnabled: Boolean
        get() = prefs.getBoolean("automation_execution_enabled", false)
        set(value) {
            prefs.edit().putBoolean("automation_execution_enabled", value).apply()
        }
}
