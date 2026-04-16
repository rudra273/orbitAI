package com.example.orbitai.feature.automation.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.orbitai.feature.automation.parser.AutomationExecutionResult
import com.example.orbitai.feature.automation.parser.ReminderDraft
import com.example.orbitai.feature.automation.parser.RuntimeToolPermission

class ReminderScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(draft: ReminderDraft): AutomationExecutionResult {
        if (draft.startTimeMillis <= System.currentTimeMillis()) {
            return AutomationExecutionResult.Failed("Reminder time must be in the future.")
        }

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return AutomationExecutionResult.PermissionRequired(
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
        ) ?: return AutomationExecutionResult.Failed("Alarm manager is unavailable on this device.")

        return AutomationExecutionResult.Launched
    }
}
