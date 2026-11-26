package com.example.orbitai.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    fun observeChat(chatId: String): Flow<ChatEntity?>

    @Query(
        """
        SELECT c.id, c.title, c.modelId, c.createdAt, c.isPinned, c.isArchived,
            (SELECT substr(m.content, 1, 200) FROM messages m
                WHERE m.chatId = c.id ORDER BY m.timestampMs DESC LIMIT 1) AS lastMessagePreview,
            (SELECT MAX(m.timestampMs) FROM messages m WHERE m.chatId = c.id) AS lastMessageAt
        FROM chats c
        ORDER BY c.createdAt DESC
        """
    )
    fun observeChatSummaries(): Flow<List<ChatSummaryRow>>

    @Query(
        """
        SELECT * FROM chats c
        WHERE c.isArchived = 0 AND NOT EXISTS (
            SELECT 1 FROM messages m
            WHERE m.chatId = c.id AND m.role = 'USER'
        )
        ORDER BY c.createdAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestEmptyChatWithoutUserMessages(): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("UPDATE chats SET title = :title WHERE id = :chatId")
    suspend fun updateTitle(chatId: String, title: String)

    @Query("UPDATE chats SET modelId = :modelId WHERE id = :chatId")
    suspend fun updateModelId(chatId: String, modelId: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("UPDATE chats SET title = :title, isTitleLocked = 1 WHERE id = :chatId")
    suspend fun rename(chatId: String, title: String)

    @Query("UPDATE chats SET isPinned = :pinned WHERE id IN (:chatIds)")
    suspend fun setPinned(chatIds: List<String>, pinned: Boolean)

    @Query("UPDATE chats SET isArchived = :archived, isPinned = 0 WHERE id IN (:chatIds)")
    suspend fun setArchived(chatIds: List<String>, archived: Boolean)

    /** Messages matching an FTS [match] expression, newest first, with a highlighted excerpt. */
    @Query(
        """
        SELECT m.id AS messageId, m.chatId AS chatId, c.title AS chatTitle, m.timestampMs AS timestampMs,
            snippet(messages_fts, '[', ']', '…', -1, 10) AS snippet
        FROM messages_fts
        JOIN messages m ON m.rowid = messages_fts.rowid
        JOIN chats c ON c.id = m.chatId
        WHERE messages_fts MATCH :match
        ORDER BY m.timestampMs DESC
        LIMIT :limit
        """
    )
    suspend fun searchMessages(match: String, limit: Int = 50): List<MessageSearchRow>
}

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestampMs ASC")
    suspend fun getMessages(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestampMs ASC")
    fun observeMessages(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: MessageEntity)

    /** Update content of the last message in a chat (streaming accumulation). */
    @Query("""
        UPDATE messages SET content = :content
        WHERE id = (
            SELECT id FROM messages WHERE chatId = :chatId
            ORDER BY timestampMs DESC LIMIT 1
        )
    """)
    suspend fun updateLastMessageContent(chatId: String, content: String)

    @Query("UPDATE messages SET content = :content WHERE id = :messageId")
    suspend fun updateMessageContentById(messageId: String, content: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    /** Deletes the message at [timestampMs] and everything after it in the chat. */
    @Query("DELETE FROM messages WHERE chatId = :chatId AND timestampMs >= :timestampMs")
    suspend fun deleteMessagesFrom(chatId: String, timestampMs: Long)
}

@Dao
interface RagDocumentDao {

    @Query("SELECT * FROM rag_documents ORDER BY addedAt DESC")
    fun observeDocuments(): Flow<List<RagDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: RagDocumentEntity)

    @Query("UPDATE rag_documents SET status = :status, chunkCount = :chunkCount WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, chunkCount: Int)

    @Query("DELETE FROM rag_documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<RagChunkEntity>)

    @Query("SELECT * FROM rag_chunks WHERE docId = :docId ORDER BY chunkIndex ASC")
    suspend fun getChunks(docId: String): List<RagChunkEntity>

    @Query("SELECT * FROM rag_chunks WHERE content LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchChunks(query: String, limit: Int = 5): List<RagChunkEntity>

    @Query("SELECT c.* FROM rag_chunks c INNER JOIN rag_documents d ON c.docId = d.id WHERE d.name LIKE '%' || :query || '%' ORDER BY c.chunkIndex ASC LIMIT :limit")
    suspend fun searchChunksByDocName(query: String, limit: Int = 10): List<RagChunkEntity>

    @Query("SELECT * FROM rag_chunks WHERE embeddingModel != :model OR embedding IS NULL LIMIT :limit")
    suspend fun chunksToEmbed(model: String, limit: Int): List<RagChunkEntity>

    @Query("SELECT COUNT(*) FROM rag_chunks WHERE embeddingModel != :model OR embedding IS NULL")
    fun observeChunksToEmbed(model: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM rag_chunks WHERE embeddingModel != :model OR embedding IS NULL")
    suspend fun countChunksToEmbed(model: String): Int

    @Query("UPDATE rag_chunks SET embedding = :embedding, embeddingModel = :model WHERE id = :id")
    suspend fun updateChunkEmbedding(id: String, embedding: ByteArray, model: String)

    @Query("SELECT * FROM rag_chunks WHERE embedding IS NOT NULL")
    suspend fun getAllChunksWithEmbeddings(): List<RagChunkEntity>

    @Query("SELECT * FROM rag_documents WHERE id = :id")
    suspend fun getDocument(id: String): RagDocumentEntity?

    @Query("SELECT * FROM rag_documents WHERE status IN ('PENDING', 'PROCESSING')")
    suspend fun getUnfinishedDocuments(): List<RagDocumentEntity>

    @Query("UPDATE rag_documents SET name = :name, sizeBytes = :sizeBytes WHERE id = :id")
    suspend fun renameDocument(id: String, name: String, sizeBytes: Long)

    @Query("UPDATE rag_documents SET status = :status, chunkCount = :chunkCount, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateResult(id: String, status: String, chunkCount: Int, errorMessage: String?)

    @Query("UPDATE rag_documents SET pagesDone = :done, pagesTotal = :total WHERE id = :id")
    suspend fun updateProgress(id: String, done: Int, total: Int)

    @Query("DELETE FROM rag_chunks WHERE docId = :docId")
    suspend fun deleteChunks(docId: String)
}

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun observeMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    suspend fun getAllMemories(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE source = :source")
    suspend fun deleteMemoriesBySource(source: String)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemory(id: String)

    @Query("UPDATE memories SET content = :content, embedding = :embedding, embeddingModel = :model WHERE id = :id")
    suspend fun updateContent(id: String, content: String, embedding: ByteArray?, model: String)

    @Query("UPDATE memories SET embedding = :embedding, embeddingModel = :model WHERE id = :id")
    suspend fun updateEmbedding(id: String, embedding: ByteArray, model: String)

    @Query("SELECT * FROM memories WHERE embeddingModel != :model OR embedding IS NULL LIMIT :limit")
    suspend fun memoriesToEmbed(model: String, limit: Int): List<MemoryEntity>

    @Query("SELECT COUNT(*) FROM memories WHERE embeddingModel != :model OR embedding IS NULL")
    suspend fun countMemoriesToEmbed(model: String): Int

    @Query("UPDATE memories SET useCount = useCount + 1, lastUsedAt = :now WHERE id IN (:ids)")
    suspend fun markUsed(ids: List<String>, now: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}

@Dao
interface ModeDao {

    @Query("SELECT * FROM modes ORDER BY isDefault DESC, createdAt ASC")
    fun observeModes(): Flow<List<ModeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMode(mode: ModeEntity)

    @Query("UPDATE modes SET name = :name, systemPrompt = :prompt, isActive = :isActive WHERE id = :id")
    suspend fun updateMode(id: String, name: String, prompt: String, isActive: Boolean)

    @Query("DELETE FROM modes WHERE id = :id AND isDefault = 0")
    suspend fun deleteMode(id: String)

    @Query("UPDATE modes SET defaultModelId = :modelId, defaultSpaceIds = :spaceIds, accentColor = :accentColor WHERE id = :id")
    suspend fun updatePreset(id: String, modelId: String?, spaceIds: String?, accentColor: Int?)

    @Query("SELECT * FROM modes WHERE id = :id LIMIT 1")
    suspend fun getModeById(id: String): ModeEntity?
}

@Dao
interface SpaceDao {

    @Query("SELECT * FROM spaces ORDER BY createdAt ASC")
    fun observeSpaces(): Flow<List<SpaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: SpaceEntity)

    @Query("UPDATE spaces SET name = :name WHERE id = :id")
    suspend fun renameSpace(id: String, name: String)

    @Query("DELETE FROM spaces WHERE id = :id")
    suspend fun deleteSpace(id: String)

    /** Observe documents belonging to a specific space. */
    @Query("SELECT * FROM rag_documents WHERE spaceId = :spaceId ORDER BY addedAt DESC")
    fun observeDocumentsInSpace(spaceId: String): Flow<List<RagDocumentEntity>>

    /** Delete all rag_documents for a space (chunks cascade via FK). */
    @Query("DELETE FROM rag_documents WHERE spaceId = :spaceId")
    suspend fun deleteDocumentsBySpaceId(spaceId: String)

    /** Every chunk in the given spaces with its document name, for the in-memory retrieval index. */
    @Query("""
        SELECT c.id, c.docId, c.chunkIndex, c.content, c.embedding, c.embeddingModel, d.name AS docName FROM rag_chunks c
        INNER JOIN rag_documents d ON c.docId = d.id
        WHERE d.spaceId IN (:spaceIds)
    """)
    suspend fun getChunksForSpaces(spaceIds: List<String>): List<ChunkWithDocName>
}

data class ChunkWithDocName(
    val id: String,
    val docId: String,
    val chunkIndex: Int,
    val content: String,
    val embedding: ByteArray?,
    val embeddingModel: String,
    val docName: String,
)

@Dao
interface MessageSourceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sources: List<MessageSourceEntity>)

    @Query(
        """
        SELECT s.messageId, s.rank, s.docId, s.docName, s.page, s.excerpt,
            EXISTS(SELECT 1 FROM rag_documents d WHERE d.id = s.docId) AS available
        FROM message_sources s
        JOIN messages m ON m.id = s.messageId
        WHERE m.chatId = :chatId
        ORDER BY s.messageId, s.rank
        """
    )
    fun observeForChat(chatId: String): Flow<List<MessageSourceRow>>

    @Query("SELECT uri FROM rag_documents WHERE id = :docId")
    suspend fun documentUri(docId: String): String?
}
