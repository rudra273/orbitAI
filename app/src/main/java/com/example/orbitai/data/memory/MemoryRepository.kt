package com.example.orbitai.data.memory

import android.content.Context
import com.example.orbitai.data.db.AppDatabase
import com.example.orbitai.data.db.MemoryEntity
import com.example.orbitai.data.MemoryFeatureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class MemoryRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).memoryDao()
    private val memoryFeatureStore = MemoryFeatureStore(context)

    val memories: Flow<List<MemoryEntity>> = dao.observeMemories()

    suspend fun addMemory(content: String, source: String = "auto") = withContext(Dispatchers.IO) {
        if (!memoryFeatureStore.isEnabled) return@withContext
        val trimmed = content.trim()
        if (trimmed.isBlank()) return@withContext

        dao.insertMemory(
            MemoryEntity(
                id        = UUID.randomUUID().toString(),
                content   = trimmed,
                source    = source,
                createdAt = System.currentTimeMillis(),
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
}
