package com.example.orbitai.tools.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object ReminderNotificationHelper {

    const val CHANNEL_ID = "orbitai_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "OrbitAI Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Background reminders created by OrbitAI tools"
        }
        manager.createNotificationChannel(channel)
    }
}
