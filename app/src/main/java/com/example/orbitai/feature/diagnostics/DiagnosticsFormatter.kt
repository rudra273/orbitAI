package com.example.orbitai.feature.diagnostics

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Facts about the app and device included in every report. Never chat, memory or document content. */
data class DiagnosticsInfo(
    val appVersion: String,
    val device: String,
    val androidVersion: String,
    val modelState: String?,
    val timestampMs: Long,
)

/** Builds plain-text diagnostics reports and redacts secrets that may appear in exception messages. */
object DiagnosticsFormatter {

    private const val MAX_TRACE_CHARS = 16_000
    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")

    private val secretPatterns = listOf(
        Regex("hf_[A-Za-z0-9]{8,}"),                  // Hugging Face tokens
        Regex("AIza[0-9A-Za-z_\\-]{20,}"),            // Google API keys
        Regex("(?i)(bearer\\s+)[A-Za-z0-9._\\-]{8,}"), // Authorization headers
        Regex("(?i)([?&](?:key|token|access_token)=)[^&\\s]+"),
    )

    fun redact(text: String): String = secretPatterns.fold(text) { acc, pattern ->
        pattern.replace(acc) { match ->
            val prefix = match.groupValues.getOrNull(1).orEmpty()
            "$prefix[REDACTED]"
        }
    }

    fun crashReport(info: DiagnosticsInfo, threadName: String, throwable: Throwable): String =
        report(info, "Crash (uncaught exception on thread \"$threadName\")", throwable.stackTraceToString())

    fun exitReport(info: DiagnosticsInfo, reason: String, description: String?, trace: String?): String =
        report(info, reason, listOfNotNull(description?.takeIf { it.isNotBlank() }, trace?.takeIf { it.isNotBlank() }).joinToString("\n\n"))

    private fun report(info: DiagnosticsInfo, kind: String, details: String): String = buildString {
        appendLine("=== OrbitAI diagnostics ===")
        appendLine("Type: $kind")
        appendLine("Time: ${timeFormat.format(Instant.ofEpochMilli(info.timestampMs).atZone(ZoneId.systemDefault()))}")
        appendLine("App version: ${info.appVersion}")
        appendLine("Device: ${info.device}")
        appendLine("Android: ${info.androidVersion}")
        appendLine("Model: ${info.modelState ?: "none loaded"}")
        if (details.isNotBlank()) {
            appendLine()
            appendLine(redact(details).take(MAX_TRACE_CHARS))
        }
    }
}
