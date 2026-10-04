package com.example.orbitai.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val content: String,
    val source: String, // "auto", "suggested", "explicit", "manual" or "user_profile"
    val createdAt: Long,
    /** Embedding of [content] for relevance search; null until computed. */
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val embedding: ByteArray? = null,
    /** How many replies this memory was sent with. */
    @ColumnInfo(defaultValue = "0") val useCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val lastUsedAt: Long = 0,
)
