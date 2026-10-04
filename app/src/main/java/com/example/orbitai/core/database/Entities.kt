package com.example.orbitai.core.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.orbitai.feature.chat.Chat
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val modelId: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val isPinned: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    /** True once the user renamed the chat; automatic titles leave it alone. */
    @ColumnInfo(defaultValue = "0") val isTitleLocked: Boolean = false,
)

/** Full-text index over message content, kept in sync with `messages` by Room's triggers. */
@Fts4(contentEntity = MessageEntity::class)
@Entity(tableName = "messages_fts")
data class MessageFtsEntity(val content: String)

/** A message matching a search, with its chat and a highlighted excerpt. */
data class MessageSearchRow(
    val messageId: String,
    val chatId: String,
    val chatTitle: String,
    val snippet: String,
    val timestampMs: Long,
)

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity        = ChatEntity::class,
        parentColumns = ["id"],
        childColumns  = ["chatId"],
        onDelete      = ForeignKey.CASCADE,
    )],
    indices = [Index("chatId"), Index(value = ["chatId", "timestampMs"])],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val role: String,        // "USER" | "ASSISTANT"
    val content: String,
    val modeName: String? = null,
    val imageUrisCsv: String? = null,
    val timestampMs: Long,
    /** Memories sent with this reply, one per line. */
    val usedMemories: String? = null,
)

/** Chat list row: chat fields plus its newest message, without loading other messages. */
data class ChatSummaryRow(
    val id: String,
    val title: String,
    val modelId: String,
    val createdAt: Long,
    val lastMessagePreview: String?,
    val lastMessageAt: Long?,
    val isPinned: Boolean,
    val isArchived: Boolean,
)

fun ChatSummaryRow.toDomain() = com.example.orbitai.feature.chat.ChatSummary(
    id = id,
    title = title,
    modelId = modelId,
    createdAt = createdAt,
    lastMessagePreview = lastMessagePreview,
    lastMessageAt = lastMessageAt,
    isPinned = isPinned,
    isArchived = isArchived,
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
    modeName    = modeName,
    imageUris   = imageUrisCsv?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    isStreaming = false,
    timestampMs = timestampMs,
    usedMemories = usedMemories?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
)

fun Chat.toEntity(): ChatEntity = ChatEntity(id, title, modelId, createdAt)

fun Message.toEntity(chatId: String): MessageEntity = MessageEntity(
    id          = id,
    chatId      = chatId,
    role        = role.name,
    content     = content,
    modeName    = modeName,
    imageUrisCsv = if (imageUris.isEmpty()) null else imageUris.joinToString(","),
    timestampMs = timestampMs,
    usedMemories = usedMemories.takeIf { it.isNotEmpty() }?.joinToString("\n"),
)
