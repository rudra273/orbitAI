package com.example.orbitai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities  = [ChatEntity::class, MessageEntity::class, RagDocumentEntity::class, RagChunkEntity::class, MemoryEntity::class, SpaceEntity::class, ModeEntity::class],
    version   = 10,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun ragDocumentDao(): RagDocumentDao
    abstract fun memoryDao(): MemoryDao
    abstract fun spaceDao(): SpaceDao
    abstract fun modeDao(): ModeDao

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
                    CREATE TABLE IF NOT EXISTS `modes` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `systemPrompt` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                // Seed the default Orbit mode
                db.execSQL("""
                    INSERT OR IGNORE INTO modes (id, name, systemPrompt, isDefault, isActive, createdAt)
                    VALUES ('orbit_default', 'Orbit', 'You are Orbit, a helpful on-device AI assistant. Be concise, accurate, and friendly.', 1, 1, ${System.currentTimeMillis()})
                """.trimIndent())
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op in development mode: schema already uses `modes`.
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    INSERT OR IGNORE INTO modes (id, name, systemPrompt, isDefault, isActive, createdAt)
                    VALUES (
                        'concise_default',
                        'Concise',
                        'You are a concise assistant. Give short, direct answers. Use only essential details and avoid extra explanation unless asked.',
                        1,
                        1,
                        ${System.currentTimeMillis()}
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO modes (id, name, systemPrompt, isDefault, isActive, createdAt)
                    VALUES (
                        'step_by_step_default',
                        'Step-by-step',
                        'You are a step-by-step assistant. Break solutions into clear numbered steps, explain each step briefly, and keep progression logical.',
                        1,
                        1,
                        ${System.currentTimeMillis() + 1}
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // isActive was already created with the table in MIGRATION_5_6.
                // This migration exists for users who somehow have version 8
                // without the column (impossible in practice). Guard against
                // the duplicate-column crash by catching the error.
                try {
                    db.execSQL("ALTER TABLE modes ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                } catch (_: Exception) {
                    // Column already exists — nothing to do.
                }
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN imageUrisCsv TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orbitai.db",
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                ).build().also { INSTANCE = it }
            }
    }
}
