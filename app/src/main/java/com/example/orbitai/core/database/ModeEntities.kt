package com.example.orbitai.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

const val ORBIT_MODE_ID = "orbit_default"
const val CONCISE_MODE_ID = "concise_default"
const val STEP_BY_STEP_MODE_ID = "step_by_step_default"
const val BUBBLE_MODE_ID = "bubble_default"

data class Mode(
    val id: String,
    val name: String,
    val systemPrompt: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val preset: ModePreset = ModePreset(),
)

/** What selecting a mode also applies (f039). Everything is optional and overridable per chat. */
data class ModePreset(
    /** Model to switch the chat to, if it is downloaded. */
    val defaultModelId: String? = null,
    /** Spaces to turn on. */
    val defaultSpaceIds: List<String> = emptyList(),
    /** Colour of the mode chip, as 0xRRGGBB. */
    val accentColor: Int? = null,
)

@Entity(tableName = "modes")
data class ModeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val systemPrompt: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val defaultModelId: String? = null,
    /** Comma-separated Space ids. */
    val defaultSpaceIds: String? = null,
    val accentColor: Int? = null,
)

fun ModeEntity.toDomain() = Mode(
    id           = id,
    name         = name,
    systemPrompt = systemPrompt,
    isDefault    = isDefault,
    isActive     = isActive,
    createdAt    = createdAt,
    preset       = ModePreset(
        defaultModelId = defaultModelId,
        defaultSpaceIds = defaultSpaceIds?.split(',')?.filter { it.isNotBlank() }.orEmpty(),
        accentColor = accentColor,
    ),
)
