package com.example.orbitai.feature.reporting

import com.example.orbitai.BuildConfig
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ContentReportRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun submit(responseText: String, note: String, source: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                check(BuildConfig.REPORT_ENDPOINT.isNotBlank()) {
                    "Reporting is not configured in this development build. Your report has not been sent."
                }
                require(responseText.isNotBlank()) { "Choose a response to report." }
                val body = JSONObject()
                    .put("response", responseText)
                    .put("note", note.trim())
                    .put("source", source)
                    .put("appVersion", BuildConfig.VERSION_NAME)
                    .toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(BuildConfig.REPORT_ENDPOINT).post(body).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Report could not be sent (HTTP ${response.code}). Please try again.")
                    }
                }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
