package com.example.orbitai.feature.automation

import android.content.Context
import androidx.core.content.edit

class AutomationSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("orbitai_tool_prefs", Context.MODE_PRIVATE)

    var isAutomationExecutionEnabled: Boolean
        get() = prefs.getBoolean("automation_execution_enabled", false)
        set(value) = prefs.edit { putBoolean("automation_execution_enabled", value) }

    /** Tools are on unless the user turned them off. */
    fun isEnabled(tool: OrbitTool): Boolean = prefs.getBoolean("tool_enabled_${tool.name}", true)

    fun setEnabled(tool: OrbitTool, enabled: Boolean) = prefs.edit { putBoolean("tool_enabled_${tool.name}", enabled) }

    fun enabledTools(): Set<OrbitTool> = OrbitTool.entries.filterTo(mutableSetOf(), ::isEnabled)
}
