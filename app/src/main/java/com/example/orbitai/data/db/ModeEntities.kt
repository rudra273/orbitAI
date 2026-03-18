package com.example.orbitai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

const val ORBIT_MODE_ID = "orbit_default"

data class Mode(
    val id: String,
    val name: String,
    val systemPrompt: String,
    val isDefault: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "modes")
data class ModeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val systemPrompt: String,
    val isDefault: Boolean,
    val createdAt: Long,
)

fun ModeEntity.toDomain() = Mode(
    id           = id,
    name         = name,
    systemPrompt = systemPrompt,
    isDefault    = isDefault,
    createdAt    = createdAt,
)
