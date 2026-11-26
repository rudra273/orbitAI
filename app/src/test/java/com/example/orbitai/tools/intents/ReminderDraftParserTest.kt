package com.example.orbitai.tools.intents

import com.example.orbitai.feature.automation.parser.ReminderDraftParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderDraftParserTest {

    @Test
    fun parsesStructuredReminderOutput() {
        val draft = ReminderDraftParser.parse(
            modelOutput = """
                Title: Call mom
                Description: Discuss travel plans
                Date: 2026-03-21
                Time: 18:30
                DurationMinutes: 45
            """.trimIndent(),
            topicHint = "",
            now = LocalDateTime.of(2026, 3, 20, 10, 0),
        )

        assertEquals("Call mom", draft.title)
        assertEquals("Discuss travel plans", draft.description)

        val zoneId = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(draft.startTimeMillis).atZone(zoneId).toLocalDateTime()
        val end = Instant.ofEpochMilli(draft.endTimeMillis).atZone(zoneId).toLocalDateTime()
        assertEquals(LocalDateTime.of(2026, 3, 21, 18, 30), start)
        assertEquals(LocalDateTime.of(2026, 3, 21, 19, 15), end)
    }

    @Test
    fun missingDateAndTimeRollOverToTomorrowNearMidnight() {
        val draft = ReminderDraftParser.parse("Title: Send proposal", "", LocalDateTime.of(2026, 10, 3, 23, 30))
        val start = Instant.ofEpochMilli(draft.startTimeMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
        assertEquals(LocalDateTime.of(2026, 10, 4, 0, 30), start)
    }

    @Test
    fun unreasonableDurationIsBoundedForCalendarHandoff() {
        val draft = ReminderDraftParser.parse("Title: Test\nDurationMinutes: 2147483647", "", LocalDateTime.of(2026, 10, 3, 12, 0))
        assertEquals(86_400_000L, draft.endTimeMillis - draft.startTimeMillis)
    }
}
