package com.example.orbitai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities  = [ChatEntity::class, MessageEntity::class, RagDocumentEntity::class, RagChunkEntity::class, MemoryEntity::class, SpaceEntity::class, AgentEntity::class],
    version   = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun ragDocumentDao(): RagDocumentDao
    abstract fun memoryDao(): MemoryDao
    abstract fun spaceDao(): SpaceDao
    abstract fun agentDao(): AgentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `rag_documents` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `uri` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `chunkCount` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `rag_chunks` (
                        `id` TEXT NOT NULL,
                        `docId` TEXT NOT NULL,
                        `chunkIndex` INTEGER NOT NULL,
                        `content` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`docId`) REFERENCES `rag_documents`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rag_chunks_docId` ON `rag_chunks` (`docId`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rag_chunks ADD COLUMN embedding BLOB")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `memories` (
                        `id` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rag_documents ADD COLUMN spaceId TEXT")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `spaces` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `agents` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `systemPrompt` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                // Seed the default Orbit agent
                db.execSQL("""
                    INSERT OR IGNORE INTO agents (id, name, systemPrompt, isDefault, createdAt)
                    VALUES ('orbit_default', 'Orbit', 'You are Orbit, a helpful on-device AI assistant. Be concise, accurate, and friendly.', 1, ${System.currentTimeMillis()})
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orbitai.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { INSTANCE = it }
            }
    }
}
