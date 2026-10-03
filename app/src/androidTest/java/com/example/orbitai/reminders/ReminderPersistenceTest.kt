package com.example.orbitai.reminders

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.orbitai.core.database.AppDatabase
import com.example.orbitai.core.database.ReminderEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ReminderPersistenceTest {
    @Test
    fun pendingRemindersSurviveReopenAndDeliveredOrCancelledOnesDoNotReturn() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reminder-test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val first = open()
        try {
            val dao = first.reminderDao()
            dao.insert(ReminderEntity("pending", "Same title", "", 100L))
            dao.insert(ReminderEntity("delivered", "Same title", "", 100L))
            dao.insert(ReminderEntity("cancelled", "Same title", "", 100L))
            dao.markDelivered("delivered")
            dao.delete("cancelled")
            dao.insert(ReminderEntity("completed", "Done", "", 100L, completed = true))
            first.close()
            val reopened = open()
            try {
                assertEquals(listOf("pending"), reopened.reminderDao().pending().map { it.id })
                assertTrue(reopened.reminderDao().find("delivered")!!.delivered)
                assertNull(reopened.reminderDao().find("cancelled"))
            } finally { reopened.close() }
        } finally {
            first.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun version11UpgradePreservesExistingDataAndCreatesValidatedReminderTable() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-test-${UUID.randomUUID()}.db"
        try {
            val fixture = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            fixture.openHelper.writableDatabase.execSQL(
                "INSERT INTO memories (id, content, source, createdAt) VALUES ('kept', 'Keep this memory', 'manual', 1)"
            )
            fixture.close()
            // Version 11 has the same schema except for the new reminders table.
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("DROP TABLE reminders")
                it.version = 11
            }
            val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13).build()
            try {
                upgraded.reminderDao().insert(ReminderEntity("new", "Reminder", "", 100L))
                assertEquals("Reminder", upgraded.reminderDao().find("new")?.title)
                upgraded.openHelper.readableDatabase.query("SELECT content FROM memories WHERE id = 'kept'").use {
                    assertTrue(it.moveToFirst())
                    assertEquals("Keep this memory", it.getString(0))
                }
            } finally { upgraded.close() }
        } finally { context.deleteDatabase(name) }
    }
    @Test
    fun version12UpgradeKeepsExistingReminderAndInitializesRecurrence() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reminder-upgrade-${UUID.randomUUID()}.db"
        try {
            val fixture = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            fixture.openHelper.writableDatabase.let { db ->
                db.execSQL("DROP TABLE reminders")
                AppDatabase.MIGRATION_11_12.migrate(db)
                db.execSQL("INSERT INTO reminders (id, title, description, triggerAt, delivered) VALUES ('kept', 'Call Mom', '', 12345, 0)")
            }
            fixture.close()
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = 12 }
            val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_12_13).build()
            try {
                val reminder = upgraded.reminderDao().find("kept")!!
                assertEquals(12345L, reminder.triggerAt)
                assertEquals(12345L, reminder.scheduledAt)
                assertEquals("NONE", reminder.repeat)
                assertFalse(reminder.completed)
            } finally { upgraded.close() }
        } finally { context.deleteDatabase(name) }
    }
}
