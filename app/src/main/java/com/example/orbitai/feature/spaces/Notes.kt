package com.example.orbitai.feature.spaces

import android.content.Context
import androidx.core.net.toUri
import java.io.File

/** Notes typed in the app are plain text files in app storage, indexed like any other Space document. */
object Notes {

    private const val DIR = "notes"
    const val NAME_PREFIX = "Note: "

    private fun dir(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }

    fun write(context: Context, docId: String, text: String): File =
        File(dir(context), "$docId.txt").also { it.writeText(text) }

    /** The note file behind [uri], or null for documents that came from elsewhere. */
    fun fileFor(context: Context, uri: String): File? {
        val path = runCatching { uri.toUri().takeIf { it.scheme == "file" }?.path }.getOrNull() ?: return null
        val file = File(path)
        return file.takeIf { it.parentFile?.canonicalPath == dir(context).canonicalPath }
    }

    fun displayName(title: String): String = NAME_PREFIX + title.trim().replace(Regex("\\s+"), " ").take(60).ifBlank { "Untitled" }
}
