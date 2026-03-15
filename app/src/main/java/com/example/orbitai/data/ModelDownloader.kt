package com.example.orbitai.data

import android.content.Context
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
    "gemma2-2b"   to "https://huggingface.co/litert-community/Gemma2-2B-IT/resolve/main/gemma2-2b-it-cpu-int8.task?download=true",
)

class ModelDownloader(private val context: Context) {

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
        val dest = File(modelDir, fileName)
        val tmp  = File(modelDir, "$fileName.tmp")

        if (dest.exists()) {
            emit(DownloadProgress(modelId, dest.length(), dest.length(), DownloadStatus.COMPLETED))
            return@flow
        }

        // Only require a token for gated models
        if (requiresAuth && !tokenStore.hasToken()) {
            emit(DownloadProgress(modelId, status = DownloadStatus.FAILED,
                error = "No HuggingFace token. Add it in Settings."))
            return@flow
        }

        activeDownloads[modelId] = true
        emit(DownloadProgress(modelId, status = DownloadStatus.DOWNLOADING))

        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
            if (requiresAuth) {
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
            emit(DownloadProgress(modelId, downloaded, totalBytes, DownloadStatus.COMPLETED))

        } catch (e: Exception) {
            tmp.delete()
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