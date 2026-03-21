package com.example.orbitai.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.orbitai.data.db.AppDatabase
import com.example.orbitai.data.db.RagChunkEntity
import com.example.orbitai.data.db.RagDocument
import com.example.orbitai.data.db.RagDocumentEntity
import com.example.orbitai.data.db.RagStatus
import com.example.orbitai.data.db.Space
import com.example.orbitai.data.db.SpaceEntity
import com.example.orbitai.data.db.toDomain
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.example.orbitai.data.rag.EmbeddingModel
import com.example.orbitai.data.rag.cosineSimilarity
import com.example.orbitai.data.rag.toByteArray
import com.example.orbitai.data.rag.toFloatArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SpaceRepository(private val context: Context) {

    private val db       = AppDatabase.getInstance(context)
    private val spaceDao = db.spaceDao()
    private val ragDao   = db.ragDocumentDao()
    private val scope    = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val spaces: StateFlow<List<Space>> = spaceDao.observeSpaces()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun observeDocumentsInSpace(spaceId: String): Flow<List<RagDocument>> =
        spaceDao.observeDocumentsInSpace(spaceId)
            .map { list -> list.map { it.toDomain() } }

    suspend fun createSpace(name: String): Space {
        val entity = SpaceEntity(
            id        = UUID.randomUUID().toString(),
            name      = name.trim(),
            createdAt = System.currentTimeMillis(),
        )
        spaceDao.insertSpace(entity)
        return entity.toDomain()
    }

    suspend fun deleteSpace(id: String) {
        // Delete all documents in the space first (chunks cascade via FK on rag_chunks.docId)
        spaceDao.deleteDocumentsBySpaceId(id)
        spaceDao.deleteSpace(id)
    }

    fun addDocumentToSpace(uri: Uri, spaceId: String) {
        scope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}

            val cr = context.contentResolver
            val (name, size) = cr.query(uri, null, null, null, null)?.use { c ->
                val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = c.getColumnIndex(OpenableColumns.SIZE)
                c.moveToFirst()
                val n = if (ni >= 0) c.getString(ni) ?: "document" else "document"
                val s = if (si >= 0) c.getLong(si) else 0L
                Pair(n, s)
            } ?: Pair("document", 0L)

            val mimeType = normalizeDocumentMimeType(cr.getType(uri), name)
            val docId    = UUID.randomUUID().toString()

            ragDao.insertDocument(
                RagDocumentEntity(
                    id         = docId,
                    name       = name,
                    uri        = uri.toString(),
                    mimeType   = mimeType,
                    sizeBytes  = size,
                    status     = RagStatus.PENDING.name,
                    chunkCount = 0,
                    addedAt    = System.currentTimeMillis(),
                    spaceId    = spaceId,
                )
            )
            processDocument(docId, uri, mimeType)
        }
    }

    private suspend fun processDocument(docId: String, uri: Uri, mimeType: String) {
        ragDao.updateStatus(docId, RagStatus.PROCESSING.name, 0)
        try {
            val textBlocks = extractTextBlocks(uri, mimeType)
            val chunks     = chunkTextBlocks(textBlocks, docId)

            val embedder = EmbeddingModel.create(context)
            val chunksWithEmbeddings = if (embedder != null) {
                chunks.map { chunk ->
                    val vec = embedder.embed(chunk.content)
                    if (vec != null) chunk.copy(embedding = vec.toByteArray()) else chunk
                }.also { embedder.close() }
            } else {
                chunks
            }

            ragDao.insertChunks(chunksWithEmbeddings)
            ragDao.updateStatus(docId, RagStatus.DONE.name, chunksWithEmbeddings.size)
        } catch (_: Exception) {
            ragDao.updateStatus(docId, RagStatus.ERROR.name, 0)
        }
    }

    private fun extractTextBlocks(uri: Uri, mimeType: String): List<String> = when {
        mimeType == "application/pdf" -> extractPdfPages(uri)
        isTextLikeDocument(mimeType)  -> extractPlainTextBlocks(uri)
        else -> emptyList()
    }

    private fun extractPlainTextBlocks(uri: Uri): List<String> {
        val text = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
            .replace("\r\n", "\n")
            .trim()

        if (text.isBlank()) return emptyList()

        return text
            .split(Regex("\\n\\s*\\n+"))
            .map { normalizeChunkText(it) }
            .filter { it.isNotBlank() }
    }

    private fun extractPdfPages(uri: Uri): List<String> = try {
        PDFBoxResourceLoader.init(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                val stripper = PDFTextStripper().apply { sortByPosition = true }
                (1..document.numberOfPages).mapNotNull { pageNumber ->
                    stripper.startPage = pageNumber
                    stripper.endPage = pageNumber
                    val pageText = normalizeChunkText(stripper.getText(document))
                    if (pageText.isBlank()) null else "[Page $pageNumber]\n$pageText"
                }
            }
        } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun chunkTextBlocks(
        textBlocks: List<String>,
        docId: String,
        chunkWordSize: Int = 500,
        overlapWordCount: Int = 125,
    ): List<RagChunkEntity> {
        if (textBlocks.isEmpty()) return emptyList()

        val chunks = mutableListOf<RagChunkEntity>()
        var chunkIndex = 0
        var currentWords = mutableListOf<String>()

        fun flushChunk() {
            if (currentWords.isEmpty()) return
            chunks += RagChunkEntity(
                id         = UUID.randomUUID().toString(),
                docId      = docId,
                chunkIndex = chunkIndex++,
                content    = currentWords.joinToString(" "),
            )
            currentWords = currentWords.takeLast(overlapWordCount).toMutableList()
        }

        textBlocks.forEach { block ->
            val blockWords = block.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (blockWords.isEmpty()) return@forEach

            if (currentWords.isNotEmpty() && currentWords.size + blockWords.size > chunkWordSize) {
                flushChunk()
            }

            if (blockWords.size >= chunkWordSize) {
                var start = 0
                while (start < blockWords.size) {
                    val preservedOverlap = if (currentWords.isEmpty()) emptyList() else currentWords
                    val availableWords = (chunkWordSize - preservedOverlap.size).coerceAtLeast(1)
                    val end = minOf(start + availableWords, blockWords.size)
                    currentWords = (preservedOverlap + blockWords.subList(start, end)).toMutableList()
                    flushChunk()
                    start = end
                }
            } else {
                currentWords.addAll(blockWords)
            }
        }

        if (currentWords.isNotEmpty()) {
            chunks += RagChunkEntity(
                id         = UUID.randomUUID().toString(),
                docId      = docId,
                chunkIndex = chunkIndex,
                content    = currentWords.joinToString(" "),
            )
        }

        return chunks
    }

    private fun normalizeChunkText(text: String): String =
        text
            .replace(Regex("[\\t\\x0B\\f\\r ]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        ragDao.deleteDocument(id)
    }

    /**
     * Search chunks across all specified spaces.
     * Uses semantic (cosine) search if the embedding model is available,
     * otherwise falls back to keyword search.
     */
    suspend fun searchChunksInSpaces(
        query: String,
        spaceIds: List<String>,
        limit: Int = 5,
    ): List<RagChunkEntity> = withContext(Dispatchers.IO) {
        if (spaceIds.isEmpty()) return@withContext emptyList()

        val embedder = EmbeddingModel.create(context)
        if (embedder != null) {
            val queryVec = embedder.embed(query)
            embedder.close()
            if (queryVec != null) {
                val allChunks = spaceDao.getAllChunksWithEmbeddingsForSpaces(spaceIds)
                if (allChunks.isNotEmpty()) {
                    return@withContext allChunks
                        .mapNotNull { chunk ->
                            val cv = chunk.embedding?.toFloatArray() ?: return@mapNotNull null
                            Pair(chunk, cosineSimilarity(queryVec, cv))
                        }
                        .sortedByDescending { it.second }
                        .take(limit)
                        .map { it.first }
                }
            }
        }

        keywordSearchInSpaces(query, spaceIds, limit)
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

    private suspend fun keywordSearchInSpaces(
        query: String,
        spaceIds: List<String>,
        limit: Int,
    ): List<RagChunkEntity> {
        val keywords = query.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 1 && it !in stopWords }
        if (keywords.isEmpty()) return emptyList()

        val hitMap = mutableMapOf<String, Pair<RagChunkEntity, Int>>()
        for (kw in keywords) {
            val hits = spaceDao.searchChunksInSpaces(spaceIds, kw, 20) +
                       spaceDao.searchChunksByDocNameInSpaces(spaceIds, kw, 20)
            for (chunk in hits) {
                hitMap[chunk.id] = Pair(chunk, (hitMap[chunk.id]?.second ?: 0) + 1)
            }
        }
        return hitMap.values.sortedByDescending { it.second }.take(limit).map { it.first }
    }
}
