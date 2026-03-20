package com.example.orbitai.tools.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.orbitai.tools.intents.IntentToolExecutionResult
import com.example.orbitai.tools.intents.ReminderDraft
import com.example.orbitai.tools.intents.RuntimeToolPermission

class ReminderScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(draft: ReminderDraft): IntentToolExecutionResult {
        if (draft.startTimeMillis <= System.currentTimeMillis()) {
            return IntentToolExecutionResult.Failed("Reminder time must be in the future.")
        }

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return IntentToolExecutionResult.PermissionRequired(
                permission = RuntimeToolPermission.NOTIFICATIONS,
                message = "Grant notifications permission to run reminders automatically in the background.",
            )
        }

        ReminderNotificationHelper.ensureChannel(appContext)

        val notificationId = (draft.startTimeMillis xor draft.title.hashCode().toLong()).toInt()
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, draft.title)
            putExtra(ReminderReceiver.EXTRA_DESCRIPTION, draft.description)
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager?.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            draft.startTimeMillis,
            pendingIntent,
        ) ?: return IntentToolExecutionResult.Failed("Alarm manager is unavailable on this device.")

        return IntentToolExecutionResult.Launched
    }
}
