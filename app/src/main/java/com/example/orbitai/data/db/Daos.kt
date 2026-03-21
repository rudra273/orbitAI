package com.example.orbitai.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Transaction
    @Query("SELECT * FROM chats ORDER BY createdAt DESC")
    fun observeAllChatsWithMessages(): Flow<List<ChatWithMessages>>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query(
        """
        SELECT * FROM chats c
        WHERE NOT EXISTS (
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
}

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestampMs ASC")
    suspend fun getMessages(chatId: String): List<MessageEntity>

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

    @Query("SELECT * FROM rag_chunks WHERE embedding IS NOT NULL")
    suspend fun getAllChunksWithEmbeddings(): List<RagChunkEntity>
}

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun observeMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    suspend fun getAllMemories(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemory(id: String)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}

@Dao
interface ModeDao {

    @Query("SELECT * FROM modes ORDER BY isDefault DESC, createdAt ASC")
    fun observeModes(): Flow<List<ModeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMode(mode: ModeEntity)

    @Query("UPDATE modes SET name = :name, systemPrompt = :prompt WHERE id = :id")
    suspend fun updateMode(id: String, name: String, prompt: String)

    @Query("DELETE FROM modes WHERE id = :id AND isDefault = 0")
    suspend fun deleteMode(id: String)

    @Query("SELECT * FROM modes WHERE id = :id LIMIT 1")
    suspend fun getModeById(id: String): ModeEntity?
}

@Dao
interface SpaceDao {

    @Query("SELECT * FROM spaces ORDER BY createdAt ASC")
    fun observeSpaces(): Flow<List<SpaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpace(space: SpaceEntity)

    @Query("DELETE FROM spaces WHERE id = :id")
    suspend fun deleteSpace(id: String)

    /** Observe documents belonging to a specific space. */
    @Query("SELECT * FROM rag_documents WHERE spaceId = :spaceId ORDER BY addedAt DESC")
    fun observeDocumentsInSpace(spaceId: String): Flow<List<RagDocumentEntity>>

    /** Delete all rag_documents for a space (chunks cascade via FK). */
    @Query("DELETE FROM rag_documents WHERE spaceId = :spaceId")
    suspend fun deleteDocumentsBySpaceId(spaceId: String)

    /** All embedded chunks whose parent document belongs to one of the given spaces. */
    @Query("""
        SELECT c.* FROM rag_chunks c
        INNER JOIN rag_documents d ON c.docId = d.id
        WHERE d.spaceId IN (:spaceIds) AND c.embedding IS NOT NULL
    """)
    suspend fun getAllChunksWithEmbeddingsForSpaces(spaceIds: List<String>): List<RagChunkEntity>

    /** Keyword search in chunks across the given spaces. */
    @Query("""
        SELECT c.* FROM rag_chunks c
        INNER JOIN rag_documents d ON c.docId = d.id
        WHERE d.spaceId IN (:spaceIds) AND c.content LIKE '%' || :query || '%'
        LIMIT :limit
    """)
    suspend fun searchChunksInSpaces(spaceIds: List<String>, query: String, limit: Int = 20): List<RagChunkEntity>

    /** Keyword search by document name across the given spaces. */
    @Query("""
        SELECT c.* FROM rag_chunks c
        INNER JOIN rag_documents d ON c.docId = d.id
        WHERE d.spaceId IN (:spaceIds) AND d.name LIKE '%' || :query || '%'
        ORDER BY c.chunkIndex ASC
        LIMIT :limit
    """)
    suspend fun searchChunksByDocNameInSpaces(spaceIds: List<String>, query: String, limit: Int = 20): List<RagChunkEntity>
}

