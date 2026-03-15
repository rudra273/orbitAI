package com.example.orbitai.data.rag

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.orbitai.data.db.AppDatabase
import com.example.orbitai.data.db.RagChunkEntity
import com.example.orbitai.data.db.RagDocument
import com.example.orbitai.data.db.RagDocumentEntity
import com.example.orbitai.data.db.RagStatus
import com.example.orbitai.data.db.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class RagRepository(private val context: Context) {

    private val db    = AppDatabase.getInstance(context)
    private val dao   = db.ragDocumentDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val documents: StateFlow<List<RagDocument>> = dao.observeDocuments()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun addDocument(uri: Uri) {
        scope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}

            val cr = context.contentResolver
            val (name, size) = cr.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                val n = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "document" else "document"
                val s = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                Pair(n, s)
            } ?: Pair("document", 0L)

            val mimeType = cr.getType(uri) ?: "application/octet-stream"
            val docId    = UUID.randomUUID().toString()

            dao.insertDocument(
                RagDocumentEntity(
                    id         = docId,
                    name       = name,
                    uri        = uri.toString(),
                    mimeType   = mimeType,
                    sizeBytes  = size,
                    status     = RagStatus.PENDING.name,
                    chunkCount = 0,
                    addedAt    = System.currentTimeMillis(),
                )
            )
            processDocument(docId, uri, mimeType)
        }
    }

    private suspend fun processDocument(docId: String, uri: Uri, mimeType: String) {
        dao.updateStatus(docId, RagStatus.PROCESSING.name, 0)
        try {
            val text   = extractText(uri, mimeType)
            val chunks = chunkText(text, docId)

            // Embed each chunk if the embedding model is available
            val embedder = EmbeddingModel.create(context)
            val chunksWithEmbeddings = if (embedder != null) {
                chunks.map { chunk ->
                    val vec = embedder.embed(chunk.content)
                    if (vec != null) chunk.copy(embedding = vec.toByteArray()) else chunk
                }.also { embedder.close() }
            } else {
                chunks
            }

            dao.insertChunks(chunksWithEmbeddings)
            dao.updateStatus(docId, RagStatus.DONE.name, chunksWithEmbeddings.size)
        } catch (_: Exception) {
            dao.updateStatus(docId, RagStatus.ERROR.name, 0)
        }
    }

    private fun extractText(uri: Uri, mimeType: String): String {
        return when {
            mimeType == "application/pdf" -> extractPdfText(uri)
            mimeType.startsWith("text/")  ->
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: ""
            else -> ""
        }
    }

    private fun extractPdfText(uri: Uri): String {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return "[PDF — could not open]"
            pfd.use {
                val renderer = android.graphics.pdf.PdfRenderer(it)
                val pageCount = renderer.pageCount
                renderer.close()
                "[PDF — $pageCount pages — full text extraction coming soon]"
            }
        } catch (e: Exception) {
            "[PDF — error: ${e.message}]"
        }
    }

    private fun chunkText(
        text: String,
        docId: String,
        chunkWordSize: Int = 500,
    ): List<RagChunkEntity> {
        if (text.isBlank()) return emptyList()
        val words      = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val stride     = (chunkWordSize * 3) / 4
        val chunks     = mutableListOf<RagChunkEntity>()
        var idx        = 0
        var chunkIndex = 0
        while (idx < words.size) {
            val end   = minOf(idx + chunkWordSize, words.size)
            val chunk = words.subList(idx, end).joinToString(" ")
            chunks += RagChunkEntity(
                id         = UUID.randomUUID().toString(),
                docId      = docId,
                chunkIndex = chunkIndex++,
                content    = chunk,
            )
            idx += stride
        }
        return chunks
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        dao.deleteDocument(id)
    }

    /**
     * Semantic search: embed query → cosine similarity against stored chunk embeddings.
     * Falls back to keyword search if no embedding model / no embeddings stored yet.
     */
    suspend fun searchChunks(query: String, limit: Int = 5): List<RagChunkEntity> = withContext(Dispatchers.IO) {
        // Try semantic search first
        val embedder = EmbeddingModel.create(context)
        if (embedder != null) {
            val queryVec = embedder.embed(query)
            embedder.close()
            if (queryVec != null) {
                val allChunks = dao.getAllChunksWithEmbeddings()
                if (allChunks.isNotEmpty()) {
                    return@withContext allChunks
                        .mapNotNull { chunk ->
                            val chunkVec = chunk.embedding?.toFloatArray() ?: return@mapNotNull null
                            Pair(chunk, cosineSimilarity(queryVec, chunkVec))
                        }
                        .sortedByDescending { it.second }
                        .take(limit)
                        .map { it.first }
                }
            }
        }

        // Fallback: keyword search (used when model not downloaded yet)
        keywordSearch(query, limit)
    }

    private val stopWords = setOf(
        "a","an","the","is","are","was","were","be","been","being",
        "have","has","had","do","does","did","will","would","shall","should",
        "may","might","must","can","could","i","me","my","mine","we","our",
        "you","your","he","him","his","she","her","it","its","they","them",
        "their","this","that","these","those","what","which","who","whom",
        "how","when","where","why","of","in","to","for","with","on","at",
        "from","by","about","as","into","through","during","before","after",
        "and","but","or","nor","not","so","if","then","than","too","very",
        "just","also","tell","give","show","get","find","know","let","make",
    )

    private suspend fun keywordSearch(query: String, limit: Int): List<RagChunkEntity> {
        val keywords = query.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 1 && it !in stopWords }
        if (keywords.isEmpty()) return emptyList()
        val hitMap = mutableMapOf<String, Pair<RagChunkEntity, Int>>()
        for (kw in keywords) {
            for (chunk in dao.searchChunks(kw, limit = 20) + dao.searchChunksByDocName(kw, limit = 20)) {
                hitMap[chunk.id] = Pair(chunk, (hitMap[chunk.id]?.second ?: 0) + 1)
            }
        }
        return hitMap.values.sortedByDescending { it.second }.take(limit).map { it.first }
    }
}
