package com.example.orbitai.feature.automation.digest

import android.content.Context
import androidx.core.content.edit

/** Separate from Message replies: its own switch and its own list of apps. Off until the user turns it on. */
class DigestSettings(context: Context) {
    private val prefs = context.getSharedPreferences("notification_digest", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) = prefs.edit { putBoolean("enabled", value) }

    var packages: Set<String>
        get() = prefs.getStringSet("packages", emptySet()).orEmpty().toSet()
        set(value) = prefs.edit { putStringSet("packages", value.toSet()) }

    fun allows(packageName: String) = enabled && packageName in packages

    companion object {
        /** Messaging and mail only. Banking and payment apps are deliberately not offered. */
        val apps = linkedMapOf(
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp Business",
            "org.thoughtcrime.securesms" to "Signal",
            "org.telegram.messenger" to "Telegram",
            "com.google.android.apps.messaging" to "Google Messages",
            "com.google.android.gm" to "Gmail",
        )
    }
}
