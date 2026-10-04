package com.example.orbitai.feature.spaces

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.orbitai.appContainer
import com.example.orbitai.core.database.AppDatabase
import kotlinx.coroutines.delay

/**
 * After the embedding model is switched, makes new vectors for chunks and memories that still hold
 * vectors from the old model. Until a chunk is done it is found by keywords only. Resumable: each
 * batch is saved, and a restart carries on where it stopped. Waits while a reply is generating.
 */
class ReembedWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getInstance(context)
    private val llmRepository = context.appContainer.llmRepository
    private val spaces = context.appContainer.spaceRepository

    override suspend fun doWork(): Result {
        val model = spaces.activeEmbeddingModelId
        try {
            while (true) {
                val chunks = db.ragDocumentDao().chunksToEmbed(model, BATCH)
                if (chunks.isEmpty()) break
                waitWhileModelBusy()
                for (chunk in chunks) {
                    val embedded = spaces.embedPassage(chunk.content) ?: return Result.success() // Model not downloaded.
                    db.ragDocumentDao().updateChunkEmbedding(chunk.id, embedded.vector.toByteArray(), embedded.modelId)
                }
                spaces.invalidateIndex()
            }
            while (true) {
                val memories = db.memoryDao().memoriesToEmbed(model, BATCH)
                if (memories.isEmpty()) break
                for (memory in memories) {
                    val embedded = spaces.embedPassage(memory.content) ?: return Result.success()
                    db.memoryDao().updateEmbedding(memory.id, embedded.vector.toByteArray(), embedded.modelId)
                }
            }
        } finally {
            spaces.invalidateIndex()
        }
        return Result.success()
    }

    private suspend fun waitWhileModelBusy() {
        while (llmRepository.isInUse()) delay(BUSY_POLL_MS)
    }

    companion object {
        private const val BATCH = 20
        private const val BUSY_POLL_MS = 2_000L
        private const val WORK_NAME = "reembed"

        /** Starts (or restarts, after another model switch) the background re-indexing. */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReembedWorker>().build(),
            )
        }
    }
}
