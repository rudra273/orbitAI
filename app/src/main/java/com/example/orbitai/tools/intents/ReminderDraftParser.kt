package com.example.orbitai.tools.intents

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderDraftParser {

    private val titleRegex = Regex("(?im)^title\\s*:\\s*(.+)$")
    private val descriptionRegex = Regex("(?im)^description\\s*:\\s*(.*)$")
    private val dateRegex = Regex("(?im)^date\\s*:\\s*(\\d{4}-\\d{2}-\\d{2})$")
    private val timeRegex = Regex("(?im)^time\\s*:\\s*(\\d{2}:\\d{2})$")
    private val durationRegex = Regex("(?im)^durationminutes\\s*:\\s*(\\d+)$")

    fun parse(
        modelOutput: String,
        topicHint: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): ReminderDraft {
        val trimmedOutput = modelOutput.trim()
        val title = titleRegex.find(trimmedOutput)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
            .ifBlank { topicHint.trim().take(60).ifBlank { "Reminder" } }

        val description = descriptionRegex.find(trimmedOutput)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()

        val date = dateRegex.find(trimmedOutput)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: now.toLocalDate()

        val time = timeRegex.find(trimmedOutput)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            ?: now.toLocalTime().withSecond(0).withNano(0).plusHours(1)

        val durationMinutes = durationRegex.find(trimmedOutput)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.coerceAtLeast(5)
            ?: 30

        val startDateTime = LocalDateTime.of(date, time)
        val endDateTime = startDateTime.plusMinutes(durationMinutes.toLong())
        val zoneId = ZoneId.systemDefault()

        return ReminderDraft(
            title = title,
            description = description,
            startTimeMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli(),
            endTimeMillis = endDateTime.atZone(zoneId).toInstant().toEpochMilli(),
        )
    }
}