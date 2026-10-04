package com.example.orbitai.feature.calendar

import android.Manifest
import android.content.Context
import android.content.ContentUris
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.orbitai.core.database.AppDatabase
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Reads only the window a question asked about (f043) and turns it into a short agenda for that one
 * answer. Nothing is stored. Orbit reminders in the window are merged in.
 */
class CalendarReader(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun agenda(window: AgendaWindow): String? {
        val zone = window.start.zone
        val lines = mutableListOf<Pair<Long, String>>()
        if (hasPermission()) lines += events(window, zone)
        lines += reminders(window, zone)
        if (lines.isEmpty()) return "Calendar for ${window.label} (private, for this answer only): nothing scheduled."
        val body = lines.sortedBy { it.first }.take(MAX_LINES).joinToString("\n") { "- ${it.second}" }
        return "Calendar for ${window.label} (private, for this answer only):\n$body"
    }

    private fun events(window: AgendaWindow, zone: ZoneId): List<Pair<Long, String>> {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, window.start.toInstant().toEpochMilli())
            ContentUris.appendId(it, window.end.toInstant().toEpochMilli())
        }.build()
        val out = mutableListOf<Pair<Long, String>>()
        try {
            context.contentResolver.query(
                uri,
                arrayOf(
                    CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
                    CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.EVENT_LOCATION,
                ),
                null, null, "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext() && out.size < MAX_LINES) {
                    val title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "(no title)"
                    val begin = c.getLong(1)
                    val allDay = c.getInt(3) == 1
                    val where = c.getString(4)?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                    // All-day events are stored in UTC; show their own date, not a shifted one.
                    val label = if (allDay) {
                        Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).format(DAY) + ", all day"
                    } else {
                        Instant.ofEpochMilli(begin).atZone(zone).format(DAY_TIME) + "–" +
                            Instant.ofEpochMilli(c.getLong(2)).atZone(zone).format(TIME)
                    }
                    out += begin to "$label $title$where"
                }
            }
        } catch (_: SecurityException) {
        }
        return out
    }

    private suspend fun reminders(window: AgendaWindow, zone: ZoneId): List<Pair<Long, String>> {
        val from = window.start.toInstant().toEpochMilli()
        val to = window.end.toInstant().toEpochMilli()
        return AppDatabase.getInstance(context).reminderDao().pending()
            .filter { it.triggerAt in from until to }
            .map { it.triggerAt to Instant.ofEpochMilli(it.triggerAt).atZone(zone).format(DAY_TIME) + " Reminder: ${it.title}" }
    }

    private companion object {
        const val MAX_LINES = 25
        val DAY get() = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
        val DAY_TIME get() = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.getDefault())
        val TIME get() = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    }
}
