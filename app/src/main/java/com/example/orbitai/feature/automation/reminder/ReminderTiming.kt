package com.example.orbitai.feature.automation.reminder

import java.time.Instant
import java.time.ZoneId

enum class ReminderRepeat(val label: String) { NONE("Once"), DAILY("Daily"), WEEKLY("Weekly") }

fun nextReminderOccurrence(anchor: Long, repeat: String, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
    if (repeat == ReminderRepeat.NONE.name) return null
    var next = Instant.ofEpochMilli(anchor).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone)
    val days = java.time.temporal.ChronoUnit.DAYS.between(next.toLocalDate(), today.toLocalDate()).coerceAtLeast(0)
    val interval = if (repeat == ReminderRepeat.WEEKLY.name) 7 else 1
    next = next.plusDays(days / interval * interval)
    while (next.toInstant().toEpochMilli() <= now) next = next.plusDays(interval.toLong())
    return next.toInstant().toEpochMilli()
}
