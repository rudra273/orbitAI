package com.example.orbitai.core.prompt

import com.example.orbitai.core.model.AVAILABLE_MODELS
import com.example.orbitai.core.model.PromptStyle
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBudgetTest {

    private fun chat(turns: Int, words: Int = 40): List<Message> = (1..turns).map { i ->
        Message(
            role = if (i % 2 == 1) Role.USER else Role.ASSISTANT,
            content = "turn$i " + List(words) { "word" }.joinToString(" "),
        )
    }

    @Test
    fun shortChatKeepsEverything() {
        val messages = chat(4)
        val result = PromptBudget.fit(messages, listOf("doc"), listOf("memory"), "Be brief", 4096, 512)
        assertEquals(messages, result.messages)
        assertEquals(listOf("doc"), result.ragContext)
        assertEquals(listOf("memory"), result.memories)
        assertEquals(0, result.droppedMessageCount)
        assertFalse(result.latestMessageTruncated)
    }

    @Test
    fun longChatDropsOldestTurnsAndKeepsNewest() {
        val messages = chat(101)
        val result = PromptBudget.fit(messages, emptyList(), emptyList(), null, 2048, 512)
        assertTrue(result.droppedMessageCount > 0)
        assertEquals(messages.last(), result.messages.last())
        assertEquals(messages.takeLast(result.messages.size), result.messages)
        assertEquals(messages.size, result.messages.size + result.droppedMessageCount)
    }

    @Test
    fun ragContextIsKeptBeforeOlderHistory() {
        val chunk = List(300) { "fact" }.joinToString(" ")
        val result = PromptBudget.fit(chat(40), listOf(chunk), emptyList(), null, 2048, 512)
        assertEquals(listOf(chunk), result.ragContext)
        assertTrue(result.droppedMessageCount > 0)
    }

    @Test
    fun oversizedLatestMessageIsTruncatedWithNotice() {
        val huge = Message(role = Role.USER, content = "x".repeat(50_000))
        val result = PromptBudget.fit(chat(2) + huge, listOf("doc"), emptyList(), null, 1280, 512)
        assertTrue(result.latestMessageTruncated)
        assertTrue(result.messages.last().content.endsWith(PromptBudget.TRUNCATION_NOTICE))
        assertEquals(1, result.messages.size)
        assertTrue(result.ragContext.isEmpty())
    }

    @Test
    fun outputReserveLargerThanContextStillLeavesRoomForThePrompt() {
        val result = PromptBudget.fit(chat(3), emptyList(), emptyList(), null, 2048, 4096)
        assertFalse(result.latestMessageTruncated)
        assertEquals(3, result.messages.size)
    }

    @Test
    fun builtPromptPlusOutputFitsContextForEveryModelAndStyle() {
        val memories = List(50) { "Memory number $it about the user and their preferences" }
        val rag = List(5) { List(500) { "context" }.joinToString(" ") }
        for (model in AVAILABLE_MODELS) {
            for (style in PromptStyle.entries) {
                val budgeted = PromptBudget.fit(chat(200), rag, memories, "Mode prompt", model.contextTokens, 512)
                val prompt = ModelPromptBuilder.buildChatPrompt(
                    style, budgeted.messages, budgeted.ragContext, budgeted.memories, "Mode prompt",
                )
                val outputReserve = minOf(512, model.contextTokens / 2)
                assertTrue(
                    "${model.id}/$style overflows",
                    PromptBudget.estimateTokens(prompt) + outputReserve <= model.contextTokens,
                )
            }
        }
    }
}
