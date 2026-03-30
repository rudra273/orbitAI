package com.example.orbitai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

const val ORBIT_MODE_ID = "orbit_default"
const val CONCISE_MODE_ID = "concise_default"
const val STEP_BY_STEP_MODE_ID = "step_by_step_default"

data class Mode(
    val id: String,
    val name: String,
    val systemPrompt: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "modes")
data class ModeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val systemPrompt: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
)

fun ModeEntity.toDomain() = Mode(
    id           = id,
    name         = name,
    systemPrompt = systemPrompt,
    isDefault    = isDefault,
    isActive     = isActive,
    createdAt    = createdAt,
)
