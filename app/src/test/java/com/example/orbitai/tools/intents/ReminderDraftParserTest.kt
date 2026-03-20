package com.example.orbitai.tools.intents

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
}
