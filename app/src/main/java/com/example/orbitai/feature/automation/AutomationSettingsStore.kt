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

    /** Calendar awareness (f043) reads calendar events, so it is off until the user turns it on. */
    var isCalendarAwarenessEnabled: Boolean
        get() = prefs.getBoolean("calendar_awareness", false)
        set(value) = prefs.edit { putBoolean("calendar_awareness", value) }

    fun enabledTools(): Set<OrbitTool> = OrbitTool.entries.filterTo(mutableSetOf(), ::isEnabled)
}
