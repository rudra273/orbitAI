package com.example.orbitai.core.common

import android.content.Context
import androidx.core.content.edit

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

    /** The "What's new" version the user has seen; new installs start at the current one. */
    var lastSeenWhatsNew: Int
        get() = prefs.getInt("last_seen_whats_new", 0)
        set(value) {
            prefs.edit { putInt("last_seen_whats_new", value) }
        }
}
