package com.example.orbitai.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RagStatus { PENDING, PROCESSING, DONE, ERROR }

data class RagDocument(
    val id: String,
    val name: String,
    val uri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val status: RagStatus,
    val chunkCount: Int,
    val addedAt: Long,
    val spaceId: String? = null,
)

@Entity(tableName = "rag_documents")
data class RagDocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val uri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val status: String,
    val chunkCount: Int,
    val addedAt: Long,
    val spaceId: String? = null,
)

@Entity(
    tableName = "rag_chunks",
    foreignKeys = [ForeignKey(
        entity        = RagDocumentEntity::class,
        parentColumns = ["id"],
        childColumns  = ["docId"],
        onDelete      = ForeignKey.CASCADE,
    )],
    indices = [Index("docId")],
)
data class RagChunkEntity(
    @PrimaryKey val id: String,
    val docId: String,
    val chunkIndex: Int,
    val content: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray? = null,
)

// ── Mappers ────────────────────────────────────────────────────────────────

fun RagDocumentEntity.toDomain() = RagDocument(
    id         = id,
    name       = name,
    uri        = uri,
    mimeType   = mimeType,
    sizeBytes  = sizeBytes,
    status     = RagStatus.valueOf(status),
    chunkCount = chunkCount,
    addedAt    = addedAt,
    spaceId    = spaceId,
)
