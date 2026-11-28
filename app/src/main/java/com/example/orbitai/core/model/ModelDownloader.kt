package com.example.orbitai.core.model

import android.content.Context
import android.util.Log
import com.example.orbitai.core.common.TokenStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
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
    /** True when a failed download kept its partial data and can be retried from where it stopped. */
    val canResume: Boolean = false,
) {
    val progress: Float get() = if (totalBytes > 0) bytesDownloaded / totalBytes.toFloat() else 0f
    val progressPercent: Int get() = (progress * 100).toInt()
}

enum class DownloadStatus { IDLE, DOWNLOADING, PAUSED, VERIFYING, COMPLETED, FAILED }

class ModelDownloader(private val context: Context) {
    companion object {
        private const val TAG = "ModelDownloader"
    }

    private val tokenStore = TokenStore(context)

    private val client = OkHttpClient.Builder()
        .addInterceptor(com.example.orbitai.feature.privacy.NetworkLogInterceptor("Model download"))
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val activeDownloads = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    private val fileDownloader = ResumableFileDownloader(client) {
        tokenStore.huggingFaceToken.takeIf { tokenStore.hasToken() }
    }

    private suspend fun downloadFile(
        modelId: String,
        url: String,
        dest: File,
        expectedSize: Long?,
        expectedSha256: String?,
        requiresAuth: Boolean,
        onProgress: suspend (bytesDone: Long, totalBytes: Long, status: DownloadStatus) -> Unit,
    ): FileDownloadResult = fileDownloader.download(
        url = url,
        dest = dest,
        expectedSize = expectedSize,
        expectedSha256 = expectedSha256,
        requiresAuth = requiresAuth,
        isCancelled = { activeDownloads[modelId] == false },
        onProgress = onProgress,
    )

    val modelDir: File = File(context.getExternalFilesDir(null), "models").also { it.mkdirs() }

    fun isDownloaded(model: LlmModel): Boolean {
        if (model.format != ModelFormat.ONNX_GENAI) {
            val directFile = File(modelDir, model.fileName)
            val nestedLegacyFile = File(directFile, directFile.name)
            return (directFile.isFile && directFile.length() > 0) ||
                (nestedLegacyFile.isFile && nestedLegacyFile.length() > 0)
        }

        val spec = MODEL_DOWNLOAD_SPECS[model.id]
        if (spec != null) {
            return spec.files.all { file ->
                val downloadedFile = File(modelDir, "${model.fileName}/${file.relativePath}")
                downloadedFile.isFile && downloadedFile.length() > 0
            }
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
        expectedSizeBytes: Long? = null,
        expectedSha256: String? = null,
    ): Flow<DownloadProgress> = flow {
        Log.d(TAG, "Starting download for $modelId -> $fileName")
        val dest = File(modelDir, fileName)

        // A previous broken build could have created a directory where a model file should live.
        // Clean that up so we can redownload the real file instead of reporting a false "complete".
        if (dest.exists() && dest.isDirectory) {
            Log.w(TAG, "Removing invalid directory at file model path: ${dest.absolutePath}")
            dest.deleteRecursively()
        }

        if (dest.isFile && dest.length() > 0) {
            emit(DownloadProgress(modelId, dest.length(), dest.length(), DownloadStatus.COMPLETED))
            return@flow
        }

        activeDownloads[modelId] = true
        emit(DownloadProgress(modelId, status = DownloadStatus.DOWNLOADING))
        try {
            val result = downloadFile(
                modelId = modelId,
                url = url,
                dest = dest,
                expectedSize = expectedSizeBytes,
                expectedSha256 = expectedSha256,
                requiresAuth = requiresAuth,
            ) { done, total, status -> emit(DownloadProgress(modelId, done, total, status)) }
            when (result) {
                FileDownloadResult.Completed -> {
                    Log.d(TAG, "Download completed for $modelId -> ${dest.absolutePath}")
                    emit(DownloadProgress(modelId, dest.length(), dest.length(), DownloadStatus.COMPLETED))
                }
                is FileDownloadResult.Failed -> emit(
                    DownloadProgress(modelId, status = DownloadStatus.FAILED, error = result.message, canResume = result.canResume)
                )
            }
        } finally {
            activeDownloads.remove(modelId)
        }
    }.flowOn(Dispatchers.IO)

    private fun download(model: LlmModel, spec: ModelDownloadSpec): Flow<DownloadProgress> = flow {
        Log.d(TAG, "Starting model download for ${model.id} -> ${model.fileName}")
        val checksums = MODEL_FILE_SHA256[model.id].orEmpty()

        if (model.format != ModelFormat.ONNX_GENAI) {
            val singleFile = spec.files.singleOrNull()
                ?: throw IllegalStateException("Expected a single-file download spec for ${model.id}")
            emitAll(
                download(
                    modelId = model.id,
                    url = singleFile.url,
                    fileName = model.fileName,
                    requiresAuth = spec.requiresAuth,
                    expectedSizeBytes = singleFile.sizeBytes,
                    expectedSha256 = checksums[singleFile.relativePath],
                ).onEach { if (it.status == DownloadStatus.COMPLETED) deleteReplacedFiles(model) }
            )
            return@flow
        }

        val modelRoot = File(modelDir, model.fileName).also { it.mkdirs() }
        val knownTotalBytes = spec.files.sumOf { it.sizeBytes ?: 0L }

        if (isDownloaded(model)) {
            emit(DownloadProgress(model.id, knownTotalBytes, knownTotalBytes, DownloadStatus.COMPLETED))
            return@flow
        }

        activeDownloads[model.id] = true
        emit(DownloadProgress(model.id, totalBytes = knownTotalBytes, status = DownloadStatus.DOWNLOADING))

        var completedBytes = 0L
        try {
            for (file in spec.files) {
                val dest = File(modelRoot, file.relativePath)
                val alreadyComplete = dest.isFile && (file.sizeBytes == null || dest.length() == file.sizeBytes)
                if (!alreadyComplete) {
                    val result = downloadFile(
                        modelId = model.id,
                        url = file.url,
                        dest = dest,
                        expectedSize = file.sizeBytes,
                        expectedSha256 = checksums[file.relativePath],
                        requiresAuth = spec.requiresAuth,
                    ) { done, _, status ->
                        emit(DownloadProgress(model.id, completedBytes + done, knownTotalBytes, status))
                    }
                    if (result is FileDownloadResult.Failed) {
                        emit(DownloadProgress(model.id, completedBytes, knownTotalBytes, DownloadStatus.FAILED, result.message, result.canResume))
                        return@flow
                    }
                }
                completedBytes += file.sizeBytes ?: dest.length()
                emit(DownloadProgress(model.id, completedBytes, knownTotalBytes, DownloadStatus.DOWNLOADING))
            }

            OnnxGenAiConfigFactory.ensureConfig(model, modelRoot)

            val total = if (knownTotalBytes > 0L) knownTotalBytes else completedBytes
            emit(DownloadProgress(model.id, total, total, DownloadStatus.COMPLETED))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Download exception for ${model.id}", e)
            emit(DownloadProgress(model.id, completedBytes, knownTotalBytes, DownloadStatus.FAILED, e.message))
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
        deletePartialDownload(model)
        deleteReplacedFiles(model)
        deleteEngineCaches(model.fileName)
        REPLACED_MODEL_FILES[model.id].orEmpty().forEach(::deleteEngineCaches)
    }

    /** Removes the caches LiteRT-LM writes to the cache dir for a model file (e.g. "<file>.xnnpack_cache"). */
    private fun deleteEngineCaches(modelFileName: String) {
        context.cacheDir.listFiles()
            ?.filter { it.name.startsWith(modelFileName) }
            ?.forEach { it.deleteRecursively() }
    }

    /** Removes files an older catalog version downloaded for [model], once they are no longer needed. */
    private fun deleteReplacedFiles(model: LlmModel) {
        REPLACED_MODEL_FILES[model.id].orEmpty().forEach { name ->
            File(modelDir, name).delete()
            File(modelDir, "$name.tmp").delete()
        }
    }

    /** Bytes already on disk from an interrupted download, used to offer "Resume". */
    fun partialBytes(model: LlmModel): Long {
        val root = File(modelDir, model.fileName)
        if (model.format != ModelFormat.ONNX_GENAI) return File(modelDir, "${model.fileName}.tmp").takeIf { it.isFile }?.length() ?: 0L
        if (!root.isDirectory) return 0L
        return root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun deletePartialDownload(model: LlmModel) {
        File(modelDir, "${model.fileName}.tmp").delete()
        File(modelDir, model.fileName).takeIf { it.isDirectory }?.walkTopDown()
            ?.filter { it.isFile && it.name.endsWith(".tmp") }
            ?.forEach { it.delete() }
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
