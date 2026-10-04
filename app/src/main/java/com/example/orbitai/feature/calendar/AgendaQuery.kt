package com.example.orbitai.feature.calendar

import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** The part of the calendar a question is about. */
data class AgendaWindow(val start: ZonedDateTime, val end: ZonedDateTime, val label: String)

/** Recognises schedule questions ("what's on my day tomorrow?", "am I free Friday?") and the dates they ask about. */
object AgendaQuery {

    private val triggers = listOf(
        Regex("""\b(?:what'?s|whats|what is|what do i have)\b.*\b(?:on|up|happening|planned|today|tomorrow|this week|next week)\b.*\b(?:day|today|tomorrow|week|weekend|schedule|calendar|agenda|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b"""),
        Regex("""\b(?:my|the)\s+(?:schedule|calendar|agenda)\b"""),
        Regex("""\b(?:am i|are we)\s+(?:free|busy|available)\b"""),
        Regex("""\bany\s+(?:meetings?|events?|appointments?)\b"""),
        Regex("""\bdo i have\s+(?:any\s+|a\s+|an\s+)?(?:meetings?|events?|appointments?|plans?)\b"""),
        Regex("""\bplan my\s+(?:day|morning|afternoon|evening|week)\b"""),
        Regex("""\bmy day\b"""),
    )

    private val dayNames = DayOfWeek.entries.associateBy { it.name.lowercase() }

    /** Null when the text isn't a schedule question. */
    fun parse(text: String, now: ZonedDateTime): AgendaWindow? {
        val t = text.lowercase().replace("’", "'")
        if (triggers.none { it.containsMatchIn(t) }) return null
        val today = now.toLocalDate()
        fun day(date: java.time.LocalDate, label: String) =
            AgendaWindow(date.atStartOfDay(now.zone), date.plusDays(1).atStartOfDay(now.zone), label)
        fun span(from: java.time.LocalDate, to: java.time.LocalDate, label: String) =
            AgendaWindow(from.atStartOfDay(now.zone), to.plusDays(1).atStartOfDay(now.zone), label)

        Regex("""\bnext week\b""").find(t)?.let {
            val monday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            return span(monday, monday.plusDays(6), "next week")
        }
        Regex("""\bthis week\b|\bmy week\b|\bthe week\b|\bplan my week\b""").find(t)?.let {
            return span(today, today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)), "this week")
        }
        Regex("""\b(?:this\s+)?weekend\b""").find(t)?.let {
            val saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            return span(saturday, saturday.plusDays(1), "this weekend")
        }
        Regex("""\bday after tomorrow\b""").find(t)?.let { return day(today.plusDays(2), "the day after tomorrow") }
        Regex("""\btomorrow\b""").find(t)?.let { return day(today.plusDays(1), "tomorrow") }
        Regex("""\b(?:(next)\s+|on\s+|this\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b""").find(t)?.let {
            val target = dayNames.getValue(it.groupValues[2])
            val date = if (it.groupValues[1] == "next") today.with(TemporalAdjusters.next(target)) else today.with(TemporalAdjusters.nextOrSame(target))
            return day(date, it.groupValues[2].replaceFirstChar { c -> c.uppercase() })
        }
        return day(today, "today")
    }
}
