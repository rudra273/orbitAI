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
        assertEquals("FREQ=WEEKLY;BYDAY=FR", weekly.repeat)
        assertEquals("2026-10-09T22:00+05:30[Asia/Kolkata]", Instant.ofEpochMilli(weekly.startTimeMillis).atZone(now.zone).toString())
    }

    @Test fun dailyRecurrenceKeepsWallClockAcrossDaylightSavingsAndSkipsMissedDays() {
        val anchor = ZonedDateTime.parse("2026-03-07T10:00:00-05:00[America/New_York]")
        val after = ZonedDateTime.parse("2026-03-08T11:00:00-04:00[America/New_York]")
        val next = nextReminderOccurrence(anchor.toInstant().toEpochMilli(), "DAILY", after.toInstant().toEpochMilli(), anchor.zone)!!
        assertEquals(after.plusDays(1).withHour(10), Instant.ofEpochMilli(next).atZone(anchor.zone))
        assertNull(nextReminderOccurrence(0, "NONE", 10, ZoneId.of("UTC")))
    }

    private fun parsed(hint: String) = LocalReminderParser.parse(hint, now)
    private fun start(hint: String) = Instant.ofEpochMilli(parsed(hint).startTimeMillis).atZone(now.zone)

    // now = Saturday 2026-10-03 18:15 IST

    @Test fun weekdaysAndWeekends() {
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", parsed("every weekday at 9 am standup").repeat)
        assertEquals("2026-10-05T09:00+05:30[Asia/Kolkata]", start("every weekday at 9 am standup").toString())
        assertEquals("standup", parsed("every weekday at 9 am standup").title)
        assertEquals("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", parsed("remind on weekdays at 8:30 to take meds").repeat)
        assertEquals("FREQ=WEEKLY;BYDAY=SA,SU", parsed("every weekend at 10 am call home").repeat)
        assertEquals("2026-10-04T10:00+05:30[Asia/Kolkata]", start("every weekend at 10 am call home").toString())
    }

    @Test fun specificDaysOfWeek() {
        assertEquals("FREQ=WEEKLY;BYDAY=MO,WE", parsed("every monday and wednesday at 7 pm gym").repeat)
        assertEquals("FREQ=WEEKLY;BYDAY=TU,TH,SA", parsed("every tue, thu and sat at 6 am run").repeat)
        assertEquals("2026-10-05T19:00+05:30[Asia/Kolkata]", start("every monday and wednesday at 7 pm gym").toString())
    }

    @Test fun monthlyByDate() {
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=1", parsed("on the 1st of every month pay rent").repeat)
        assertEquals("2026-11-01T09:00+05:30[Asia/Kolkata]", start("on the 1st of every month pay rent").toString())
        assertEquals("pay rent", parsed("on the 1st of every month pay rent").title)
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=15", parsed("every month on the 15th at 10 am pay card bill").repeat)
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=5", parsed("monthly on the 5th send invoice").repeat)
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=20", parsed("every 20th check meter").repeat)
        assertEquals("2026-10-20T09:00+05:30[Asia/Kolkata]", start("every 20th check meter").toString())
    }

    @Test fun everyNHoursAndDays() {
        assertEquals("FREQ=HOURLY;INTERVAL=2", parsed("every 2 hours drink water").repeat)
        assertEquals(now.plusHours(2).withSecond(0).withNano(0), start("every 2 hours drink water"))
        assertEquals("drink water", parsed("every 2 hours drink water").title)
        assertEquals("FREQ=HOURLY", parsed("every hour stretch").repeat)
        assertEquals("FREQ=DAILY;INTERVAL=3", parsed("every 3 days at 8 pm water plants").repeat)
        assertEquals("FREQ=DAILY", parsed("every day at 7 am vitamins").repeat)
        assertEquals("2026-10-04T07:00+05:30[Asia/Kolkata]", start("every day at 7 am vitamins").toString())
    }

    @Test fun partsOfTheDay() {
        assertEquals("2026-10-04T09:00+05:30[Asia/Kolkata]", start("tomorrow morning call bank").toString())
        assertEquals("2026-10-03T19:00+05:30[Asia/Kolkata]", start("tonight take out trash").toString())
    }
}
