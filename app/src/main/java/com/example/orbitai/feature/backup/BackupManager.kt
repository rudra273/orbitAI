package com.example.orbitai.feature.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.orbitai.core.database.AppDatabase
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** What a backup holds, shown before the user confirms a restore. */
data class BackupPreview(
    val createdAt: Long,
    val schemaVersion: Int,
    val chats: Int,
    val memories: Int,
    val modes: Int,
    val spaces: Int,
    val includesKeys: Boolean,
)

/**
 * Backs up the database and settings into one encrypted file (f055). A restore is staged and applied
 * the next time the app starts, before the database is opened.
 */
class BackupManager(context: Context) {

    private val context = context.applicationContext
    private val gson = Gson()
    private val staging = File(this.context.filesDir, "pending_restore")

    suspend fun create(target: Uri, passphrase: CharArray, includeKeys: Boolean): Result<BackupPreview> =
        withContext(Dispatchers.IO) {
            runCatching {
                val db = AppDatabase.getInstance(context)
                val sqlite = db.openHelper.writableDatabase
                sqlite.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
                val preview = BackupPreview(
                    createdAt = System.currentTimeMillis(),
                    schemaVersion = AppDatabase.VERSION,
                    chats = count(sqlite, "chats"),
                    memories = count(sqlite, "memories"),
                    modes = count(sqlite, "modes"),
                    spaces = count(sqlite, "spaces"),
                    includesKeys = includeKeys,
                )
                val dbFile = context.getDatabasePath(DB_NAME)
                val out = context.contentResolver.openOutputStream(target, "wt") ?: error("Couldn't write to that location.")
                ZipOutputStream(BackupCodec.encryptTo(out.buffered(), passphrase)).use { zip ->
                    zip.putNextEntry(ZipEntry(MANIFEST))
                    zip.write(gson.toJson(preview).toByteArray())
                    zip.closeEntry()
                    zip.putNextEntry(ZipEntry(DB_ENTRY))
                    dbFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    zip.putNextEntry(ZipEntry(PREFS_ENTRY))
                    zip.write(gson.toJson(readPrefs(includeKeys)).toByteArray())
                    zip.closeEntry()
                }
                preview
            }
        }

    /** Decrypts and checks the file, and stages it for the next start. Nothing live is touched yet. */
    suspend fun stageRestore(source: Uri, passphrase: CharArray): Result<BackupPreview> =
        withContext(Dispatchers.IO) {
            runCatching {
                val temp = File(context.cacheDir, "restore-staging").apply { deleteRecursively(); mkdirs() }
                try {
                    val plain = File(temp, "payload.zip")
                    val input = context.contentResolver.openInputStream(source) ?: error("Couldn't open that file.")
                    input.buffered().use { src -> plain.outputStream().use { BackupCodec.decrypt(src, it, passphrase) } }

                    var preview: BackupPreview? = null
                    ZipInputStream(plain.inputStream().buffered()).use { zip ->
                        while (true) {
                            val entry = zip.nextEntry ?: break
                            when (entry.name) {
                                MANIFEST -> preview = gson.fromJson(zip.readBytes().decodeToString(), BackupPreview::class.java)
                                DB_ENTRY -> File(temp, DB_ENTRY).outputStream().use { zip.copyTo(it) }
                                PREFS_ENTRY -> File(temp, PREFS_ENTRY).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                    val found = preview ?: throw NotABackupException()
                    val dbFile = File(temp, DB_ENTRY)
                    if (!dbFile.exists() || !File(temp, PREFS_ENTRY).exists()) throw NotABackupException()
                    if (found.schemaVersion > AppDatabase.VERSION) {
                        error("This backup is from a newer version of Orbit. Update the app, then restore it.")
                    }
                    SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { check ->
                        check.rawQuery("PRAGMA integrity_check", null).use { c ->
                            check(c.moveToFirst() && c.getString(0) == "ok") { "The backup's data is damaged." }
                        }
                    }
                    staging.deleteRecursively()
                    staging.mkdirs()
                    dbFile.copyTo(File(staging, DB_ENTRY))
                    File(temp, PREFS_ENTRY).copyTo(File(staging, PREFS_ENTRY))
                    found
                } finally {
                    temp.deleteRecursively()
                }
            }
        }

    fun hasPendingRestore(): Boolean = File(staging, DB_ENTRY).exists()

    fun cancelPendingRestore() {
        staging.deleteRecursively()
    }

    private fun count(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM $table").use { if (it.moveToFirst()) it.getInt(0) else 0 }

    private fun readPrefs(includeKeys: Boolean): JsonObject {
        val root = JsonObject()
        for (name in PREF_FILES) {
            val all = context.getSharedPreferences(name, Context.MODE_PRIVATE).all
            val file = JsonObject()
            for ((key, value) in all) {
                if (!includeKeys && name == SECRET_PREFS && key in SECRET_KEYS) continue
                val entry = JsonObject()
                when (value) {
                    is String -> { entry.addProperty("t", "s"); entry.addProperty("v", value) }
                    is Int -> { entry.addProperty("t", "i"); entry.addProperty("v", value) }
                    is Long -> { entry.addProperty("t", "l"); entry.addProperty("v", value) }
                    is Float -> { entry.addProperty("t", "f"); entry.addProperty("v", value) }
                    is Boolean -> { entry.addProperty("t", "b"); entry.addProperty("v", value) }
                    is Set<*> -> {
                        entry.addProperty("t", "ss")
                        entry.add("v", gson.toJsonTree(value.filterIsInstance<String>()))
                    }
                    else -> continue
                }
                file.add(key, entry)
            }
            root.add(name, file)
        }
        return root
    }

    companion object {
        private const val DB_NAME = "orbitai.db"
        private const val MANIFEST = "manifest.json"
        private const val DB_ENTRY = "orbitai.db"
        private const val PREFS_ENTRY = "prefs.json"
        private const val SECRET_PREFS = "orbitai_prefs"
        private val SECRET_KEYS = setOf("hf_token", "gemini_api_key")

        /** Settings worth moving to a new phone; per-device state (backends, downloads, model files) is left out. */
        private val PREF_FILES = listOf(
            "orbitai_prefs", "orbitai_chat", "orbitai_voice", "orbitai_theme_prefs", "orbitai_tool_prefs",
            "orbitai_saved_prompts", "orbitai_onboarding_prefs", "orbitai_download_prefs", "inference_prefs",
            "mode_inference_prefs",
        )

        /** Call first thing in `Application.onCreate`, before the database or any settings are read. */
        fun applyPendingRestore(context: Context) {
            val staging = File(context.filesDir, "pending_restore")
            val dbFile = File(staging, DB_ENTRY)
            if (!dbFile.exists()) return
            try {
                val target = context.getDatabasePath(DB_NAME)
                val incoming = File(target.path + ".restoring")
                dbFile.copyTo(incoming, overwrite = true)
                listOf("", "-wal", "-shm", "-journal").forEach { File(target.path + it).delete() }
                check(incoming.renameTo(target)) { "Couldn't put the restored data in place." }

                val root = com.google.gson.JsonParser.parseString(File(staging, PREFS_ENTRY).readText()).asJsonObject
                for ((name, fileJson) in root.entrySet()) {
                    if (name !in PREF_FILES) continue
                    val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    prefs.edit(commit = true) {
                    for (key in prefs.all.keys) {
                        // Keys excluded from the backup stay as they are on this phone.
                        if (name == SECRET_PREFS && key in SECRET_KEYS && !fileJson.asJsonObject.has(key)) continue
                        remove(key)
                    }
                    for ((key, raw) in fileJson.asJsonObject.entrySet()) {
                        val entry = raw.asJsonObject
                        val v = entry.get("v")
                        when (entry.get("t").asString) {
                            "s" -> putString(key, v.asString)
                            "i" -> putInt(key, v.asInt)
                            "l" -> putLong(key, v.asLong)
                            "f" -> putFloat(key, v.asFloat)
                            "b" -> putBoolean(key, v.asBoolean)
                            "ss" -> putStringSet(key, v.asJsonArray.map { it.asString }.toSet())
                        }
                    }
                    }
                }
            } finally {
                staging.deleteRecursively()
            }
        }
    }
}
