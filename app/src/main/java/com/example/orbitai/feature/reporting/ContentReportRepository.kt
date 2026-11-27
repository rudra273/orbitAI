package com.example.orbitai.feature.reporting

import android.content.Context
import com.example.orbitai.BuildConfig
import java.io.IOException
import java.text.DateFormat
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ContentReportRepository(context: Context) {
    companion object {
        val isConfigured: Boolean get() = BuildConfig.REPORT_ENDPOINT.isNotBlank()

        private val submissionLock = Mutex()
    }

    private val dailyLimit = DailyReportLimit(
        context.applicationContext.getSharedPreferences("orbit_ai_reporting", Context.MODE_PRIVATE),
    )

    private val client = OkHttpClient.Builder()
        .addInterceptor(com.example.orbitai.feature.privacy.NetworkLogInterceptor("Content report"))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun submit(responseText: String, note: String, source: String, submissionId: String): Result<String> =
        withContext(Dispatchers.IO) {
            submissionLock.withLock { submitLocked(responseText, note, source, submissionId) }
        }

    private fun submitLocked(responseText: String, note: String, source: String, submissionId: String): Result<String> =
        try {
            check(isConfigured) {
                "Reporting is not configured in this development build. Your report has not been sent."
            }
            dailyLimit.retryAt(submissionId)?.let { throw IOException(retryMessage(it)) }
            require(responseText.isNotBlank()) { "Choose a response to report." }
            val body = JSONObject()
                .put("submissionId", submissionId)
                .put("response", responseText)
                .put("note", note.trim())
                .put("source", source)
                .put("appVersion", BuildConfig.VERSION_NAME)
                .toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(BuildConfig.REPORT_ENDPOINT)
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 429) {
                    val retrySeconds = response.header("Retry-After")?.toLongOrNull()?.takeIf { it in 1..86_400 }
                    val retryAt = retrySeconds?.let { Instant.now().plusSeconds(it) }
                    throw IOException(retryAt?.let(::retryMessage) ?: "Too many reports. Try again later.")
                }
                if (!response.isSuccessful) {
                    throw IOException("Report could not be sent (HTTP ${response.code}). Please try again.")
                }
                val receipt = JSONObject(response.body?.string().orEmpty()).optString("reportId")
                if (receipt != submissionId) throw IOException("Report receipt could not be verified. Please try again.")
                dailyLimit.record(submissionId)
                Result.success(receipt)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun retryMessage(retryAt: Instant): String {
        val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date.from(retryAt))
        return "Reporting limit reached. Try again after $time."
    }
}
