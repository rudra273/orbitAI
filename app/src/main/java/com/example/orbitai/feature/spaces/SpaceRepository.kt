package com.example.orbitai.feature.spaces

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.orbitai.core.database.AppDatabase
import com.example.orbitai.core.database.RagChunkEntity
import com.example.orbitai.core.database.RagDocument
import com.example.orbitai.core.database.RagDocumentEntity
import com.example.orbitai.core.database.RagStatus
import com.example.orbitai.core.database.Space
import com.example.orbitai.core.database.SpaceEntity
import com.example.orbitai.core.database.toDomain
import com.example.orbitai.core.common.normalizeDocumentMimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    /** Spaces read straight from the database (for screens that open before [spaces] has loaded). */
    suspend fun currentSpaces(): List<Space> = spaceDao.observeSpaces().first().map { it.toDomain() }

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

    suspend fun renameSpace(id: String, name: String) {
        spaceDao.renameSpace(id, name.trim())
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
            DocumentIngestWorker.enqueue(context, docId)
        }
    }

    /** Re-queues documents left unfinished, e.g. when the process died mid-way. Safe to call on every start. */
    fun resumeUnfinishedIngestion() {
        scope.launch {
            ragDao.getUnfinishedDocuments().forEach { DocumentIngestWorker.enqueue(context, it.id) }
        }
    }

    /** Indexes a failed document again. */
    fun retryDocument(id: String) {
        scope.launch {
            ragDao.updateResult(id, RagStatus.PENDING.name, 0, null)
            DocumentIngestWorker.enqueue(context, id, replace = true)
        }
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        DocumentIngestWorker.cancel(context, id)
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
