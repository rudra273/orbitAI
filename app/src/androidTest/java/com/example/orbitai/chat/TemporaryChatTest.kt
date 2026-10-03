package com.example.orbitai.chat

import androidx.test.platform.app.InstrumentationRegistry
import com.example.orbitai.core.database.AppDatabase
import com.example.orbitai.feature.chat.ChatRepository
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporaryChatTest {

    @Test
    fun temporaryChatsNeverReachTheDatabase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getInstance(context).openHelper.readableDatabase
        fun count(table: String) = db.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }
        val before = listOf("chats", "messages", "memories").associateWith(::count)

        val repo = ChatRepository(context)
        val chat = repo.createTemporaryChat()
        assertTrue(ChatRepository.isTemporaryChat(chat.id))
        val question = Message(role = Role.USER, content = "My name is Test and I live in Pune")
        val reply = Message(role = Role.ASSISTANT, content = "", isStreaming = true)
        repo.addMessage(chat.id, question)
        repo.addMessage(chat.id, reply)
        repo.updateMessage(reply.id, "Hello", isStreaming = false)
        assertEquals(listOf("My name is Test and I live in Pune", "Hello"), repo.getChat(chat.id)!!.messages.map { it.content })

        assertEquals(before, listOf("chats", "messages", "memories").associateWith(::count))

        repo.discardTemporaryChat(chat.id)
        assertNull(repo.getChat(chat.id))
    }
}
