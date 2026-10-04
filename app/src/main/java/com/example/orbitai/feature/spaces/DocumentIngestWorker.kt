package com.example.orbitai.feature.spaces

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.orbitai.appContainer
import com.example.orbitai.core.common.isTextLikeDocument
import com.example.orbitai.core.database.AppDatabase
import com.example.orbitai.core.database.RagChunkEntity
import com.example.orbitai.core.database.RagStatus
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Indexes one Space document: extracts text, chunks it, embeds the chunks and stores them.
 * Each run starts from a clean slate (old chunks are removed), so a run interrupted by process
 * death simply runs again. Waits while a chat or bubble reply is generating, so indexing never
 * competes with the model.
 */
class DocumentIngestWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val ragDao = AppDatabase.getInstance(context).ragDocumentDao()
    private val llmRepository = context.appContainer.llmRepository
    private val spaceRepository = context.appContainer.spaceRepository

    override suspend fun doWork(): Result {
        val docId = inputData.getString(KEY_DOC_ID) ?: return Result.failure()
        val doc = ragDao.getDocument(docId) ?: return Result.success() // Deleted meanwhile.

        ragDao.deleteChunks(docId)
        spaceRepository.invalidateIndex()
        ragDao.updateResult(docId, RagStatus.PROCESSING.name, 0, null)
        ragDao.updateProgress(docId, 0, 0)
        return try {
            val blocks = extractTextBlocks(docId, doc.uri.toUri(), doc.mimeType)
            val texts = DocumentChunker.chunk(blocks)
            if (texts.isEmpty()) throw IngestException(if (doc.mimeType == "application/pdf") IngestFailure.SCANNED_PDF else IngestFailure.EMPTY_FILE)

            val chunks = texts.mapIndexed { index, text ->
                if (index % EMBED_BATCH == 0) waitWhileModelBusy()
                RagChunkEntity(
                    id = UUID.randomUUID().toString(),
                    docId = docId,
                    chunkIndex = index,
                    content = text,
                    embedding = spaceRepository.embed(text)?.toByteArray(),
                )
            }
            if (ragDao.getDocument(docId) == null) return Result.success()
            ragDao.insertChunks(chunks)
            ragDao.updateResult(docId, RagStatus.DONE.name, chunks.size, null)
            spaceRepository.invalidateIndex()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.w(TAG, "Indexing failed for ${doc.name}", e)
            ragDao.updateResult(docId, RagStatus.ERROR.name, 0, IngestFailure.reasonFor(e))
            Result.failure()
        }
    }

    private suspend fun waitWhileModelBusy() {
        while (llmRepository.isInUse()) delay(BUSY_POLL_MS)
    }

    private suspend fun extractTextBlocks(docId: String, uri: Uri, mimeType: String): List<String> = when {
        mimeType == "application/pdf" -> extractPdfPages(docId, uri)
        isTextLikeDocument(mimeType) -> extractPlainTextBlocks(docId, uri)
        else -> throw IngestException(IngestFailure.UNSUPPORTED)
    }

    private suspend fun extractPlainTextBlocks(docId: String, uri: Uri): List<String> {
        val input = applicationContext.contentResolver.openInputStream(uri)
            ?: throw java.io.FileNotFoundException(uri.toString())
        val text = input.bufferedReader().use { it.readText() }.replace("\r\n", "\n").trim()
        ragDao.updateProgress(docId, 1, 1)
        if (text.isBlank()) return emptyList()
        return text.split(Regex("\\n\\s*\\n+")).map(DocumentChunker::normalize).filter { it.isNotBlank() }
    }

    private suspend fun extractPdfPages(docId: String, uri: Uri): List<String> {
        PDFBoxResourceLoader.init(applicationContext)
        val input = applicationContext.contentResolver.openInputStream(uri)
            ?: throw java.io.FileNotFoundException(uri.toString())
        return input.use {
            PDDocument.load(it).use { document ->
                val total = document.numberOfPages
                val stripper = PDFTextStripper().apply { sortByPosition = true }
                ragDao.updateProgress(docId, 0, total)
                (1..total).mapNotNull { pageNumber ->
                    if (pageNumber % PAGE_BATCH == 1) waitWhileModelBusy()
                    stripper.startPage = pageNumber
                    stripper.endPage = pageNumber
                    val pageText = DocumentChunker.normalize(stripper.getText(document))
                    if (pageNumber % PAGE_BATCH == 0 || pageNumber == total) ragDao.updateProgress(docId, pageNumber, total)
                    if (pageText.isBlank()) null else "[Page $pageNumber]\n$pageText"
                }
            }
        }
    }

    companion object {
        private const val TAG = "DocumentIngest"
        private const val KEY_DOC_ID = "doc_id"
        private const val PAGE_BATCH = 5
        private const val EMBED_BATCH = 8
        private const val BUSY_POLL_MS = 1_000L

        private fun workName(docId: String) = "ingest-$docId"

        fun enqueue(context: Context, docId: String, replace: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<DocumentIngestWorker>()
                .setInputData(workDataOf(KEY_DOC_ID to docId))
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(docId),
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context, docId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(docId))
        }
    }
}
