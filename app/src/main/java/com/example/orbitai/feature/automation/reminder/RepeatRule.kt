package com.example.orbitai.feature.automation.reminder

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * How a reminder repeats, stored as an RRULE-style string (the format calendars use) so new
 * kinds can be added later without another migration. Old "DAILY"/"WEEKLY" values still parse.
 */
sealed interface RepeatRule {

    data object Once : RepeatRule
    /** Every [count] hours, days or weeks. */
    data class Every(val count: Int, val unit: Unit) : RepeatRule
    /** On these weekdays, at the reminder's time. */
    data class OnDays(val days: Set<DayOfWeek>) : RepeatRule
    /** On this day of every month; short months use their last day. */
    data class Monthly(val dayOfMonth: Int) : RepeatRule

    enum class Unit(val freq: String) { HOURS("HOURLY"), DAYS("DAILY"), WEEKS("WEEKLY") }

    fun encode(): String = when (this) {
        Once -> "NONE"
        is Every -> "FREQ=${unit.freq}" + if (count > 1) ";INTERVAL=$count" else ""
        is OnDays -> "FREQ=WEEKLY;BYDAY=" + days.sorted().joinToString(",") { it.name.take(2) }
        is Monthly -> "FREQ=MONTHLY;BYMONTHDAY=$dayOfMonth"
    }

    val label: String
        get() = when (this) {
            Once -> "Once"
            is Every -> when {
                count == 1 && unit == Unit.HOURS -> "Every hour"
                count == 1 && unit == Unit.DAYS -> "Daily"
                count == 1 && unit == Unit.WEEKS -> "Weekly"
                else -> "Every $count ${unit.name.lowercase()}"
            }
            is OnDays -> when (days) {
                WEEKDAYS -> "Weekdays"
                WEEKEND -> "Weekends"
                else -> "Every " + days.sorted().joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
            }
            is Monthly -> "Monthly on the ${ordinal(dayOfMonth)}"
        }

    /**
     * The first occurrence after [now], keeping the wall-clock time of [anchor] (so 9:00 stays
     * 9:00 across daylight-saving changes). Hourly repeats use real elapsed time. Null for [Once].
     */
    fun next(anchor: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val start = Instant.ofEpochMilli(anchor).atZone(zone)
        val after = maxOf(now, anchor - 1)
        return when (this) {
            Once -> null
            is Every -> when (unit) {
                Unit.HOURS -> {
                    val step = count * 3_600_000L
                    val passed = ((after - anchor) / step).coerceAtLeast(0) + 1
                    anchor + passed * step
                }
                Unit.DAYS, Unit.WEEKS -> {
                    val interval = count.toLong() * if (unit == Unit.WEEKS) 7 else 1
                    val today = Instant.ofEpochMilli(after).atZone(zone).toLocalDate()
                    val days = ChronoUnit.DAYS.between(start.toLocalDate(), today).coerceAtLeast(0)
                    var next = start.plusDays(days / interval * interval)
                    while (next.toInstant().toEpochMilli() <= after) next = next.plusDays(interval)
                    next.toInstant().toEpochMilli()
                }
            }
            is OnDays -> {
                if (days.isEmpty()) return null
                var day = maxOf(start.toLocalDate(), Instant.ofEpochMilli(after).atZone(zone).toLocalDate())
                repeat(8) {
                    val candidate = ZonedDateTime.of(day, start.toLocalTime(), zone)
                    if (day.dayOfWeek in days && candidate.toInstant().toEpochMilli() > after) return candidate.toInstant().toEpochMilli()
                    day = day.plusDays(1)
                }
                null
            }
            is Monthly -> {
                var month = maxOf(
                    java.time.YearMonth.from(start),
                    java.time.YearMonth.from(Instant.ofEpochMilli(after).atZone(zone)),
                )
                repeat(14) {
                    val date = month.atDay(minOf(dayOfMonth, month.lengthOfMonth()))
                    val candidate = ZonedDateTime.of(date, start.toLocalTime(), zone)
                    if (candidate.toInstant().toEpochMilli() > after) return candidate.toInstant().toEpochMilli()
                    month = month.plusMonths(1)
                }
                null
            }
        }
    }

    companion object {
        val WEEKDAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        fun parse(value: String?): RepeatRule {
            val text = value?.trim().orEmpty()
            when (text) {
                "", "NONE" -> return Once
                "DAILY" -> return Every(1, Unit.DAYS)
                "WEEKLY" -> return Every(1, Unit.WEEKS)
            }
            val parts = text.split(';').mapNotNull { part ->
                part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].uppercase() to it[1].uppercase() }
            }.toMap()
            val interval = parts["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            return when (parts["FREQ"]) {
                "HOURLY" -> Every(interval, Unit.HOURS)
                "DAILY" -> Every(interval, Unit.DAYS)
                "WEEKLY" -> parts["BYDAY"]?.let { byDay ->
                    OnDays(byDay.split(',').mapNotNull { code -> DayOfWeek.entries.firstOrNull { it.name.startsWith(code) } }.toSet())
                } ?: Every(interval, Unit.WEEKS)
                "MONTHLY" -> parts["BYMONTHDAY"]?.toIntOrNull()?.coerceIn(1, 31)?.let { Monthly(it) } ?: Once
                else -> Once
            }
        }

        fun ordinal(n: Int): String = n.toString() + when {
            n % 100 in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else -> "th"
        }
    }
}
