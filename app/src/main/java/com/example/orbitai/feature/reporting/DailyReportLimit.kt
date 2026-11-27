package com.example.orbitai.feature.reporting

import android.content.SharedPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal class DailyReportLimit(
    private val prefs: SharedPreferences,
    private val clock: () -> Instant = Instant::now,
) {
    fun retryAt(submissionId: String): Instant? {
        val day = utcDay()
        val receipts = receipts(day)
        return day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            .takeIf { submissionId !in receipts && receipts.size >= DAILY_LIMIT }
    }

    fun record(submissionId: String) {
        val day = utcDay()
        prefs.edit()
            .putLong("utc_day", day.toEpochDay())
            .putStringSet("report_ids", receipts(day) + submissionId)
            .apply()
    }

    private fun utcDay(): LocalDate = LocalDate.ofInstant(clock(), ZoneOffset.UTC)

    private fun receipts(day: LocalDate): Set<String> =
        if (prefs.getLong("utc_day", Long.MIN_VALUE) == day.toEpochDay()) {
            prefs.getStringSet("report_ids", null).orEmpty()
        } else {
            emptySet()
        }

    private companion object {
        const val DAILY_LIMIT = 3
    }
}
