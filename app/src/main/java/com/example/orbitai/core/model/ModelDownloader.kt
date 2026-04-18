package com.example.orbitai.core.model

import android.content.Context
import android.util.Log
import com.example.orbitai.core.common.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
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

    fun isDownloaded(model: LlmModel): Boolean {
        if (model.format != ModelFormat.ONNX_GENAI) {
            val directFile = File(modelDir, model.fileName)
            val nestedLegacyFile = File(directFile, directFile.name)
            return directFile.isFile || nestedLegacyFile.isFile
        }

        val spec = MODEL_DOWNLOAD_SPECS[model.id]
        if (spec != null) {
            return spec.files.all { file -> File(modelDir, "${model.fileName}/${file.relativePath}").exists() }
        }
        return File(modelDir, model.fileName).exists()
    }

    fun modelPath(fileName: String): String = File(modelDir, fileName).absolutePath

    fun download(model: LlmModel): Flow<DownloadProgress> {
        val spec = MODEL_DOWNLOAD_SPECS[model.id]
            ?: return flow {
                emit(
                    DownloadProgress(
                        modelId = model.id,
                        status = DownloadStatus.FAILED,
                        error = "Download spec missing for ${model.displayName}",
                    )
                )
            }.flowOn(Dispatchers.IO)

        return download(model, spec)
    }

    fun download(
        modelId: String,
        url: String,
        fileName: String,
        requiresAuth: Boolean = true,
    ): Flow<DownloadProgress> = flow {
        Log.d(TAG, "Starting download for $modelId -> $fileName")
        val dest = File(modelDir, fileName)
        val tmp  = File(modelDir, "$fileName.tmp")

        // A previous broken build could have created a directory where a model file should live.
        // Clean that up so we can redownload the real file instead of reporting a false "complete".
        if (dest.exists() && dest.isDirectory) {
            Log.w(TAG, "Removing invalid directory at file model path: ${dest.absolutePath}")
            dest.deleteRecursively()
        }

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

    private fun download(model: LlmModel, spec: ModelDownloadSpec): Flow<DownloadProgress> = flow {
        Log.d(TAG, "Starting model download for ${model.id} -> ${model.fileName}")

        if (model.format != ModelFormat.ONNX_GENAI) {
            val singleFile = spec.files.singleOrNull()
                ?: throw IllegalStateException("Expected a single-file download spec for ${model.id}")
            emitAll(
                download(
                    modelId = model.id,
                    url = singleFile.url,
                    fileName = model.fileName,
                    requiresAuth = spec.requiresAuth,
                )
            )
            return@flow
        }

        val modelRoot = File(modelDir, model.fileName).also { it.mkdirs() }
        val knownTotalBytes = spec.files.sumOf { it.sizeBytes ?: 0L }
        var completedBytes = 0L

        if (isDownloaded(model)) {
            emit(
                DownloadProgress(
                    modelId = model.id,
                    bytesDownloaded = knownTotalBytes,
                    totalBytes = knownTotalBytes,
                    status = DownloadStatus.COMPLETED,
                )
            )
            return@flow
        }

        activeDownloads[model.id] = true
        emit(
            DownloadProgress(
                modelId = model.id,
                totalBytes = knownTotalBytes,
                status = DownloadStatus.DOWNLOADING,
            )
        )

        try {
            for (file in spec.files) {
                if (!coroutineContext.isActive || activeDownloads[model.id] == false) {
                    emit(
                        DownloadProgress(
                            modelId = model.id,
                            bytesDownloaded = completedBytes,
                            totalBytes = knownTotalBytes,
                            status = DownloadStatus.FAILED,
                            error = "Cancelled",
                        )
                    )
                    return@flow
                }

                val dest = File(modelRoot, file.relativePath)
                val fallbackBytes = file.sizeBytes ?: dest.takeIf(File::exists)?.length() ?: 0L
                if (dest.exists()) {
                    completedBytes += fallbackBytes
                    emit(
                        DownloadProgress(
                            modelId = model.id,
                            bytesDownloaded = completedBytes,
                            totalBytes = knownTotalBytes,
                            status = DownloadStatus.DOWNLOADING,
                        )
                    )
                    continue
                }

                val parentDir = dest.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs()
                }

                val tmp = File(dest.absolutePath + ".tmp")
                val requestBuilder = Request.Builder()
                    .url(file.url)
                    .header("User-Agent", "Mozilla/5.0")
                if (spec.requiresAuth && tokenStore.hasToken()) {
                    requestBuilder.header("Authorization", "Bearer ${tokenStore.huggingFaceToken}")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    val errMsg = when (response.code) {
                        401  -> "Invalid token. Check your HuggingFace token in Settings."
                        403  -> "Access denied. Accept the model license on HuggingFace first."
                        404  -> "Model file not found."
                        else -> "HTTP error ${response.code}"
                    }
                    emit(
                        DownloadProgress(
                            modelId = model.id,
                            bytesDownloaded = completedBytes,
                            totalBytes = knownTotalBytes,
                            status = DownloadStatus.FAILED,
                            error = errMsg,
                        )
                    )
                    return@flow
                }

                val body = response.body ?: run {
                    emit(
                        DownloadProgress(
                            modelId = model.id,
                            bytesDownloaded = completedBytes,
                            totalBytes = knownTotalBytes,
                            status = DownloadStatus.FAILED,
                            error = "Empty response",
                        )
                    )
                    return@flow
                }

                val totalBytes = if (knownTotalBytes > 0L) knownTotalBytes else body.contentLength()
                var fileBytesDownloaded = 0L

                try {
                    tmp.outputStream().use { out ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8 * 1024)
                            var bytes = input.read(buffer)
                            while (bytes >= 0) {
                                if (!coroutineContext.isActive || activeDownloads[model.id] == false) {
                                    tmp.delete()
                                    emit(
                                        DownloadProgress(
                                            modelId = model.id,
                                            bytesDownloaded = completedBytes + fileBytesDownloaded,
                                            totalBytes = totalBytes,
                                            status = DownloadStatus.FAILED,
                                            error = "Cancelled",
                                        )
                                    )
                                    return@flow
                                }

                                out.write(buffer, 0, bytes)
                                fileBytesDownloaded += bytes
                                emit(
                                    DownloadProgress(
                                        modelId = model.id,
                                        bytesDownloaded = completedBytes + fileBytesDownloaded,
                                        totalBytes = totalBytes,
                                        status = DownloadStatus.DOWNLOADING,
                                    )
                                )
                                bytes = input.read(buffer)
                            }
                        }
                    }

                    if (!tmp.renameTo(dest)) {
                        throw IllegalStateException("Unable to finalize ${file.relativePath}")
                    }
                } catch (e: Exception) {
                    tmp.delete()
                    throw e
                }

                completedBytes += file.sizeBytes ?: fileBytesDownloaded
            }

            OnnxGenAiConfigFactory.ensureConfig(model, modelRoot)

            emit(
                DownloadProgress(
                    modelId = model.id,
                    bytesDownloaded = if (knownTotalBytes > 0L) knownTotalBytes else completedBytes,
                    totalBytes = if (knownTotalBytes > 0L) knownTotalBytes else completedBytes,
                    status = DownloadStatus.COMPLETED,
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Download exception for ${model.id}", e)
            emit(
                DownloadProgress(
                    modelId = model.id,
                    bytesDownloaded = completedBytes,
                    totalBytes = knownTotalBytes,
                    status = DownloadStatus.FAILED,
                    error = e.message,
                )
            )
        } finally {
            activeDownloads.remove(model.id)
        }
    }.flowOn(Dispatchers.IO)

    fun cancelDownload(modelId: String) {
        activeDownloads[modelId] = false
    }

    fun deleteModel(model: LlmModel) {
        val target = File(modelDir, model.fileName)
        if (target.isDirectory) target.deleteRecursively() else target.delete()
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
