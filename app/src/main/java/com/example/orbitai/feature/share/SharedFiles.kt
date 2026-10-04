package com.example.orbitai.feature.share

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.orbitai.core.common.isImageDocument
import com.example.orbitai.core.common.isTextLikeDocument
import com.example.orbitai.core.common.normalizeDocumentMimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** A file shared to Orbit, copied into app storage and served through Orbit's FileProvider. */
data class SharedFile(val uri: Uri, val name: String, val mimeType: String) {
    val isImage: Boolean get() = isImageDocument(mimeType)
    /** PDFs and text files, which Spaces can index. */
    val isDocument: Boolean get() = mimeType == "application/pdf" || isTextLikeDocument(mimeType)
}

data class SharedImport(val files: List<SharedFile>, val skipped: List<String>)

object SharedFileNames {
    const val MAX_BYTES = 50L * 1024 * 1024

    /** A safe file name that keeps the extension, adding [extension] when the name has none. */
    fun safeName(name: String, extension: String?): String {
        val cleaned = name.substringAfterLast('/').replace(Regex("""[^\p{L}\p{M}\p{N}._ -]+"""), "_").trim(' ', '.').take(80)
        val base = cleaned.ifBlank { "shared" }
        return if ('.' !in base && !extension.isNullOrBlank()) "$base.$extension" else base
    }

    fun isSupported(mimeType: String): Boolean =
        isImageDocument(mimeType) || mimeType == "application/pdf" || isTextLikeDocument(mimeType)
}

/**
 * Share-sheet links are readable only while the receiving screen is open, so each file is
 * copied to `files/shared/` right away and used from there.
 */
class SharedFileImporter(private val context: Context) {

    suspend fun import(uris: List<Uri>): SharedImport = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "shared").apply { mkdirs() }
        val files = mutableListOf<SharedFile>()
        val skipped = mutableListOf<String>()
        uris.distinct().forEach { uri ->
            val (rawName, size) = queryNameAndSize(uri)
            val mimeType = normalizeDocumentMimeType(context.contentResolver.getType(uri), rawName)
            if (!SharedFileNames.isSupported(mimeType) || size > SharedFileNames.MAX_BYTES) {
                skipped += rawName
                return@forEach
            }
            val name = SharedFileNames.safeName(rawName, MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType))
            val target = File(File(dir, UUID.randomUUID().toString()).apply { mkdirs() }, name)
            val copied = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } != null
            }.getOrDefault(false)
            if (copied && target.length() <= SharedFileNames.MAX_BYTES) {
                files += SharedFile(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target), name, mimeType)
            } else {
                target.parentFile?.deleteRecursively()
                skipped += rawName
            }
        }
        SharedImport(files, skipped)
    }

    private fun queryNameAndSize(uri: Uri): Pair<String, Long> =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (!c.moveToFirst()) return@use null
                val name = c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let(c::getString)
                val size = c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }?.let(c::getLong)
                (name ?: uri.lastPathSegment ?: "shared") to (size ?: 0L)
            }
        }.getOrNull() ?: ((uri.lastPathSegment ?: "shared") to 0L)
}
