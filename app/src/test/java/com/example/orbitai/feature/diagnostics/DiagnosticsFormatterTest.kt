package com.example.orbitai.feature.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsFormatterTest {

    private val info = DiagnosticsInfo(
        appVersion = "1.0.0 (10104)",
        device = "samsung SM-S928B (qcom)",
        androidVersion = "16 (API 36)",
        modelState = "gemma4-e2b on GPU",
        timestampMs = 1_790_000_000_000L,
    )

    @Test fun crashReportHasContextAndStackTrace() {
        val report = DiagnosticsFormatter.crashReport(info, "main", IllegalStateException("Model failed"))
        listOf("1.0.0 (10104)", "SM-S928B", "API 36", "gemma4-e2b on GPU", "IllegalStateException", "Model failed", "\"main\"")
            .forEach { assertTrue("missing $it", report.contains(it)) }
    }

    @Test fun secretsInExceptionMessagesAreRedacted() {
        val secrets = listOf(
            "hf_AbCdEf1234567890xyz",
            "AIzaSyD-1234567890abcdefghijklmnopq",
            "Bearer eyJhbGciOiJIUzI1NiJ9.payload",
            "https://example.org/x?key=supersecretvalue",
        )
        val report = DiagnosticsFormatter.crashReport(info, "main", RuntimeException("failed: ${secrets.joinToString(" ")}"))
        assertFalse(report.contains("hf_AbCdEf1234567890xyz"))
        assertFalse(report.contains("AIzaSyD-1234567890abcdefghijklmnopq"))
        assertFalse(report.contains("eyJhbGciOiJIUzI1NiJ9.payload"))
        assertFalse(report.contains("supersecretvalue"))
        assertTrue(report.contains("[REDACTED]"))
        assertTrue(report.contains("?key=[REDACTED]"))
    }

    @Test fun reportOnlyContainsWhatItIsGiven() {
        // The formatter has no access to chats or memories; a report is header fields plus details.
        val report = DiagnosticsFormatter.exitReport(info, "Closed by Android: low memory", "lmk", null)
        assertTrue(report.startsWith("=== OrbitAI diagnostics ==="))
        assertTrue(report.contains("Closed by Android: low memory"))
        assertTrue(report.lines().size < 12)
    }

    @Test fun longTracesAreCapped() {
        val report = DiagnosticsFormatter.exitReport(info, "ANR", null, "x".repeat(100_000))
        assertTrue(report.length < 17_000)
    }
}
