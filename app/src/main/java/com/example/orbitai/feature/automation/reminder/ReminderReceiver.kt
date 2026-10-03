package com.example.orbitai.feature.automation.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.orbitai.R
import com.example.orbitai.core.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID)
        if (id == null) {
            showNotification(context, intent, null)
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = ReminderScheduler(context)
                val anchor = intent.getLongExtra(EXTRA_ANCHOR, Long.MIN_VALUE)
                when (intent.action) {
                    ACTION_SNOOZE -> { scheduler.snooze(id, anchor); return@launch }
                    ACTION_DONE -> { scheduler.acknowledge(id, anchor); return@launch }
                }
                ReminderScheduler.operationMutex.withLock {
                    val dao = AppDatabase.getInstance(context).reminderDao()
                    val reminder = dao.find(id) ?: return@withLock
                    if (reminder.delivered || reminder.completed) return@withLock
                    if (intent.hasExtra(EXTRA_TRIGGER_AT) && intent.getLongExtra(EXTRA_TRIGGER_AT, 0) != reminder.triggerAt) return@withLock
                    val details = Intent().apply {
                        putExtra(EXTRA_TITLE, reminder.title)
                        putExtra(EXTRA_DESCRIPTION, reminder.description)
                        putExtra(EXTRA_ANCHOR, reminder.scheduledAt)
                    }
                    if (showNotification(context, details, id)) {
                        val next = nextReminderOccurrence(reminder.scheduledAt, reminder.repeat, System.currentTimeMillis())
                        if (next == null) dao.markDelivered(id) else {
                            val updated = reminder.copy(triggerAt = next, delivered = false)
                            dao.update(updated)
                            scheduler.enqueue(updated)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Unable to deliver reminder", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, intent: Intent, id: String?): Boolean {
        ReminderNotificationHelper.ensureChannel(context)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Reminder" }
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, title.hashCode())
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                notificationId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun actionIntent(action: String) = PendingIntent.getBroadcast(context, 0,
            Intent(context, ReminderReceiver::class.java).apply {
                this.action = action
                data = android.net.Uri.Builder().scheme("orbitai").authority("reminder-action").appendPath(id.orEmpty()).build()
                putExtra(EXTRA_REMINDER_ID, id)
                putExtra(EXTRA_ANCHOR, intent.getLongExtra(EXTRA_ANCHOR, Long.MIN_VALUE))
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, ReminderNotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(description.ifBlank { "Tap to open OrbitAI" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(description.ifBlank { title }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .apply {
                if (id != null) {
                    addAction(0, "Snooze 10 min", actionIntent(ACTION_SNOOZE))
                    addAction(0, "Done", actionIntent(ACTION_DONE))
                }
            }
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channel = context.getSystemService(android.app.NotificationManager::class.java)
            ?.getNotificationChannel(ReminderNotificationHelper.CHANNEL_ID)
        if (channel?.importance == android.app.NotificationManager.IMPORTANCE_NONE) return false
        if (id == null) manager.notify(notificationId, notification) else manager.notify(id, 0, notification)
        return true
    }

    companion object {
        const val ACTION_SNOOZE = "com.example.orbitai.reminder.SNOOZE"
        const val ACTION_DONE = "com.example.orbitai.reminder.DONE"
        const val EXTRA_ANCHOR = "scheduled_anchor"
        const val EXTRA_TRIGGER_AT = "trigger_at"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
