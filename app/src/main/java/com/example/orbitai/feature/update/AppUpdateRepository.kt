package com.example.orbitai.feature.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppUpdateInfo(
    val installedVersion: String,
    val latestVersion: String,
    val isUpdateAvailable: Boolean,
    val releaseUrl: String,
    val downloadUrl: String?,
)

class AppUpdateRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun installedVersion(): String {
        return runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    suspend fun fetchLatestRelease(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "OrbitAI-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("GitHub release check failed with HTTP ${response.code}")
                }

                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val installedVersion = installedVersion().normalizeVersion()
                val latestVersion = json.optString("tag_name").normalizeVersion()
                val releaseUrl = json.optString("html_url")
                AppUpdateInfo(
                    installedVersion = installedVersion.ifBlank { "Unknown" },
                    latestVersion = latestVersion.ifBlank { "Unknown" },
                    isUpdateAvailable = compareVersions(latestVersion, installedVersion) > 0,
                    releaseUrl = releaseUrl,
                    downloadUrl = null,
                )
            }
        }
    }

    fun openReleasePage(releaseUrl: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/rudra273/orbitAI/releases/latest"

        internal fun compareVersions(left: String, right: String): Int {
            val leftParts = left.normalizeVersionParts()
            val rightParts = right.normalizeVersionParts()
            val maxSize = maxOf(leftParts.size, rightParts.size)

            for (index in 0 until maxSize) {
                val leftValue = leftParts.getOrElse(index) { 0 }
                val rightValue = rightParts.getOrElse(index) { 0 }
                if (leftValue != rightValue) {
                    return leftValue.compareTo(rightValue)
                }
            }
            return 0
        }

        private fun String.normalizeVersion(): String {
            return trim()
                .removePrefix("v")
                .substringBefore('-')
        }

        private fun String.normalizeVersionParts(): List<Int> {
            return normalizeVersion()
                .split('.')
                .mapNotNull { it.toIntOrNull() }
        }
    }
}
