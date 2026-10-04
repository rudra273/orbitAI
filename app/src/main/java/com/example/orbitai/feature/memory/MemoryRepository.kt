package com.example.orbitai.feature.memory

import android.content.Context
import com.example.orbitai.core.database.AppDatabase
import com.example.orbitai.core.database.MemoryEntity
import com.example.orbitai.appContainer
import com.example.orbitai.feature.spaces.toByteArray
import com.example.orbitai.feature.spaces.toFloatArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class MemoryRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).memoryDao()
    private val memoryFeatureStore = MemoryFeatureStore(context)
    /** Shared embedding model (same one Spaces use). */
    private val spaces get() = context.appContainer.spaceRepository

    val memories: Flow<List<MemoryEntity>> = dao.observeMemories()

    suspend fun addMemory(content: String, source: String = "auto") = withContext(Dispatchers.IO) {
        if (!memoryFeatureStore.isEnabled) return@withContext false
        val trimmed = content.trim()
        if (trimmed.isBlank()) return@withContext false

        val embedded = spaces.embedPassage(trimmed)
        dao.insertMemory(
            MemoryEntity(
                id        = UUID.randomUUID().toString(),
                content   = trimmed,
                source    = source,
                createdAt = System.currentTimeMillis(),
                embedding = embedded?.vector?.toByteArray(),
                embeddingModel = embedded?.modelId ?: spaces.activeEmbeddingModelId,
            )
        )
        true
    }

    /** Changes a memory's text and re-embeds it. */
    suspend fun updateMemory(id: String, content: String) = withContext(Dispatchers.IO) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return@withContext
        val embedded = spaces.embedPassage(trimmed)
        dao.updateContent(id, trimmed, embedded?.vector?.toByteArray(), embedded?.modelId ?: spaces.activeEmbeddingModelId)
    }

    /**
     * Memories to send with [query]: profile facts plus the most relevant few (see [MemorySelector]).
     * Memories saved before embeddings existed are embedded here once.
     */
    suspend fun selectForPrompt(query: String): List<MemoryEntity> = withContext(Dispatchers.IO) {
        if (!memoryFeatureStore.isEnabled) return@withContext emptyList()
        val activeModel = spaces.activeEmbeddingModelId
        val all = dao.getAllMemories().map { memory ->
            if (memory.embedding != null && memory.embeddingModel == activeModel) memory
            else spaces.embedPassage(memory.content)?.let { embedded ->
                val vector = embedded.vector.toByteArray()
                dao.updateEmbedding(memory.id, vector, embedded.modelId)
                memory.copy(embedding = vector, embeddingModel = embedded.modelId)
            } ?: memory.copy(embedding = null)
        }
        if (all.isEmpty()) return@withContext emptyList()
        val byId = all.associateBy { it.id }
        val selected = MemorySelector.select(
            memories = all.map { MemoryCandidate(it.id, it.content, it.source, it.embedding?.toFloatArray()) },
            query = query,
            queryEmbedding = spaces.embedQuery(query),
        ).mapNotNull { byId[it.id] }
        if (selected.isNotEmpty()) dao.markUsed(selected.map { it.id }, System.currentTimeMillis())
        selected
    }

    suspend fun setUserNameMemory(name: String) = withContext(Dispatchers.IO) {
        if (!memoryFeatureStore.isEnabled) return@withContext
        val trimmed = name.trim()
        if (trimmed.isBlank()) return@withContext

        dao.deleteMemoriesBySource(USER_PROFILE_SOURCE)
        val embedded = spaces.embedPassage("The user's name is $trimmed.")
        dao.insertMemory(
            MemoryEntity(
                id = UUID.randomUUID().toString(),
                content = "The user's name is $trimmed.",
                source = USER_PROFILE_SOURCE,
                createdAt = System.currentTimeMillis(),
                embedding = embedded?.vector?.toByteArray(),
                embeddingModel = embedded?.modelId ?: spaces.activeEmbeddingModelId,
            )
        )
    }

    suspend fun deleteMemory(id: String) = withContext(Dispatchers.IO) {
        dao.deleteMemory(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    suspend fun getAllMemories(): List<MemoryEntity> = withContext(Dispatchers.IO) {
        if (!memoryFeatureStore.isEnabled) {
            emptyList()
        } else {
            dao.getAllMemories()
        }
    }

    companion object {
        const val USER_PROFILE_SOURCE = "user_profile"
    }
}
