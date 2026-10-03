package com.example.orbitai.reminders

import com.example.orbitai.feature.automation.parser.LocalReminderParser
import com.example.orbitai.feature.automation.reminder.nextReminderOccurrence
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime
import java.time.Instant
import java.time.ZoneId

class LocalReminderParserTest {
    private val now = ZonedDateTime.parse("2026-10-03T18:15:00+05:30[Asia/Kolkata]")
    private fun time(hint: String, clock: ZonedDateTime = now) =
        Instant.ofEpochMilli(LocalReminderParser.parse(hint, clock).startTimeMillis).atZone(clock.zone)

    @Test fun tenPmIsTwentyTwoInTheDeviceZone() {
        assertEquals(now.withHour(22).withMinute(0), time("at 10 pm to call Mom"))
        assertEquals(now.withHour(22).withMinute(0), time("call Mom at 10 p.m."))
        assertEquals(now.withHour(22).withMinute(30), time("call Mom at 10:30 PM"))
    }

    @Test fun tomorrowKeepsDateAndPmTime() {
        assertEquals(now.plusDays(1).withHour(22).withMinute(0), time("tomorrow at 10 pm to submit report"))
    }

    @Test fun missingDateRollsForwardButExplicitDateDoesNotChange() {
        val late = now.withHour(23)
        assertEquals(now.plusDays(1).withHour(22).withMinute(0), time("at 10 pm", late))
        assertEquals(now.withHour(22).withMinute(0), time("today at 10 pm", late))
    }

    @Test fun noonAndMidnightHaveCorrectTwelveHourConversion() {
        assertEquals(0, time("tomorrow at 12 am").hour)
        assertEquals(12, time("tomorrow at 12 pm").hour)
        assertEquals(0, time("tomorrow at midnight").hour)
        assertEquals(12, time("tomorrow at noon").hour)
    }

    @Test fun relativeTimeAndRepeatingWeekday() {
        assertEquals(now.plusMinutes(10), time("in 10 minutes to drink water"))
        val weekly = LocalReminderParser.parse("every Friday at 10 pm submit report", now)
        assertEquals("WEEKLY", weekly.repeat)
        assertEquals("2026-10-09T22:00+05:30[Asia/Kolkata]", Instant.ofEpochMilli(weekly.startTimeMillis).atZone(now.zone).toString())
    }

    @Test fun dailyRecurrenceKeepsWallClockAcrossDaylightSavingsAndSkipsMissedDays() {
        val anchor = ZonedDateTime.parse("2026-03-07T10:00:00-05:00[America/New_York]")
        val after = ZonedDateTime.parse("2026-03-08T11:00:00-04:00[America/New_York]")
        val next = nextReminderOccurrence(anchor.toInstant().toEpochMilli(), "DAILY", after.toInstant().toEpochMilli(), anchor.zone)!!
        assertEquals(after.plusDays(1).withHour(10), Instant.ofEpochMilli(next).atZone(anchor.zone))
        assertNull(nextReminderOccurrence(0, "NONE", 10, ZoneId.of("UTC")))
    }
}
