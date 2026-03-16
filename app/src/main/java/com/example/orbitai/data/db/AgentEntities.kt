package com.example.orbitai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

const val ORBIT_AGENT_ID = "orbit_default"

data class Agent(
    val id: String,
    val name: String,
    val systemPrompt: String,
    val isDefault: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val systemPrompt: String,
    val isDefault: Boolean,
    val createdAt: Long,
)

fun AgentEntity.toDomain() = Agent(
    id           = id,
    name         = name,
    systemPrompt = systemPrompt,
    isDefault    = isDefault,
    createdAt    = createdAt,
)
