package com.example.orbitai.feature.automation.parser

import com.example.orbitai.feature.automation.reminder.RepeatRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** Common commands work without downloading or waking a model; every draft is reviewed. */
object LocalReminderParser {

    private const val DAY = "monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun"
    private const val ORDINAL = "(\\d{1,2})(?:st|nd|rd|th)"

    fun parse(hint: String, now: ZonedDateTime = ZonedDateTime.now()): ReminderDraft {
        var remaining = hint.trim().replace(Regex("(?i)\\b([ap])\\.?\\s*m\\.?(?=\\s|$|[,;])"), "$1m")
        var repeat: RepeatRule = RepeatRule.Once
        var date = now.toLocalDate()
        var time: LocalTime? = null
        var explicitDate = false
        fun remove(match: MatchResult) { remaining = remaining.replaceRange(match.range, " ").trim() }

        // ── Repeats ──────────────────────────────────────────────────────────
        Regex("(?i)\\b(?:every|each)\\s+(?:(\\d{1,3})\\s+)?(hours?|hrs?)\\b|\\bhourly\\b").find(remaining)?.let {
            repeat = RepeatRule.Every(it.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1, RepeatRule.Unit.HOURS)
            remove(it)
        }
        Regex("(?i)\\b(?:every|each)\\s+(\\d{1,3})\\s+(days?|weeks?)\\b").find(remaining)?.let {
            val unit = if (it.groupValues[2].lowercase().startsWith("w")) RepeatRule.Unit.WEEKS else RepeatRule.Unit.DAYS
            repeat = RepeatRule.Every(it.groupValues[1].toInt().coerceAtLeast(1), unit)
            remove(it)
        }
        Regex("(?i)\\b(?:(?:on\\s+)?(?:every|each)\\s+weekdays?|on\\s+weekdays|weekdays)\\b").find(remaining)?.let {
            repeat = RepeatRule.OnDays(RepeatRule.WEEKDAYS)
            remove(it)
        }
        Regex("(?i)\\b(?:(?:on\\s+)?(?:every|each)\\s+weekends?|on\\s+weekends)\\b").find(remaining)?.let {
            repeat = RepeatRule.OnDays(RepeatRule.WEEKEND)
            remove(it)
        }
        Regex(
            "(?i)\\b(?:on\\s+)?(?:the\\s+)?$ORDINAL\\s+(?:of\\s+)?(?:every|each)\\s+month\\b" +
                "|\\b(?:every|each)\\s+month\\s+on\\s+(?:the\\s+)?$ORDINAL\\b" +
                "|\\bmonthly\\s+on\\s+(?:the\\s+)?$ORDINAL\\b" +
                "|\\b(?:every|each)\\s+$ORDINAL\\b",
        ).find(remaining)?.let {
            val day = it.groupValues.drop(1).firstOrNull { group -> group.isNotEmpty() }?.toInt()?.coerceIn(1, 31) ?: 1
            repeat = RepeatRule.Monthly(day)
            remove(it)
        }
        Regex("(?i)\\b(?:every|each)\\s+((?:$DAY)(?:\\s*(?:,|and|&)\\s*(?:$DAY))*)\\b").find(remaining)?.let {
            val days = Regex("(?i)$DAY").findAll(it.groupValues[1]).mapNotNull { m -> dayOf(m.value) }.toSet()
            if (days.isNotEmpty()) {
                repeat = RepeatRule.OnDays(days)
                remove(it)
            }
        }
        Regex("(?i)\\b(every day|daily|every week|weekly|every month|monthly)\\b").find(remaining)?.let {
            repeat = when (it.value.lowercase()) {
                "every day", "daily" -> RepeatRule.Every(1, RepeatRule.Unit.DAYS)
                "every week", "weekly" -> RepeatRule.Every(1, RepeatRule.Unit.WEEKS)
                else -> RepeatRule.Monthly(now.dayOfMonth)
            }
            remove(it)
        }

        // ── One-off dates and times ──────────────────────────────────────────
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
        Regex("(?i)\\b(today|tomorrow|\\d{4}-\\d{2}-\\d{2})\\b").find(remaining)?.let {
            val parsed = when (it.value.lowercase()) {
                "today" -> now.toLocalDate()
                "tomorrow" -> now.toLocalDate().plusDays(1)
                else -> runCatching { LocalDate.parse(it.value) }.getOrNull()
            }
            if (parsed != null) { date = parsed; explicitDate = true; remove(it) }
        }
        Regex("(?i)\\b(?:(next)\\s+|on\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b").find(remaining)?.let {
            val day = DayOfWeek.valueOf(it.groupValues[2].uppercase())
            date = now.toLocalDate().with(if (it.groupValues[1].equals("next", true)) TemporalAdjusters.next(day) else TemporalAdjusters.nextOrSame(day))
            explicitDate = true
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
        Regex("(?i)\\b(?:in the\\s+)?(morning|afternoon|evening|tonight)\\b").find(remaining)?.takeIf { time == null }?.let {
            time = when (it.groupValues[1].lowercase()) {
                "morning" -> LocalTime.of(9, 0)
                "afternoon" -> LocalTime.of(14, 0)
                else -> LocalTime.of(19, 0)
            }
            remove(it)
        }

        // ── First occurrence ─────────────────────────────────────────────────
        if (start == null) {
            start = when (val rule = repeat) {
                is RepeatRule.Every -> if (rule.unit == RepeatRule.Unit.HOURS && time == null) {
                    now.plusHours(rule.count.toLong()).withSecond(0).withNano(0)
                } else firstAfter(rule, date, time, now)
                is RepeatRule.OnDays, is RepeatRule.Monthly -> firstAfter(rule, now.toLocalDate(), time, now)
                RepeatRule.Once -> {
                    val candidate = if (time != null || explicitDate) date.atTime(time ?: LocalTime.of(9, 0)).atZone(now.zone)
                        else now.plusHours(1).withSecond(0).withNano(0)
                    if (candidate <= now && !explicitDate) candidate.plusDays(1) else candidate
                }
            }
        }
        val title = remaining.replace(Regex("\\s+"), " ").trim(' ', ',', '.', ':')
            .removePrefix("to ").removeSuffix(" on").removeSuffix(" at").trim().ifBlank { "Reminder" }
        return ReminderDraft(title, hint, start!!.toInstant().toEpochMilli(), start!!.plusMinutes(30).toInstant().toEpochMilli(), repeat = repeat.encode())
    }

    /** The first time the rule fires on or after [date] at [time] (09:00 by default) that is still in the future. */
    private fun firstAfter(rule: RepeatRule, date: LocalDate, time: LocalTime?, now: ZonedDateTime): ZonedDateTime {
        val anchor = date.atTime(time ?: LocalTime.of(9, 0)).atZone(now.zone)
        val first = if (anchor > now && (rule !is RepeatRule.OnDays || anchor.dayOfWeek in rule.days) &&
            (rule !is RepeatRule.Monthly || anchor.dayOfMonth == minOf(rule.dayOfMonth, anchor.toLocalDate().lengthOfMonth()))) {
            anchor.toInstant().toEpochMilli()
        } else {
            rule.next(anchor.toInstant().toEpochMilli(), now.toInstant().toEpochMilli(), now.zone) ?: anchor.toInstant().toEpochMilli()
        }
        return java.time.Instant.ofEpochMilli(first).atZone(now.zone)
    }

    private fun dayOf(token: String): DayOfWeek? {
        val prefix = token.lowercase().take(3)
        return DayOfWeek.entries.firstOrNull { it.name.lowercase().startsWith(prefix) }
    }
}
