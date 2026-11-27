package com.example.orbitai.core.common

import android.content.Context
import androidx.core.content.edit

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark"), BLACK("Pure black") }

/** How the app looks. [chatTextScale] applies to chat messages only (0.9–1.3). */
data class Appearance(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val chatTextScale: Float = 1f,
    val compactMessages: Boolean = false,
) {
    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.BLACK -> true
    }
}

class ThemeSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("orbitai_theme_prefs", Context.MODE_PRIVATE)

    var appearance: Appearance
        get() = Appearance(
            mode = themeMode(),
            dynamicColor = prefs.getBoolean("dynamic_color", false),
            chatTextScale = prefs.getFloat("chat_text_scale", 1f),
            compactMessages = prefs.getBoolean("compact_messages", false),
        )
        set(value) = prefs.edit {
            putString("theme_mode", value.mode.name)
            putBoolean("dynamic_color", value.dynamicColor)
            putFloat("chat_text_scale", value.chatTextScale)
            putBoolean("compact_messages", value.compactMessages)
        }

    /**
     * Fresh installs follow the system. Users who picked a theme in an earlier build keep it: the old
     * `is_dark_theme` flag becomes an explicit Light or Dark choice.
     */
    private fun themeMode(): ThemeMode = resolveMode(
        stored = prefs.getString("theme_mode", null),
        legacyDark = if (prefs.contains("is_dark_theme")) prefs.getBoolean("is_dark_theme", true) else null,
    )

    companion object {
        /** [legacyDark] is the previous `is_dark_theme` value, or null if it was never set. */
        fun resolveMode(stored: String?, legacyDark: Boolean?): ThemeMode {
            stored?.let { name -> ThemeMode.entries.firstOrNull { it.name == name }?.let { return it } }
            return when (legacyDark) {
                null -> ThemeMode.SYSTEM
                true -> ThemeMode.DARK
                false -> ThemeMode.LIGHT
            }
        }
    }
}
