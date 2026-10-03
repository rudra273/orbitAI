package com.example.orbitai.feature.automation.replies

import android.content.Context

class NotificationReplySettings(context: Context) {
    private val prefs = context.getSharedPreferences("notification_replies", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }

    var packages: Set<String>
        get() = prefs.getStringSet("packages", emptySet()).orEmpty().toSet()
        set(value) { prefs.edit().putStringSet("packages", value.toSet()).apply() }

    fun allows(packageName: String) = enabled && packageName in packages

    companion object {
        val apps = linkedMapOf(
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp Business",
            "org.thoughtcrime.securesms" to "Signal",
            "org.telegram.messenger" to "Telegram",
            "com.google.android.apps.messaging" to "Google Messages",
        )
    }
}
