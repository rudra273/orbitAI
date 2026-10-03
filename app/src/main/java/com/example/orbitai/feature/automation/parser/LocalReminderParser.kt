package com.example.orbitai.feature.automation.parser

import com.example.orbitai.feature.automation.reminder.ReminderRepeat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

/** Common commands work without downloading or waking a model; every draft is reviewed. */
object LocalReminderParser {
    fun parse(hint: String, now: ZonedDateTime = ZonedDateTime.now()): ReminderDraft {
        var remaining = hint.trim().replace(Regex("(?i)\\b([ap])\\.?\\s*m\\.?(?=\\s|$|[,;])"), "$1m")
        var repeat = ReminderRepeat.NONE
        var date = now.toLocalDate()
        var time: LocalTime? = null
        var explicitDate = false
        fun remove(match: MatchResult) { remaining = remaining.replaceRange(match.range, " ").trim() }
        val relative = Regex("(?i)\\bin\\s+(\\d{1,5})\\s+(minutes?|mins?|hours?|hrs?|days?)\\b").find(remaining)
        var start: ZonedDateTime? = relative?.let {
            val amount = it.groupValues[1].toLong()
            remove(it)
            when (it.groupValues[2].lowercase().first()) {
                'm' -> now.plusMinutes(amount)
                'h' -> now.plusHours(amount)
                else -> now.plusDays(amount)
            }
        }
        Regex("(?i)\\b(every day|daily|every week|weekly)\\b").find(remaining)?.let {
            repeat = if (it.value.lowercase() in listOf("every day", "daily")) ReminderRepeat.DAILY else ReminderRepeat.WEEKLY
            remove(it)
        }
        Regex("(?i)\\b(today|tomorrow|\\d{4}-\\d{2}-\\d{2})\\b").find(remaining)?.let {
            val parsed = when (it.value.lowercase()) {
                "today" -> now.toLocalDate()
                "tomorrow" -> now.toLocalDate().plusDays(1)
                else -> runCatching { LocalDate.parse(it.value) }.getOrNull()
            }
            if (parsed != null) { date = parsed; explicitDate = true; remove(it) }
        }
        Regex("(?i)\\b(?:(every|next)\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b").find(remaining)?.let {
            val day = DayOfWeek.valueOf(it.groupValues[2].uppercase())
            date = now.toLocalDate().with(if (it.groupValues[1].equals("next", true)) TemporalAdjusters.next(day) else TemporalAdjusters.nextOrSame(day))
            explicitDate = true
            if (it.groupValues[1].equals("every", true)) repeat = ReminderRepeat.WEEKLY
            remove(it)
        }
        Regex("(?i)\\b(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))\\s*(am|pm)?\\b|\\b(?:at\\s+)?(\\d{1,2})\\s*(am|pm)\\b").find(remaining)?.let {
            val hour = (it.groupValues[1].ifBlank { it.groupValues[4] }).toInt()
            val minute = it.groupValues[2].ifBlank { "0" }.toInt()
            val period = it.groupValues[3].ifBlank { it.groupValues[5] }.lowercase()
            if (minute in 0..59 && (if (period.isBlank()) hour in 0..23 else hour in 1..12)) {
                time = LocalTime.of(if (period.isBlank()) hour else hour % 12 + if (period == "pm") 12 else 0, minute)
                remove(it)
            }
        }
        Regex("(?i)\\b(?:at\\s+)?(noon|midnight)\\b").find(remaining)?.let {
            time = if (it.groupValues[1].equals("noon", true)) LocalTime.NOON else LocalTime.MIDNIGHT
            remove(it)
        }
        if (start == null) {
            start = if (time != null || explicitDate) date.atTime(time ?: LocalTime.of(9, 0)).atZone(now.zone)
                    else now.plusHours(1).withSecond(0).withNano(0)
            if (start <= now && (!explicitDate || repeat != ReminderRepeat.NONE)) {
                start = if (repeat == ReminderRepeat.WEEKLY) start.plusWeeks(1) else start.plusDays(1)
            }
        }
        val title = remaining.replace(Regex("\\s+"), " ").trim(' ', ',', '.', ':')
            .removePrefix("to ").ifBlank { "Reminder" }
        return ReminderDraft(title, hint, start.toInstant().toEpochMilli(), start.plusMinutes(30).toInstant().toEpochMilli(), repeat = repeat.name)
    }
}
