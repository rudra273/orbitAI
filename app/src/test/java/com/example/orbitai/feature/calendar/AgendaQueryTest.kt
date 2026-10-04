package com.example.orbitai.feature.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AgendaQueryTest {

    // Wednesday 2026-10-07, 10:00
    private val now = ZonedDateTime.of(2026, 10, 7, 10, 0, 0, 0, ZoneId.of("Asia/Kolkata"))

    private fun dates(text: String): Pair<LocalDate, LocalDate>? =
        AgendaQuery.parse(text, now)?.let { it.start.toLocalDate() to it.end.minusNanos(1).toLocalDate() }

    @Test
    fun ordinaryChatIsNotAScheduleQuestion() {
        listOf("explain kotlin", "write an email about the meeting notes", "what is the capital of France", "tell me a joke about calendars")
            .forEach { assertNull(it, AgendaQuery.parse(it, now)) }
    }

    @Test
    fun todayIsTheDefault() {
        assertEquals(LocalDate.of(2026, 10, 7) to LocalDate.of(2026, 10, 7), dates("what's on my day?"))
        assertEquals(LocalDate.of(2026, 10, 7) to LocalDate.of(2026, 10, 7), dates("Do I have any meetings"))
    }

    @Test
    fun tomorrowAndWeekdays() {
        assertEquals(LocalDate.of(2026, 10, 8) to LocalDate.of(2026, 10, 8), dates("What's my schedule tomorrow"))
        assertEquals(LocalDate.of(2026, 10, 9) to LocalDate.of(2026, 10, 9), dates("am I free on Friday?"))
        assertEquals(LocalDate.of(2026, 10, 12) to LocalDate.of(2026, 10, 12), dates("am I free next Monday?"))
    }

    @Test
    fun weeksAndWeekend() {
        assertEquals(LocalDate.of(2026, 10, 7) to LocalDate.of(2026, 10, 11), dates("what's on my calendar this week"))
        assertEquals(LocalDate.of(2026, 10, 12) to LocalDate.of(2026, 10, 18), dates("show my agenda next week"))
        assertEquals(LocalDate.of(2026, 10, 10) to LocalDate.of(2026, 10, 11), dates("any events this weekend?"))
    }
}
