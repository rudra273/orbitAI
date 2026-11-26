package com.example.orbitai.feature.automation.reminder

import com.example.orbitai.core.database.ReminderEntity
import java.time.Instant
import java.time.ZoneId

/** Sections of the Reminders list. */
enum class ReminderGroup(val label: String) { TODAY("Today"), UPCOMING("Upcoming"), REPEATING("Repeating"), DONE("Done") }

object ReminderGroups {

    /** Today = due or overdue today; repeating reminders get their own section; finished ones go last. */
    fun group(reminders: List<ReminderEntity>, now: Long, zone: ZoneId = ZoneId.systemDefault()): Map<ReminderGroup, List<ReminderEntity>> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return reminders.groupBy { reminder ->
            when {
                reminder.completed -> ReminderGroup.DONE
                RepeatRule.parse(reminder.repeat) != RepeatRule.Once -> ReminderGroup.REPEATING
                reminder.delivered || !Instant.ofEpochMilli(reminder.triggerAt).atZone(zone).toLocalDate().isAfter(today) -> ReminderGroup.TODAY
                else -> ReminderGroup.UPCOMING
            }
        }.mapValues { (group, list) ->
            if (group == ReminderGroup.DONE) list.sortedByDescending { it.triggerAt } else list.sortedBy { it.triggerAt }
        }.toSortedMap(compareBy { it.ordinal })
    }
}
