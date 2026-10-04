package com.example.orbitai.feature.spaces

import com.example.orbitai.feature.spaces.embedding.EmbedKind
import com.example.orbitai.feature.spaces.embedding.Embedded

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.orbitai.core.database.AppDatabase
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        invalidateIndex()
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
            if (ragDao.countChunksToEmbed(activeEmbeddingModelId) > 0) ReembedWorker.enqueue(context)
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
        invalidateIndex()
    }

    private val embedder = SharedEmbedder(context)
    private val indexLock = Mutex()
    @Volatile private var cachedIndex: Pair<Set<String>, RetrievalIndex>? = null

    /** Id of the embedding model in use; vectors from other models are ignored until re-indexed. */
    val activeEmbeddingModelId: String get() = embedder.activeModelId

    /** Embeds stored text (a document chunk or a memory); null when the model isn't downloaded. */
    fun embedPassage(text: String): Embedded? = embedder.embed(text, EmbedKind.PASSAGE)

    /** Embeds a question; null when the model isn't downloaded. */
    fun embedQuery(text: String): FloatArray? = embedder.embed(text, EmbedKind.QUERY)?.vector

    /** Drops the retrieval index so the next search rebuilds it (after documents change). */
    fun invalidateIndex() {
        cachedIndex = null
    }

    /** Frees the embedding model and the retrieval index; both are rebuilt on next use. */
    fun releaseMemory() {
        cachedIndex = null
        embedder.release()
    }

    /**
     * Finds the chunks in [spaceIds] most relevant to [query], combining keyword (BM25) and
     * semantic ranking. Returns nothing when no chunk is relevant (e.g. small talk).
     */
    suspend fun searchChunksInSpaces(
        query: String,
        spaceIds: List<String>,
        limit: Int = 5,
    ): List<IndexChunk> = withContext(Dispatchers.IO) {
        if (spaceIds.isEmpty() || query.isBlank()) return@withContext emptyList()
        val index = indexFor(spaceIds.toSet())
        if (index.size == 0) return@withContext emptyList()
        index.search(query, embedQuery(query), limit)
    }

    private suspend fun indexFor(spaceIds: Set<String>): RetrievalIndex = indexLock.withLock {
        cachedIndex?.takeIf { it.first == spaceIds }?.second
            ?: RetrievalIndex(
                spaceDao.getChunksForSpaces(spaceIds.toList()).map {
                    // Vectors from another embedding model can't be compared; those chunks are found by keywords until re-indexed.
                    val usable = it.embeddingModel == activeEmbeddingModelId
                    IndexChunk(it.id, it.docId, it.docName, it.chunkIndex, it.content, if (usable) it.embedding?.toFloatArray() else null)
                },
            ).also { cachedIndex = spaceIds to it }
    }
}
