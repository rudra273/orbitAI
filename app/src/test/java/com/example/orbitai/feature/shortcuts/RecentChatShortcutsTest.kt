package com.example.orbitai.feature.shortcuts

import com.example.orbitai.feature.chat.ChatSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentChatShortcutsTest {

    private fun chat(id: String, last: Long?, title: String = "Chat $id", archived: Boolean = false) =
        ChatSummary(id, title, "gemma", createdAt = 0, lastMessagePreview = null, lastMessageAt = last, isArchived = archived)

    @Test
    fun picksTwoNewestChatsWithMessages() {
        val chats = listOf(chat("a", 10), chat("b", 30), chat("empty", null), chat("c", 20), chat("old", 40, archived = true))
        assertEquals(listOf("b", "c"), RecentChatShortcuts.pick(chats).map { it.id })
    }

    @Test
    fun titlesAreShortAndNeverBlank() {
        assertEquals("Recent chat", RecentChatShortcuts.title(chat("a", 1, title = "New Chat")))
        assertEquals(25, RecentChatShortcuts.title(chat("a", 1, title = "x".repeat(60))).length)
    }
}
