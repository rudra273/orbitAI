package com.example.orbitai.core.engine

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.example.orbitai.BuildConfig

/** Hardware backends, fastest first. */
enum class HardwareBackend(val label: String) { NPU("NPU"), GPU("GPU"), CPU("CPU") }

/** The user's "Run on" choice for a model. AUTO may fall back; the others never do. */
enum class BackendPreference(val backend: HardwareBackend?) {
    AUTO(null), NPU(HardwareBackend.NPU), GPU(HardwareBackend.GPU), CPU(HardwareBackend.CPU);

    companion object {
        fun of(backend: HardwareBackend) = entries.first { it.backend == backend }
    }
}

/** What an engine may try: the backends the model supports, the user's choice, and the speed-test winner. */
data class BackendPlan(
    val supported: List<HardwareBackend>,
    val preference: BackendPreference = BackendPreference.AUTO,
    val tested: HardwareBackend? = null,
)

/** The backend the user picked can't run this model. Only thrown for a picked backend, never for Auto. */
class BackendUnavailableException(val backend: HardwareBackend, message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

/** A loaded engine, the backend it runs on and, in Auto, the backend it should have used if that one failed. */
data class BackendLoad<T>(val value: T, val backend: HardwareBackend, val fellBackFrom: HardwareBackend?) {
    val label: String get() = fellBackFrom?.let { "${backend.label} (${it.label} failed)" } ?: backend.label
}

/**
 * Chooses which backends to try for a model. Before each attempt a marker is saved; if the app
 * dies during a native init (a crash, not an exception), the marker is still there on the next
 * load and that backend is recorded as crashed, so a bad driver can't crash the app every time.
 * Everything recorded is cleared when the app or the Android build changes.
 */
class BackendCascade(context: Context, private val modelKey: String) {

    private val prefs = context.getSharedPreferences("orbitai_backends", Context.MODE_PRIVATE)

    init {
        val stamp = deviceStamp()
        if (prefs.getString(STAMP_KEY, null) != stamp) {
            prefs.edit(commit = true) { clear(); putString(STAMP_KEY, stamp) }
        }
    }

    /** Loads with [plan]: a picked backend is tried alone; Auto walks the order until one works. */
    fun <T> load(plan: BackendPlan, create: (HardwareBackend) -> T): BackendLoad<T> {
        recordPendingCrash()
        val picked = plan.preference.backend?.takeIf { it in plan.supported }
        if (picked != null) {
            if (picked in crashed()) {
                throw BackendUnavailableException(
                    picked,
                    "${picked.label} closed Orbit the last time it loaded this model. " +
                        "In Settings > Models, set Run on to Auto, or pick ${picked.label} again to retry.",
                )
            }
            return try {
                BackendLoad(attempt(picked, create), picked, null)
            } catch (e: Exception) {
                throw BackendUnavailableException(
                    picked,
                    "${picked.label} couldn't run this model on your phone (${e.message}). " +
                        "In Settings > Models, set Run on to Auto or another option.",
                    e,
                )
            }
        }
        val expected = Planner.expected(plan.supported, plan.tested)
        var lastError: Throwable? = null
        for (backend in Planner.order(plan.supported, failed() + crashed(), plan.tested)) {
            try {
                return BackendLoad(attempt(backend, create), backend, expected.takeIf { it != backend })
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("No backend available")
    }

    /** Before a speed test: forgets ordinary failures so every backend is retried. Returns the ones that crashed the app. */
    fun prepareForTest(): Set<HardwareBackend> {
        recordPendingCrash()
        prefs.edit { remove(failedKey) }
        return crashed()
    }

    /** Called when the user picks [backend] themselves: it gets a fresh try. */
    fun forget(backend: HardwareBackend) {
        prefs.edit {
            putStringSet(failedKey, (failed() - backend).names())
            putStringSet(crashedKey, (crashed() - backend).names())
        }
    }

    /** Records that [backend] loaded but produced unusable output, so Auto skips it. */
    fun markFailed(backend: HardwareBackend) {
        if (backend == HardwareBackend.CPU) return
        prefs.edit { putStringSet(failedKey, (failed() + backend).names()) }
    }

    private fun <T> attempt(backend: HardwareBackend, create: (HardwareBackend) -> T): T {
        prefs.edit(commit = true) { putString(attemptKey, backend.name) }
        try {
            return create(backend)
        } catch (e: Exception) {
            markFailed(backend)
            throw e
        } finally {
            prefs.edit { remove(attemptKey) }
        }
    }

    private fun recordPendingCrash() {
        val crashedNow = prefs.getString(attemptKey, null)
            ?.let { runCatching { HardwareBackend.valueOf(it) }.getOrNull() }
        prefs.edit(commit = true) {
            remove(attemptKey)
            if (crashedNow != null && crashedNow != HardwareBackend.CPU) {
                putStringSet(crashedKey, (crashed() + crashedNow).names())
            }
        }
    }

    private val attemptKey get() = "attempt:$modelKey"
    private val failedKey get() = "failed:$modelKey"
    private val crashedKey get() = "crashed:$modelKey"

    private fun failed() = read(failedKey)
    private fun crashed() = read(crashedKey)

    private fun read(key: String): Set<HardwareBackend> =
        prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull { runCatching { HardwareBackend.valueOf(it) }.getOrNull() }.toSet()

    private fun Set<HardwareBackend>.names() = map { it.name }.toSet()

    object Planner {
        /** Auto order: the tested winner first, then fastest first; CPU is always tried. */
        fun order(supported: List<HardwareBackend>, failed: Set<HardwareBackend>, tested: HardwareBackend? = null): List<HardwareBackend> {
            val base = supported.filter { it != HardwareBackend.CPU && it !in failed }.sortedBy { it.ordinal } + HardwareBackend.CPU
            return if (tested != null && tested in base) listOf(tested) + (base - tested) else base
        }

        /** The backend Auto aims for; landing on another one means it fell back. */
        fun expected(supported: List<HardwareBackend>, tested: HardwareBackend?): HardwareBackend =
            tested?.takeIf { it in supported } ?: supported.minByOrNull { it.ordinal } ?: HardwareBackend.CPU
    }

    companion object {
        private const val STAMP_KEY = "device_stamp"

        /** Changes after an app update or an Android/driver update. */
        fun deviceStamp(): String = "${Build.FINGERPRINT}|${BuildConfig.VERSION_CODE}"
    }
}
