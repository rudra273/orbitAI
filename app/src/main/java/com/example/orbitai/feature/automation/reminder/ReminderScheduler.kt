package com.example.orbitai.feature.automation.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import com.example.orbitai.core.database.AppDatabase
import com.example.orbitai.core.database.ReminderEntity
import com.example.orbitai.feature.automation.parser.AutomationExecutionResult
import com.example.orbitai.feature.automation.parser.ReminderDraft
import com.example.orbitai.feature.automation.parser.RuntimeToolPermission
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val dao = AppDatabase.getInstance(appContext).reminderDao()
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    val reminders = dao.observeAll()

    suspend fun schedule(draft: ReminderDraft): AutomationExecutionResult = operationMutex.withLock {
        if (draft.title.isBlank() || draft.startTimeMillis <= System.currentTimeMillis()) {
            return@withLock AutomationExecutionResult.Failed("Enter a title and a reminder time in the future.")
        }
        ReminderNotificationHelper.ensureChannel(appContext)
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
            return@withLock AutomationExecutionResult.PermissionRequired(
                RuntimeToolPermission.NOTIFICATIONS,
                "Enable Orbit notifications to receive reminders.",
            )
        }
        val channel = appContext.getSystemService(NotificationManager::class.java)
            ?.getNotificationChannel(ReminderNotificationHelper.CHANNEL_ID)
        if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
            return@withLock AutomationExecutionResult.Failed("Enable the OrbitAI Reminders notification channel in Android Settings.")
        }
        val previous = draft.id?.let { dao.find(it) }
        if (draft.id != null && previous == null) return@withLock AutomationExecutionResult.Failed("This reminder was removed. Create a new one.")
        val reminder = ReminderEntity(draft.id ?: UUID.randomUUID().toString(), draft.title.trim(), draft.description,
            draft.startTimeMillis, repeat = draft.repeat, scheduledAt = draft.startTimeMillis)
        if (previous == null) dao.insert(reminder) else dao.update(reminder)
        try {
            enqueue(reminder)
        } catch (e: Exception) {
            if (previous == null) dao.delete(reminder.id) else dao.update(previous)
            if (e is CancellationException) throw e
            return@withLock AutomationExecutionResult.Failed("Couldn't schedule the reminder. Please try again.")
        }
        NotificationManagerCompat.from(appContext).cancel(reminder.id, 0)
        return@withLock AutomationExecutionResult.Launched
    }

    fun exactAlarmsEnabled(): Boolean = alarmManager?.canScheduleExactAlarms() == true

    fun enqueue(reminder: ReminderEntity) {
        val manager = checkNotNull(alarmManager)
        val time = reminder.triggerAt.coerceAtLeast(System.currentTimeMillis())
        val pending = pendingIntent(reminder.id, reminder.triggerAt)
        if (manager.canScheduleExactAlarms()) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
                return
            } catch (_: SecurityException) {
                // Access can be revoked between the check and scheduling.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending)
    }

    suspend fun snooze(id: String, expectedAnchor: Long? = null) = operationMutex.withLock {
        val current = dao.find(id) ?: return@withLock
        if (current.completed || (expectedAnchor != null && expectedAnchor != current.scheduledAt)) return@withLock
        val updated = current.copy(triggerAt = System.currentTimeMillis() + 10 * 60_000L, delivered = false)
        dao.update(updated)
        enqueue(updated)
        NotificationManagerCompat.from(appContext).cancel(id, 0)
    }

    suspend fun complete(id: String) = operationMutex.withLock {
        val current = dao.find(id) ?: return@withLock
        dao.update(current.copy(completed = true))
        alarmManager?.cancel(pendingIntent(id, current.triggerAt))
        NotificationManagerCompat.from(appContext).cancel(id, 0)
    }

    suspend fun acknowledge(id: String, expectedAnchor: Long? = null) = operationMutex.withLock {
        val current = dao.find(id) ?: return@withLock
        if (current.completed || (expectedAnchor != null && expectedAnchor != current.scheduledAt)) return@withLock
        if (current.repeat == ReminderRepeat.NONE.name) {
            dao.update(current.copy(completed = true))
            alarmManager?.cancel(pendingIntent(id, current.triggerAt))
        }
        NotificationManagerCompat.from(appContext).cancel(id, 0)
    }

    suspend fun restore() = operationMutex.withLock {
        dao.pending().forEach(::enqueue)
    }

    suspend fun cancel(id: String) = operationMutex.withLock {
        dao.delete(id)
        alarmManager?.cancel(pendingIntent(id, 0))
        NotificationManagerCompat.from(appContext).cancel(id, 0)
    }

    companion object {
        internal val operationMutex = Mutex()
    }

    private fun pendingIntent(id: String, triggerAt: Long): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        0,
        Intent(appContext, ReminderReceiver::class.java).apply {
            data = Uri.Builder().scheme("orbitai").authority("reminder").appendPath(id).build()
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
            putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, triggerAt)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
