package com.example.orbitai.feature.chat.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.orbitai.feature.chat.Chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

/** Writes chat exports off the main thread, to a share-able cache file or a user-picked document. */
class ChatExporter(private val context: Context) {

    suspend fun write(chat: Chat, modelName: String?, format: ExportFormat, includeImages: Boolean, out: OutputStream) =
        withContext(Dispatchers.IO) {
            when (format) {
                ExportFormat.MARKDOWN -> out.write(ChatExport.markdown(chat, modelName).toByteArray())
                ExportFormat.TEXT -> out.write(ChatExport.plainText(chat, modelName).toByteArray())
                ExportFormat.PDF -> ChatPdfWriter(context).write(chat, modelName, includeImages, out)
            }
        }

    suspend fun saveTo(uri: Uri, chat: Chat, modelName: String?, format: ExportFormat, includeImages: Boolean) =
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(uri) ?: error("Couldn't open the chosen file.")
            stream.use { write(chat, modelName, format, includeImages, it) }
        }

    /** Writes the export to the app cache and returns a share-sheet intent for it. */
    suspend fun shareIntent(chat: Chat, modelName: String?, format: ExportFormat, includeImages: Boolean): Intent =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, ChatExport.fileName(chat, format))
            file.outputStream().use { write(chat, modelName, format, includeImages, it) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, ChatExport.displayTitle(chat))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(send, "Share chat").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
}
