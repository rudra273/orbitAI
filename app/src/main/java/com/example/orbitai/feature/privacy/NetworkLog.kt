package com.example.orbitai.feature.privacy

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import java.io.File

/** One network call Orbit made: when, to which host, why, and how many bytes came back (-1 unknown). */
data class NetworkEvent(val timeMs: Long, val host: String, val purpose: String, val bytes: Long)

/**
 * A local list of the app's network calls for the last 30 days (f057). It stores only the host and
 * the purpose: never a path, query, header, request body or key.
 */
class NetworkLog(private val file: File, private val clock: () -> Long = System::currentTimeMillis) {

    @Synchronized
    fun record(host: String, purpose: String, bytes: Long = -1) {
        val cleanHost = host.filter { it.isLetterOrDigit() || it == '.' || it == '-' }.take(120)
        if (cleanHost.isEmpty()) return
        val cleanPurpose = purpose.filter { it != '\t' && it != '\n' && it != '\r' }.take(40)
        runCatching {
            file.parentFile?.mkdirs()
            file.appendText("${clock()}\t$cleanHost\t$cleanPurpose\t$bytes\n")
            if (file.length() > MAX_BYTES) rewrite(events())
        }
    }

    /** Newest first, only the last 30 days. */
    @Synchronized
    fun events(): List<NetworkEvent> {
        if (!file.exists()) return emptyList()
        val cutoff = clock() - RETENTION_MS
        return runCatching {
            file.readLines().mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size != 4) return@mapNotNull null
                val time = parts[0].toLongOrNull() ?: return@mapNotNull null
                NetworkEvent(time, parts[1], parts[2], parts[3].toLongOrNull() ?: -1)
            }.filter { it.timeMs >= cutoff }.sortedByDescending { it.timeMs }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun clear() {
        file.delete()
    }

    private fun rewrite(events: List<NetworkEvent>) {
        file.writeText(events.take(KEEP_LINES).reversed().joinToString("") { "${it.timeMs}\t${it.host}\t${it.purpose}\t${it.bytes}\n" })
    }

    companion object {
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
        private const val MAX_BYTES = 200_000L
        private const val KEEP_LINES = 1000

        @Volatile var shared: NetworkLog? = null
            private set

        fun init(context: Context) {
            if (shared == null) shared = NetworkLog(File(context.applicationContext.filesDir, "network_log.tsv"))
        }

        /** Hosts only; for calls made by libraries that don't use OkHttp. */
        fun note(host: String, purpose: String) {
            shared?.record(host, purpose)
        }
    }
}

/** Logs every call made through an OkHttp client: host and purpose only. */
class NetworkLogInterceptor(private val purpose: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val host = chain.request().url.host
        val response = try {
            chain.proceed(chain.request())
        } catch (e: java.io.IOException) {
            NetworkLog.shared?.record(host, purpose)
            throw e
        }
        NetworkLog.shared?.record(host, purpose, response.body?.contentLength() ?: -1)
        return response
    }
}
