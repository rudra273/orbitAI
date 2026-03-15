package com.example.orbitai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val content: String,
    val source: String, // "auto" or "explicit"
    val createdAt: Long,
)
