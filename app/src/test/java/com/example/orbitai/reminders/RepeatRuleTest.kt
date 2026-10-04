package com.example.orbitai.reminders

import com.example.orbitai.core.database.ReminderEntity
import com.example.orbitai.feature.automation.reminder.ReminderGroup
import com.example.orbitai.feature.automation.reminder.ReminderGroups
import com.example.orbitai.feature.automation.reminder.ReminderScheduler
import com.example.orbitai.feature.automation.reminder.RepeatRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class RepeatRuleTest {

    private val ist = ZoneId.of("Asia/Kolkata")
    private val ny = ZoneId.of("America/New_York")
    private fun at(text: String) = ZonedDateTime.parse(text).toInstant().toEpochMilli()
    private fun show(millis: Long?, zone: ZoneId) = Instant.ofEpochMilli(millis!!).atZone(zone).toString()

    @Test
    fun encodingRoundTripsAndLegacyValuesParse() {
        listOf(
            RepeatRule.Once,
            RepeatRule.Every(1, RepeatRule.Unit.DAYS),
            RepeatRule.Every(3, RepeatRule.Unit.HOURS),
            RepeatRule.Every(2, RepeatRule.Unit.WEEKS),
            RepeatRule.OnDays(RepeatRule.WEEKDAYS),
            RepeatRule.Monthly(31),
        ).forEach { assertEquals(it, RepeatRule.parse(it.encode())) }
        assertEquals(RepeatRule.Every(1, RepeatRule.Unit.DAYS), RepeatRule.parse("DAILY"))
        assertEquals(RepeatRule.Every(1, RepeatRule.Unit.WEEKS), RepeatRule.parse("WEEKLY"))
        assertEquals(RepeatRule.Once, RepeatRule.parse("NONE"))
        assertEquals(RepeatRule.Once, RepeatRule.parse("FREQ=YEARLY"))
        assertEquals("Weekdays", RepeatRule.OnDays(RepeatRule.WEEKDAYS).label)
        assertEquals("Monthly on the 22nd", RepeatRule.Monthly(22).label)
        assertEquals("Every 2 hours", RepeatRule.Every(2, RepeatRule.Unit.HOURS).label)
    }

    @Test
    fun weekdaysSkipTheWeekend() {
        val friday9 = at("2026-10-02T09:00+05:30[Asia/Kolkata]")
        val next = RepeatRule.OnDays(RepeatRule.WEEKDAYS).next(friday9, at("2026-10-02T09:01+05:30[Asia/Kolkata]"), ist)
        assertEquals("2026-10-05T09:00+05:30[Asia/Kolkata]", show(next, ist))
    }

    @Test
    fun monthlyUsesTheLastDayOfShortMonths() {
        val jan31 = at("2026-01-31T08:00+05:30[Asia/Kolkata]")
        val rule = RepeatRule.Monthly(31)
        val feb = rule.next(jan31, at("2026-01-31T08:01+05:30[Asia/Kolkata]"), ist)
        assertEquals("2026-02-28T08:00+05:30[Asia/Kolkata]", show(feb, ist))
        assertEquals("2026-03-31T08:00+05:30[Asia/Kolkata]", show(rule.next(jan31, feb!!, ist), ist))
    }

    @Test
    fun everyNHoursUsesElapsedTimeAcrossDst() {
        // 2026-03-08 02:00 New York clocks jump to 03:00.
        val anchor = at("2026-03-08T00:30-05:00[America/New_York]")
        val next = RepeatRule.Every(2, RepeatRule.Unit.HOURS).next(anchor, anchor + 1, ny)
        assertEquals("2026-03-08T03:30-04:00[America/New_York]", show(next, ny))
    }

    @Test
    fun dailyAndWeekdayKeepWallClockAcrossDst() {
        val anchor = at("2026-03-06T09:00-05:00[America/New_York]") // Friday
        val nextWeekday = RepeatRule.OnDays(RepeatRule.WEEKDAYS).next(anchor, anchor + 1, ny)
        assertEquals("2026-03-09T09:00-04:00[America/New_York]", show(nextWeekday, ny))
        val everyTwoDays = RepeatRule.Every(2, RepeatRule.Unit.DAYS).next(anchor, anchor + 1, ny)
        assertEquals("2026-03-08T09:00-04:00[America/New_York]", show(everyTwoDays, ny))
    }

    @Test
    fun timeZoneChangeKeepsTheSameMoment() {
        // Created in India for 9:00. Reminders store an instant, not a zone, so after moving to
        // New York it keeps firing at the same moment (23:30 there), like a calendar event in its zone.
        val anchor = at("2026-10-05T09:00+05:30[Asia/Kolkata]")
        val now = at("2026-10-06T10:00-04:00[America/New_York]")
        val next = RepeatRule.Every(1, RepeatRule.Unit.DAYS).next(anchor, now, ny)
        assertEquals("2026-10-06T23:30-04:00[America/New_York]", show(next, ny))
    }

    @Test
    fun onceNeverRepeats() {
        assertNull(RepeatRule.Once.next(0, 10, ist))
        assertNull(RepeatRule.OnDays(emptySet()).next(0, 10, ist))
    }

    @Test
    fun snoozeTargets() {
        val now = ZonedDateTime.parse("2026-10-03T18:15+05:30[Asia/Kolkata]")
        assertEquals(now.plusMinutes(10).toInstant().toEpochMilli(), ReminderScheduler.snoozeUntil(ReminderScheduler.SNOOZE_10_MIN, now))
        assertEquals(now.plusHours(1).toInstant().toEpochMilli(), ReminderScheduler.snoozeUntil(ReminderScheduler.SNOOZE_HOUR, now))
        assertEquals(at("2026-10-04T09:00+05:30[Asia/Kolkata]"), ReminderScheduler.snoozeUntil(ReminderScheduler.SNOOZE_TOMORROW, now))
    }

    @Test
    fun remindersAreGrouped() {
        val now = at("2026-10-03T18:15+05:30[Asia/Kolkata]")
        fun r(id: String, at: String, repeat: String = "NONE", done: Boolean = false, delivered: Boolean = false) =
            ReminderEntity(id, id, "", at(at), delivered = delivered, repeat = repeat, completed = done)
        val groups = ReminderGroups.group(
            listOf(
                r("later-today", "2026-10-03T21:00+05:30[Asia/Kolkata]"),
                r("overdue", "2026-10-03T09:00+05:30[Asia/Kolkata]", delivered = true),
                r("next-week", "2026-10-10T09:00+05:30[Asia/Kolkata]"),
                r("gym", "2026-10-05T07:00+05:30[Asia/Kolkata]", repeat = "FREQ=WEEKLY;BYDAY=MO"),
                r("old", "2026-09-01T09:00+05:30[Asia/Kolkata]", done = true),
            ),
            now,
            ist,
        )
        assertEquals(listOf(ReminderGroup.TODAY, ReminderGroup.UPCOMING, ReminderGroup.REPEATING, ReminderGroup.DONE), groups.keys.toList())
        assertEquals(listOf("overdue", "later-today"), groups.getValue(ReminderGroup.TODAY).map { it.id })
        assertEquals(DayOfWeek.MONDAY, Instant.ofEpochMilli(groups.getValue(ReminderGroup.REPEATING).single().triggerAt).atZone(ist).dayOfWeek)
    }
}
