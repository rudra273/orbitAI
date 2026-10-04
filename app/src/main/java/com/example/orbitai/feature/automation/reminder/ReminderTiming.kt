package com.example.orbitai.feature.automation.reminder

import java.time.ZoneId

/** Next occurrence of a repeating reminder (see [RepeatRule]); null when it doesn't repeat. */
fun nextReminderOccurrence(anchor: Long, repeat: String, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? =
    RepeatRule.parse(repeat).next(anchor, now, zone)
