package com.example.orbitai.core.model

import android.content.Context
import android.util.Log
import com.example.orbitai.core.common.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class DownloadProgress(
    val modelId: String,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val error: String? = null,
) {
    val progress: Float get() = if (totalBytes > 0) bytesDownloaded / totalBytes.toFloat() else 0f
    val progressPercent: Int get() = (progress * 100).toInt()
}

enum class DownloadStatus { IDLE, DOWNLOADING, PAUSED, COMPLETED, FAILED }

val MODEL_DOWNLOAD_URLS = mapOf(
    "gemma3-1b"   to "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task?download=true",
    "gemma3-4b"   to "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm/resolve/main/gemma-3n-E4B-it-int4.litertlm?download=true",
    "gemma3-2b"   to "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm?download=true",
    "gemma4-e2b"  to "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true",
    "gemma4-e4b"  to "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm?download=true",
    "gemma2-2b"   to "https://huggingface.co/litert-community/Gemma2-2B-IT/resolve/main/gemma2-2b-it-cpu-int8.task?download=true",
)

val MODEL_DOWNLOAD_REQUIRES_AUTH = mapOf(
    "gemma3-1b" to true,
    "gemma3-4b" to true,
    "gemma3-2b" to true,
    "gemma4-e2b" to true,
    "gemma4-e4b" to true,
    "gemma2-2b" to true,
)

class ModelDownloader(private val context: Context) {
    companion object {
        private const val TAG = "ModelDownloader"
    }

    private val tokenStore = TokenStore(context)

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val activeDownloads = mutableMapOf<String, Boolean>()

    val modelDir: File = File(context.getExternalFilesDir(null), "models").also { it.mkdirs() }

    fun isDownloaded(model: LlmModel): Boolean = File(modelDir, model.fileName).exists()

    fun modelPath(fileName: String): String = File(modelDir, fileName).absolutePath

    fun download(
        modelId: String,
        url: String,
        fileName: String,
        requiresAuth: Boolean = true,
    ): Flow<DownloadProgress> = flow {
        Log.d(TAG, "Starting download for $modelId -> $fileName")
        val dest = File(modelDir, fileName)
        val tmp  = File(modelDir, "$fileName.tmp")

        if (dest.exists()) {
            emit(DownloadProgress(modelId, dest.length(), dest.length(), DownloadStatus.COMPLETED))
            return@flow
        }

        activeDownloads[modelId] = true
        emit(DownloadProgress(modelId, status = DownloadStatus.DOWNLOADING))

        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
            if (requiresAuth && tokenStore.hasToken()) {
                requestBuilder.header("Authorization", "Bearer ${tokenStore.huggingFaceToken}")
            }
            val request = requestBuilder.build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errMsg = when (response.code) {
                    401  -> "Invalid token. Check your HuggingFace token in Settings."
                    403  -> "Access denied. Accept the model license on HuggingFace first."
                    404  -> "Model file not found."
                    else -> "HTTP error ${response.code}"
                }
                Log.e(TAG, "Download failed for $modelId: ${response.code} $errMsg")
                emit(DownloadProgress(modelId, status = DownloadStatus.FAILED, error = errMsg))
                return@flow
            }

            val body       = response.body ?: run {
                emit(DownloadProgress(modelId, status = DownloadStatus.FAILED, error = "Empty response"))
                return@flow
            }
            val totalBytes = body.contentLength()
            var downloaded = 0L

            tmp.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var bytes  = input.read(buffer)
                    while (bytes >= 0) {
                        if (!coroutineContext.isActive || activeDownloads[modelId] == false) {
                            tmp.delete()
                            emit(DownloadProgress(modelId, status = DownloadStatus.FAILED,
                                error = "Cancelled"))
                            return@flow
                        }
                        out.write(buffer, 0, bytes)
                        downloaded += bytes
                        emit(DownloadProgress(modelId, downloaded, totalBytes,
                            DownloadStatus.DOWNLOADING))
                        bytes = input.read(buffer)
                    }
                }
            }

            tmp.renameTo(dest)
            Log.d(TAG, "Download completed for $modelId -> ${dest.absolutePath}")
            emit(DownloadProgress(modelId, downloaded, totalBytes, DownloadStatus.COMPLETED))

        } catch (e: Exception) {
            tmp.delete()
            Log.e(TAG, "Download exception for $modelId", e)
            emit(DownloadProgress(modelId, status = DownloadStatus.FAILED, error = e.message))
        } finally {
            activeDownloads.remove(modelId)
        }
    }.flowOn(Dispatchers.IO)

    fun cancelDownload(modelId: String) {
        activeDownloads[modelId] = false
    }

    fun deleteModel(model: LlmModel) {
        File(modelDir, model.fileName).delete()
    }

    fun isEmbeddingDownloaded(model: EmbeddingModelConfig): Boolean {
        if (!File(modelDir, model.fileName).exists()) return false
        if (model.tokenizerFileName != null && !File(modelDir, model.tokenizerFileName).exists()) return false
        return true
    }

    fun deleteEmbeddingModel(model: EmbeddingModelConfig) {
        File(modelDir, model.fileName).delete()
        model.tokenizerFileName?.let { File(modelDir, it).delete() }
    }
}
