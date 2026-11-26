package com.example.orbitai.feature.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Keeps the last few diagnostics reports in app-private storage. Nothing leaves the device unless
 * the user shares a report from Settings.
 */
class DiagnosticsStore(context: Context) {

    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "diagnostics").also { it.mkdirs() }
    private val prefs = appContext.getSharedPreferences("orbitai_diagnostics_prefs", Context.MODE_PRIVATE)

    private val _pendingPrompt = MutableStateFlow(prefs.getBoolean(KEY_PENDING_PROMPT, false))
    /** True after an unexpected exit until the user reviews or dismisses the prompt. */
    val pendingPrompt: StateFlow<Boolean> = _pendingPrompt.asStateFlow()

    /** Last model state ("loading <id>" / "<id> on <backend>"), so exit reports can name it. */
    @Volatile var modelState: String? = prefs.getString(KEY_MODEL_STATE, null)
        set(value) {
            field = value
            prefs.edit().putString(KEY_MODEL_STATE, value).commit()
        }

    fun info(timestampMs: Long = System.currentTimeMillis()) = DiagnosticsInfo(
        appVersion = appVersion(),
        device = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.HARDWARE})",
        androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        modelState = modelState,
        timestampMs = timestampMs,
    )

    /** Called from the uncaught-exception handler; must not throw. */
    fun recordCrash(thread: Thread, throwable: Throwable) {
        runCatching {
            write(DiagnosticsFormatter.crashReport(info(), thread.name, throwable))
            prefs.edit().putBoolean(KEY_PENDING_PROMPT, true).commit()
        }
    }

    /** Records low-memory kills, ANRs and native crashes Android reported since the last check. */
    fun recordExitReasons() {
        runCatching {
            val manager = appContext.getSystemService(ActivityManager::class.java)
            val lastChecked = prefs.getLong(KEY_LAST_EXIT_CHECK, 0L)
            val exits = manager.getHistoricalProcessExitReasons(appContext.packageName, 0, 10)
                .filter { it.timestamp > lastChecked && it.reason in REPORTED_REASONS }
            exits.sortedBy { it.timestamp }.forEach { exit ->
                val trace = if (exit.reason == ApplicationExitInfo.REASON_ANR) {
                    exit.traceInputStream?.use { it.readBytes().decodeToString().take(8_000) }
                } else null
                write(DiagnosticsFormatter.exitReport(info(exit.timestamp), reasonLabel(exit.reason), exit.description, trace))
            }
            // On the first check, exits from before diagnostics existed are recorded but not prompted.
            val prompt = exits.isNotEmpty() && lastChecked != 0L
            prefs.edit()
                .putLong(KEY_LAST_EXIT_CHECK, manager.getHistoricalProcessExitReasons(appContext.packageName, 0, 1).firstOrNull()?.timestamp ?: System.currentTimeMillis())
                .apply { if (prompt) putBoolean(KEY_PENDING_PROMPT, true) }
                .apply()
            if (prompt) _pendingPrompt.value = true
        }.onFailure { Log.w(TAG, "Couldn't read exit reasons", it) }
    }

    /** All stored reports, newest first. */
    fun readReports(): String =
        reportFiles().joinToString("\n\n") { it.readText() }

    fun hasReports(): Boolean = reportFiles().isNotEmpty()

    fun dismissPrompt() {
        prefs.edit().putBoolean(KEY_PENDING_PROMPT, false).apply()
        _pendingPrompt.value = false
    }

    fun clear() {
        reportFiles().forEach { it.delete() }
        dismissPrompt()
    }

    private fun write(report: String) {
        File(dir, "report-${System.currentTimeMillis()}.txt").writeText(report)
        reportFiles().drop(MAX_REPORTS).forEach { it.delete() }
    }

    private fun reportFiles(): List<File> =
        dir.listFiles { file -> file.name.startsWith("report-") }?.sortedByDescending { it.name }.orEmpty()

    private fun appVersion(): String = runCatching {
        val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        "${info.versionName} (${info.longVersionCode})"
    }.getOrDefault("unknown")

    private fun reasonLabel(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_LOW_MEMORY -> "Closed by Android: low memory"
        ApplicationExitInfo.REASON_ANR -> "App not responding (ANR)"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native crash"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "Closed by Android: excessive resource use"
        else -> "Exit reason $reason"
    }

    companion object {
        private const val TAG = "Diagnostics"
        private const val MAX_REPORTS = 10
        private const val KEY_PENDING_PROMPT = "pending_prompt"
        private const val KEY_LAST_EXIT_CHECK = "last_exit_check"
        private const val KEY_MODEL_STATE = "model_state"
        private val REPORTED_REASONS = setOf(
            ApplicationExitInfo.REASON_LOW_MEMORY,
            ApplicationExitInfo.REASON_ANR,
            ApplicationExitInfo.REASON_CRASH_NATIVE,
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
        )
    }
}
