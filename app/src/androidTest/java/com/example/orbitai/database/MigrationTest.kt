package com.example.orbitai.database

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.orbitai.core.database.AppDatabase
import com.example.orbitai.core.database.CONCISE_MODE_ID
import com.example.orbitai.core.database.ORBIT_MODE_ID
import com.example.orbitai.core.database.STEP_BY_STEP_MODE_ID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class MigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, AppDatabase::class.java)

    /** Builds the original v1 schema by hand, then runs the production migration chain. */
    @Test
    fun version1UpgradesThroughEveryMigrationAndKeepsChats() = runBlocking {
        val name = "migration-v1-${UUID.randomUUID()}.db"
        try {
            context.getDatabasePath(name).parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
                db.execSQL("CREATE TABLE IF NOT EXISTS `chats` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `modelId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `chatId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestampMs` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`chatId`) REFERENCES `chats`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId` ON `messages` (`chatId`)")
                db.execSQL("INSERT INTO chats (id, title, modelId, createdAt) VALUES ('chat', 'Old chat', 'gemma3-1b', 1)")
                db.execSQL("INSERT INTO messages (id, chatId, role, content, timestampMs) VALUES ('msg', 'chat', 'USER', 'Hello from v1', 2)")
                db.version = 1
            }

            // Room validates the migrated schema against the current entities when it opens.
            val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(*AppDatabase.ALL_MIGRATIONS)
                .build()
            try {
                val chat = upgraded.chatDao().getChatById("chat")
                assertEquals("Old chat", chat?.title)
                val message = upgraded.messageDao().getMessages("chat").single()
                assertEquals("Hello from v1", message.content)
                assertNull(message.imageUrisCsv)
                assertNull(message.modeName)
                val modeIds = upgraded.openHelper.readableDatabase.query("SELECT id FROM modes").use { cursor ->
                    buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
                }
                assertEquals(setOf(ORBIT_MODE_ID, CONCISE_MODE_ID, STEP_BY_STEP_MODE_ID), modeIds)
                assertEquals(AppDatabase.VERSION, upgraded.openHelper.readableDatabase.version)
            } finally {
                upgraded.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    /**
     * Seeds every table at schema 13 and migrates to the current version.
     * When the version is bumped, this covers 13 → latest automatically.
     */
    @Test
    fun version13SeededDataSurvivesMigrationToLatest() {
        val name = "migration-v13-${UUID.randomUUID()}.db"
        helper.createDatabase(name, 13).apply {
            execSQL("INSERT INTO chats (id, title, modelId, createdAt) VALUES ('chat', 'Chat', 'gemma3-1b', 1)")
            execSQL("INSERT INTO messages (id, chatId, role, content, modeName, imageUrisCsv, timestampMs) VALUES ('msg', 'chat', 'ASSISTANT', 'Answer', 'Orbit', NULL, 2)")
            execSQL("INSERT INTO spaces (id, name, createdAt) VALUES ('space', 'Notes', 1)")
            execSQL("INSERT INTO rag_documents (id, name, uri, mimeType, sizeBytes, status, chunkCount, addedAt, spaceId) VALUES ('doc', 'a.txt', 'content://a', 'text/plain', 10, 'DONE', 1, 1, 'space')")
            execSQL("INSERT INTO rag_chunks (id, docId, chunkIndex, content, embedding) VALUES ('chunk', 'doc', 0, 'Chunk text', NULL)")
            execSQL("INSERT INTO memories (id, content, source, createdAt) VALUES ('mem', 'Likes tea', 'manual', 1)")
            execSQL("INSERT INTO modes (id, name, systemPrompt, isDefault, isActive, createdAt) VALUES ('custom', 'Custom', 'Prompt', 0, 1, 1)")
            execSQL("INSERT INTO reminders (id, title, description, triggerAt, delivered, repeat, completed, scheduledAt) VALUES ('rem', 'Call', '', 100, 0, 'DAILY', 0, 100)")
            close()
        }

        helper.runMigrationsAndValidate(name, AppDatabase.VERSION, true, *AppDatabase.ALL_MIGRATIONS).use { db ->
            fun count(table: String) = db.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }
            for (table in listOf("chats", "messages", "spaces", "rag_documents", "rag_chunks", "memories", "reminders")) {
                assertEquals("$table rows", 1, count(table))
            }
            db.query("SELECT repeat FROM reminders WHERE id = 'rem'").use {
                it.moveToFirst()
                assertEquals("DAILY", it.getString(0))
            }
            db.query("SELECT status, errorMessage, pagesDone, pagesTotal FROM rag_documents WHERE id = 'doc'").use {
                it.moveToFirst()
                assertEquals("DONE", it.getString(0))
                assertNull(it.getString(1))
                assertEquals(0, it.getInt(2))
                assertEquals(0, it.getInt(3))
            }
            db.query("SELECT content, useCount, lastUsedAt, embedding FROM memories WHERE id = 'mem'").use {
                it.moveToFirst()
                assertEquals("Likes tea", it.getString(0))
                assertEquals(0, it.getInt(1))
                assertEquals(0L, it.getLong(2))
                assertTrue(it.isNull(3))
            }
            db.query("SELECT usedMemories FROM messages WHERE id = 'msg'").use {
                it.moveToFirst()
                assertTrue(it.isNull(0))
            }
            db.query("SELECT isPinned, isArchived, isTitleLocked FROM chats WHERE id = 'chat'").use {
                it.moveToFirst()
                assertEquals(listOf(0, 0, 0), listOf(it.getInt(0), it.getInt(1), it.getInt(2)))
            }
            // Existing messages are searchable, and new ones are indexed by the triggers.
            fun matches(term: String) = db.query("SELECT COUNT(*) FROM messages_fts WHERE messages_fts MATCH '$term'").use { it.moveToFirst(); it.getInt(0) }
            assertEquals(1, matches("Answer"))
            db.execSQL("INSERT INTO messages (id, chatId, role, content, modeName, imageUrisCsv, timestampMs) VALUES ('msg2', 'chat', 'USER', 'Pineapple pizza', NULL, NULL, 3)")
            assertEquals(1, matches("pineapple"))
            // Sources attach to a reply and go away with it.
            db.execSQL("INSERT INTO message_sources (messageId, rank, chunkId, docId, docName, page, excerpt) VALUES ('msg', 1, 'chunk', 'doc', 'a.txt', NULL, 'Chunk text')")
            assertEquals(1, count("message_sources"))
            db.execSQL("PRAGMA foreign_keys=ON")
            db.execSQL("DELETE FROM messages WHERE id = 'msg'")
            assertEquals(0, count("message_sources"))
        }
        context.deleteDatabase(name)
    }
}
