package com.example.orbitai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Space(
    val id: String,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
)

fun SpaceEntity.toDomain() = Space(id = id, name = name, createdAt = createdAt)
