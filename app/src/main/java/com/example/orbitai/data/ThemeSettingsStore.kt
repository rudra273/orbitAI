package com.example.orbitai.data

import android.content.Context

class ThemeSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("orbitai_theme_prefs", Context.MODE_PRIVATE)

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("is_dark_theme", true)
        set(value) {
            prefs.edit().putBoolean("is_dark_theme", value).apply()
        }
}
