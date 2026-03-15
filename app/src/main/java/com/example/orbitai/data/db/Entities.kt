package com.example.orbitai.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.orbitai.data.Chat
import com.example.orbitai.data.Message
import com.example.orbitai.data.Role

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val modelId: String,
    val createdAt: Long,
)

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity        = ChatEntity::class,
        parentColumns = ["id"],
        childColumns  = ["chatId"],
        onDelete      = ForeignKey.CASCADE,
    )],
    indices = [Index("chatId")],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val role: String,        // "USER" | "ASSISTANT"
    val content: String,
    val timestampMs: Long,
)

/** Room relation: one chat with all its messages. */
data class ChatWithMessages(
    @Embedded val chat: ChatEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "chatId",
    )
    val messages: List<MessageEntity>,
)

// ── Mappers ────────────────────────────────────────────────────────────────

fun ChatWithMessages.toDomain(): Chat = Chat(
    id        = chat.id,
    title     = chat.title,
    modelId   = chat.modelId,
    createdAt = chat.createdAt,
    messages  = messages.sortedBy { it.timestampMs }.map { it.toDomain() },
)

fun ChatEntity.toDomain(messages: List<MessageEntity>): Chat = Chat(
    id        = id,
    title     = title,
    modelId   = modelId,
    createdAt = createdAt,
    messages  = messages.map { it.toDomain() },
)

fun MessageEntity.toDomain(): Message = Message(
    id          = id,
    role        = Role.valueOf(role),
    content     = content,
    isStreaming = false,
    timestampMs = timestampMs,
)

fun Chat.toEntity(): ChatEntity = ChatEntity(id, title, modelId, createdAt)

fun Message.toEntity(chatId: String): MessageEntity = MessageEntity(
    id          = id,
    chatId      = chatId,
    role        = role.name,
    content     = content,
    timestampMs = timestampMs,
)
