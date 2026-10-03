package com.example.orbitai.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.ColumnInfo
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val triggerAt: Long,
    val delivered: Boolean = false,
    @ColumnInfo(defaultValue = "'NONE'") val repeat: String = "NONE",
    @ColumnInfo(defaultValue = "0") val completed: Boolean = false,
    @ColumnInfo(defaultValue = "0") val scheduledAt: Long = triggerAt,
)

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders ORDER BY completed ASC, triggerAt ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE delivered = 0 AND completed = 0")
    suspend fun pending(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun find(id: String): ReminderEntity?

    @Query("UPDATE reminders SET delivered = 1 WHERE id = :id")
    suspend fun markDelivered(id: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)
}
