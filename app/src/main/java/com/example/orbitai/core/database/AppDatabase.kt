package com.example.orbitai.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities  = [ChatEntity::class, MessageEntity::class, RagDocumentEntity::class, RagChunkEntity::class, MemoryEntity::class, SpaceEntity::class, ModeEntity::class, ReminderEntity::class, MessageFtsEntity::class, MessageSourceEntity::class],
    version   = AppDatabase.VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun ragDocumentDao(): RagDocumentDao
    abstract fun memoryDao(): MemoryDao
    abstract fun spaceDao(): SpaceDao
    abstract fun modeDao(): ModeDao
    abstract fun reminderDao(): ReminderDao
    abstract fun messageSourceDao(): MessageSourceDao

    companion object {
        const val VERSION = 20

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

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN modeName TEXT")
            }
        }

        internal val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `reminders` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `triggerAt` INTEGER NOT NULL,
                        `delivered` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        internal val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN repeat TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN completed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN scheduledAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE reminders SET scheduledAt = triggerAt")
            }
        }

        internal val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId_timestampMs` ON `messages` (`chatId`, `timestampMs`)")
            }
        }

        internal val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rag_documents ADD COLUMN errorMessage TEXT")
                db.execSQL("ALTER TABLE rag_documents ADD COLUMN pagesDone INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rag_documents ADD COLUMN pagesTotal INTEGER NOT NULL DEFAULT 0")
            }
        }

        internal val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memories ADD COLUMN embedding BLOB")
                db.execSQL("ALTER TABLE memories ADD COLUMN useCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE memories ADD COLUMN lastUsedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN usedMemories TEXT")
            }
        }

        internal val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chats ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chats ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chats ADD COLUMN isTitleLocked INTEGER NOT NULL DEFAULT 0")
                // Same SQL Room generates for MessageFtsEntity (see schemas/17.json).
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `messages_fts` USING FTS4(`content` TEXT NOT NULL, content=`messages`)")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_BEFORE_UPDATE BEFORE UPDATE ON `messages` BEGIN DELETE FROM `messages_fts` WHERE `docid`=OLD.`rowid`; END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_BEFORE_DELETE BEFORE DELETE ON `messages` BEGIN DELETE FROM `messages_fts` WHERE `docid`=OLD.`rowid`; END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_AFTER_UPDATE AFTER UPDATE ON `messages` BEGIN INSERT INTO `messages_fts`(`docid`, `content`) VALUES (NEW.`rowid`, NEW.`content`); END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_messages_fts_AFTER_INSERT AFTER INSERT ON `messages` BEGIN INSERT INTO `messages_fts`(`docid`, `content`) VALUES (NEW.`rowid`, NEW.`content`); END")
                // Index the messages that already exist.
                db.execSQL("INSERT INTO messages_fts(messages_fts) VALUES('rebuild')")
            }
        }

        internal val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `message_sources` (`messageId` TEXT NOT NULL, `rank` INTEGER NOT NULL, `chunkId` TEXT NOT NULL, `docId` TEXT NOT NULL, `docName` TEXT NOT NULL, `page` INTEGER, `excerpt` TEXT NOT NULL, PRIMARY KEY(`messageId`, `rank`), FOREIGN KEY(`messageId`) REFERENCES `messages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_sources_messageId` ON `message_sources` (`messageId`)")
            }
        }

        /** Reminder repeats move to RRULE-style strings (see RepeatRule); "NONE" stays as is. */
        internal val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE reminders SET repeat = 'FREQ=DAILY' WHERE repeat = 'DAILY'")
                db.execSQL("UPDATE reminders SET repeat = 'FREQ=WEEKLY' WHERE repeat = 'WEEKLY'")
            }
        }

        internal val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE modes ADD COLUMN defaultModelId TEXT")
                db.execSQL("ALTER TABLE modes ADD COLUMN defaultSpaceIds TEXT")
                db.execSQL("ALTER TABLE modes ADD COLUMN accentColor INTEGER")
            }
        }

        internal val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
        )

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orbitai.db",
                ).addMigrations(*ALL_MIGRATIONS).build().also { INSTANCE = it }
            }
    }
}
