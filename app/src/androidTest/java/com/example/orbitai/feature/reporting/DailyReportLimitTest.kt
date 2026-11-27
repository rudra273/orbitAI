package com.example.orbitai.feature.reporting

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyReportLimitTest {
    @Test
    fun limitsThreeDistinctReportsAcrossInstancesAndResetsAtUtcMidnight() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "report-limit-test-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        var now = Instant.parse("2026-11-27T23:59:00Z")
        val nextDay = Instant.parse("2026-11-28T00:00:00Z")
        try {
            val initial = DailyReportLimit(prefs) { now }
            repeat(3) { index ->
                val id = "report-$index"
                assertNull(initial.retryAt(id))
                initial.record(id)
                initial.record(id)
            }
            val reopened = DailyReportLimit(prefs) { now }
            assertEquals(nextDay, reopened.retryAt("fourth"))
            assertNull(reopened.retryAt("report-0"))

            now = nextDay
            assertNull(reopened.retryAt("fourth"))
            reopened.record("fourth")
            assertNull(reopened.retryAt("fifth"))
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}
