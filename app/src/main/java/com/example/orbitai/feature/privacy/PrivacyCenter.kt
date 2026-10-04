package com.example.orbitai.feature.privacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.orbitai.appContainer
import com.example.orbitai.core.database.AppDatabase
import com.example.orbitai.core.model.CustomModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** What Orbit keeps on the phone, by category, and the ways to remove it (f057). */
data class StorageReport(
    val chats: Int,
    val memories: Int,
    val spaces: Int,
    val documents: Int,
    val reminders: Int,
    val databaseBytes: Long,
    val modelBytes: Long,
    val cacheBytes: Long,
)

class PrivacyCenter(context: Context) {

    private val context = context.applicationContext
    private val container get() = context.appContainer
    private val modelsDir get() = File(context.getExternalFilesDir(null), "models")

    suspend fun report(): StorageReport = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context).openHelper.readableDatabase
        fun count(table: String) = db.query("SELECT COUNT(*) FROM $table").use { if (it.moveToFirst()) it.getInt(0) else 0 }
        StorageReport(
            chats = count("chats"),
            memories = count("memories"),
            spaces = count("spaces"),
            documents = count("rag_documents"),
            reminders = count("reminders"),
            databaseBytes = listOf("", "-wal", "-shm").sumOf { File(context.getDatabasePath("orbitai.db").path + it).length() },
            modelBytes = modelsDir.sizeOf(),
            cacheBytes = context.cacheDir.sizeOf() + (context.externalCacheDir?.sizeOf() ?: 0L),
        )
    }

    suspend fun clearChats() = withContext(Dispatchers.IO) {
        AppDatabase.getInstance(context).openHelper.writableDatabase.execSQL("DELETE FROM chats")
    }

    suspend fun clearMemories() = container.memoryRepository.clearAll()

    suspend fun clearSpaces() = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context).openHelper.writableDatabase
        db.execSQL("DELETE FROM rag_chunks")
        db.execSQL("DELETE FROM rag_documents")
        db.execSQL("DELETE FROM spaces")
        container.spaceRepository.invalidateIndex()
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
    }

    /** Frees the loaded model first. Imported models are removed from the list too. */
    suspend fun deleteModels() = withContext(Dispatchers.IO) {
        container.llmRepository.releaseIfIdle()
        CustomModels.models.value.forEach { CustomModels.remove(it.id) }
        modelsDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    /**
     * Returns the app to a fresh-install state: cancels reminders, deletes the database, every
     * settings file, saved files and caches (and the models when asked), then ends the process.
     */
    suspend fun wipeEverything(includeModels: Boolean): Nothing {
        withContext(Dispatchers.IO) {
            runCatching {
                val dao = AppDatabase.getInstance(context).reminderDao()
                dao.pending().forEach { container.reminderScheduler.cancel(it.id) }
            }
            container.llmRepository.releaseIfIdle()
            AppDatabase.getInstance(context).close()
            context.deleteDatabase("orbitai.db")
            File(context.applicationInfo.dataDir, "shared_prefs").listFiles()?.forEach {
                context.deleteSharedPreferences(it.name.removeSuffix(".xml"))
            }
            context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
            if (includeModels) modelsDir.listFiles()?.forEach { it.deleteRecursively() }
        }
        android.os.Process.killProcess(android.os.Process.myPid())
        throw IllegalStateException("unreachable")
    }

    private fun File.sizeOf(): Long = if (!exists()) 0L else walkTopDown().filter { it.isFile }.sumOf { it.length() }

    companion object {
        fun appSettingsIntent(context: Context): Intent =
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
