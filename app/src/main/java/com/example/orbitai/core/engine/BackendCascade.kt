package com.example.orbitai.core.engine

import android.content.Context
import androidx.core.content.edit

/** Hardware backends, fastest first. */
enum class HardwareBackend(val label: String) { NPU("NPU"), GPU("GPU"), CPU("CPU") }

/**
 * Chooses which backends to try for a model, fastest first, skipping ones that already failed
 * for that model file. Before each attempt a marker is saved; if the app dies during a native
 * init (a crash, not an exception), the marker is still there on the next load and that backend
 * is treated as failed, so a bad driver can't crash the app every time.
 */
class BackendCascade(context: Context, private val modelKey: String) {

    private val prefs = context.getSharedPreferences("orbitai_backends", Context.MODE_PRIVATE)

    /** Backends to try, in order. CPU is always last and always tried. */
    fun order(supported: List<HardwareBackend>): List<HardwareBackend> {
        prefs.getString(attemptKey, null)?.let { crashed -> markFailed(HardwareBackend.valueOf(crashed)) }
        return Planner.order(supported, failed())
    }

    /** Runs [create] on each backend until one works. */
    fun <T> firstWorking(supported: List<HardwareBackend>, create: (HardwareBackend) -> T): Pair<T, HardwareBackend> {
        var lastError: Throwable? = null
        for (backend in order(supported)) {
            prefs.edit(commit = true) { putString(attemptKey, backend.name) }
            try {
                val result = create(backend)
                prefs.edit { remove(attemptKey) }
                return result to backend
            } catch (e: Exception) {
                prefs.edit { remove(attemptKey) }
                if (backend != HardwareBackend.CPU) markFailed(backend)
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("No backend available")
    }

    private val attemptKey get() = "attempt:$modelKey"
    private val failedKey get() = "failed:$modelKey"

    private fun failed(): Set<HardwareBackend> =
        prefs.getStringSet(failedKey, emptySet()).orEmpty().mapNotNull { runCatching { HardwareBackend.valueOf(it) }.getOrNull() }.toSet()

    private fun markFailed(backend: HardwareBackend) {
        prefs.edit {
            putStringSet(failedKey, failed().map { it.name }.toSet() + backend.name)
            remove(attemptKey)
        }
    }

    object Planner {
        fun order(supported: List<HardwareBackend>, failed: Set<HardwareBackend>): List<HardwareBackend> =
            (supported.filter { it != HardwareBackend.CPU && it !in failed }.sortedBy { it.ordinal }) + HardwareBackend.CPU
    }
}
