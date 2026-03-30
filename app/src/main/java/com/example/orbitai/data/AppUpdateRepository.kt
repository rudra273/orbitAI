package com.example.orbitai.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
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
                val assets = json.optJSONArray("assets")

                var apkUrl: String? = null
                if (assets != null) {
                    for (index in 0 until assets.length()) {
                        val asset = assets.optJSONObject(index) ?: continue
                        val name = asset.optString("name")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                AppUpdateInfo(
                    installedVersion = installedVersion.ifBlank { "Unknown" },
                    latestVersion = latestVersion.ifBlank { "Unknown" },
                    isUpdateAvailable = compareVersions(latestVersion, installedVersion) > 0,
                    releaseUrl = releaseUrl,
                    downloadUrl = apkUrl,
                )
            }
        }
    }

    suspend fun downloadApk(
        downloadUrl: String,
        versionName: String,
        onProgress: (Int) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val targetDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "updates"
            ).also { it.mkdirs() }
            val sanitizedVersion = versionName.ifBlank { "latest" }.replace("[^0-9A-Za-z._-]".toRegex(), "_")
            val targetFile = File(targetDir, "orbitai-$sanitizedVersion.apk")
            val tmpFile = File(targetDir, "${targetFile.name}.tmp")

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "OrbitAI-Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("APK download failed with HTTP ${response.code}")
                }

                val body = response.body ?: error("Empty APK response")
                val totalBytes = body.contentLength().coerceAtLeast(1L)
                var bytesRead = 0L
                onProgress(0)

                tmpFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            bytesRead += count
                            val progress = ((bytesRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                            onProgress(progress)
                        }
                    }
                }
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tmpFile.renameTo(targetFile)) {
                error("Unable to prepare downloaded APK")
            }
            onProgress(100)
            targetFile
        }
    }

    fun launchInstaller(apkFile: File): Result<Unit> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            error("Allow 'Install unknown apps' for OrbitAI, then tap Install again.")
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
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
