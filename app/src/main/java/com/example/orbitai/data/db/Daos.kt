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

