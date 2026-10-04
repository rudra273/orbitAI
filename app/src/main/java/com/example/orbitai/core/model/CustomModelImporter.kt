package com.example.orbitai.core.model

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import com.example.orbitai.appContainer
import com.example.orbitai.core.common.InferenceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext

/** What the user picked and entered for a model file before importing it. */
data class CustomModelRequest(
    val uri: Uri,
    val displayName: String,
    val promptStyle: PromptStyle,
    val supportsVision: Boolean,
    val contextTokens: Int,
)

/**
 * Copies a `.litertlm` / `.task` file into `models/custom/`, then loads it once so a broken file
 * is reported right away (with the engine's own error) and removed.
 */
class CustomModelImporter(private val context: Context) {

    private val modelsDir = File(context.getExternalFilesDir(null), "models/${CustomModelSpec.CUSTOM_DIR}")

    /** Display name and size of the picked file. */
    fun describe(uri: Uri): Pair<String, Long> =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (!c.moveToFirst()) null
            else (c.getString(0) ?: "model") to (if (c.isNull(1)) -1L else c.getLong(1))
        } ?: ("model" to -1L)

    suspend fun import(request: CustomModelRequest, onProgress: (copied: Long, total: Long) -> Unit): Result<CustomModelSpec> =
        withContext(Dispatchers.IO) {
            val (fileName, size) = describe(request.uri)
            val format = CustomModelSpec.formatOf(fileName)
                ?: return@withContext Result.failure(IllegalArgumentException("Pick a .litertlm or .task model file."))
            modelsDir.mkdirs()
            val free = StatFs(modelsDir.path).availableBytes
            if (size > 0 && size + STORAGE_MARGIN > free) {
                return@withContext Result.failure(IllegalStateException(
                    "Not enough storage: the model needs ${formatModelSize(size)} and ${formatModelSize(free)} is free.",
                ))
            }
            val id = CustomModelSpec.ID_PREFIX + UUID.randomUUID().toString().take(8)
            val target = File(modelsDir, "$id-" + fileName.replace(Regex("""[^\w.\-]+"""), "_"))
            val partial = File(target.path + ".tmp")
            try {
                val input = context.contentResolver.openInputStream(request.uri) ?: error("Couldn't open the file.")
                input.use { source ->
                    partial.outputStream().use { out ->
                        val buffer = ByteArray(1 shl 16)
                        var copied = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = source.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            copied += read
                            onProgress(copied, size)
                        }
                    }
                }
                check(partial.renameTo(target)) { "Couldn't save the model file." }
                val spec = CustomModelSpec(
                    id = id,
                    displayName = request.displayName.trim().ifBlank { fileName.substringBeforeLast('.') },
                    file = target.name,
                    format = format,
                    promptStyle = request.promptStyle,
                    supportsVision = request.supportsVision,
                    contextTokens = request.contextTokens,
                )
                // Load it once through the shared repository, so it never sits in memory twice.
                val repo = context.appContainer.llmRepository
                repo.beginUse()
                try {
                    repo.loadModel(spec.toModel(), InferenceSettings())
                } finally {
                    repo.endUse()
                }
                CustomModels.add(spec)
                Result.success(spec)
            } catch (e: Throwable) {
                partial.delete()
                target.delete()
                if (e is kotlinx.coroutines.CancellationException) throw e
                Result.failure(IllegalStateException("This model couldn't be loaded: ${e.message ?: e.javaClass.simpleName}", e))
            }
        }

    /** Removes an imported model's file and registry entry. */
    fun delete(spec: CustomModelSpec) {
        val repo = context.appContainer.llmRepository
        if (repo.isModelLoaded(spec.id, InferenceSettings())) repo.releaseIfIdle()
        File(modelsDir, spec.file).delete()
        CustomModels.remove(spec.id)
    }

    private companion object {
        const val STORAGE_MARGIN = 200L * 1024 * 1024
    }
}
