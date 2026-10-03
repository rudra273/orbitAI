package com.example.orbitai.core.model

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

sealed interface FileDownloadResult {
    data object Completed : FileDownloadResult
    /** [canResume] is true when a partial file was kept and retrying later should continue it. */
    data class Failed(val message: String, val canResume: Boolean = false) : FileDownloadResult
}

/** Single-file HTTP download with resume and integrity checks. Free of Android context so it can be unit-tested. */
class ResumableFileDownloader(
    private val client: OkHttpClient,
    private val bearerToken: () -> String?,
) {
    companion object {
        private const val TAG = "ResumableFileDownloader"
        private const val PROGRESS_STEP_BYTES = 1L * 1024 * 1024
    }

    /**
     * Downloads [url] into [dest] through `<dest>.tmp`, resuming an existing partial file with an
     * HTTP Range request. The partial file is kept on network errors and when the coroutine is
     * cancelled (app closed, worker stopped), and discarded only when [isCancelled] returns true
     * or when verification fails. The file is renamed into place only after its size and
     * SHA-256 (when known) match.
     */
    suspend fun download(
        url: String,
        dest: File,
        expectedSize: Long?,
        expectedSha256: String?,
        requiresAuth: Boolean,
        isCancelled: () -> Boolean = { false },
        onProgress: suspend (bytesDone: Long, totalBytes: Long, status: DownloadStatus) -> Unit = { _, _, _ -> },
    ): FileDownloadResult {
        coroutineContext.ensureActive()
        dest.parentFile?.mkdirs()
        val tmp = File(dest.absolutePath + ".tmp")
        var existing = if (tmp.isFile) tmp.length() else 0L
        if (expectedSize != null && existing > expectedSize) {
            tmp.delete()
            existing = 0L
        }

        try {
            if (expectedSize == null || existing < expectedSize) {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                if (requiresAuth) {
                    bearerToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                }
                if (existing > 0L) {
                    Log.d(TAG, "Resuming ${dest.name} from byte $existing")
                    requestBuilder.header("Range", "bytes=$existing-")
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (response.code == 416) {
                        // Server says the range is past the end: the partial file is stale.
                        tmp.delete()
                        return FileDownloadResult.Failed("Download state was out of date. Tap Download to start again.")
                    }
                    if (!response.isSuccessful) {
                        val errMsg = httpErrorMessage(response.code)
                        Log.e(TAG, "Download failed for ${dest.name}: ${response.code} $errMsg")
                        return FileDownloadResult.Failed(errMsg, canResume = response.code >= 500)
                    }
                    val body = response.body ?: return FileDownloadResult.Failed("Empty response")

                    // 206 = server honoured the Range header; anything else restarts from zero.
                    val append = existing > 0L && response.code == 206
                    if (!append) existing = 0L
                    val totalBytes = expectedSize
                        ?: body.contentLength().takeIf { it >= 0 }?.plus(existing)
                        ?: -1L

                    var downloaded = existing
                    var lastReported = downloaded
                    coroutineContext.ensureActive()
                    FileOutputStream(tmp, append).use { out ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var bytes = input.read(buffer)
                            while (bytes >= 0) {
                                coroutineContext.ensureActive()
                                if (isCancelled()) {
                                    tmp.delete()
                                    return FileDownloadResult.Failed("Cancelled")
                                }
                                out.write(buffer, 0, bytes)
                                downloaded += bytes
                                if (downloaded - lastReported >= PROGRESS_STEP_BYTES) {
                                    lastReported = downloaded
                                    onProgress(downloaded, totalBytes, DownloadStatus.DOWNLOADING)
                                }
                                bytes = input.read(buffer)
                            }
                        }
                    }
                    onProgress(downloaded, totalBytes, DownloadStatus.DOWNLOADING)
                }
            }

            if (expectedSize != null && tmp.length() != expectedSize) {
                Log.e(TAG, "Size mismatch for ${dest.name}: ${tmp.length()} != $expectedSize")
                tmp.delete()
                return FileDownloadResult.Failed("Download corrupted (incomplete file). Tap Download to retry.")
            }
            if (expectedSha256 != null) {
                onProgress(tmp.length(), tmp.length(), DownloadStatus.VERIFYING)
                val actual = sha256Hex(tmp)
                if (!actual.equals(expectedSha256, ignoreCase = true)) {
                    Log.e(TAG, "SHA-256 mismatch for ${dest.name}: $actual != $expectedSha256")
                    tmp.delete()
                    return FileDownloadResult.Failed("Download corrupted (checksum mismatch). Tap Download to retry.")
                }
            }
            if (!tmp.renameTo(dest)) {
                return FileDownloadResult.Failed("Unable to finalize ${dest.name}")
            }
            return FileDownloadResult.Completed
        } catch (e: CancellationException) {
            // Keep the partial file so the next attempt resumes.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Download exception for ${dest.name} (partial file kept for resume)", e)
            return FileDownloadResult.Failed(
                "Download interrupted${e.message?.let { ": $it" } ?: ""}. Tap Download to resume.",
                canResume = true,
            )
        }
    }

    private suspend fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            var bytes = input.read(buffer)
            while (bytes >= 0) {
                coroutineContext.ensureActive()
                digest.update(buffer, 0, bytes)
                bytes = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun httpErrorMessage(code: Int): String = when (code) {
        401  -> "Invalid token. Check your HuggingFace token in Settings."
        403  -> "Access denied. Accept the model license on HuggingFace first."
        404  -> "Model file not found."
        else -> "HTTP error $code"
    }
}
