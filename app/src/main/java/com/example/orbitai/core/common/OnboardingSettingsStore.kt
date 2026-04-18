package com.example.orbitai.core.common

import android.content.Context

class OnboardingSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("orbitai_onboarding_prefs", Context.MODE_PRIVATE)

    var hasCompletedWelcome: Boolean
        get() = prefs.getBoolean("has_completed_welcome", false)
        set(value) {
            prefs.edit().putBoolean("has_completed_welcome", value).apply()
        }

    var userName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(value) {
            prefs.edit().putString("user_name", value.trim()).apply()
        }
}
