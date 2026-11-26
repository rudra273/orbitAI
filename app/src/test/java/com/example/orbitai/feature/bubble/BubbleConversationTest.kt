package com.example.orbitai.feature.bubble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleConversationTest {

    @Test
    fun keepsTheLatestExchangesInOrder() {
        val c = BubbleConversation(maxExchanges = 3)
        (1..5).forEach { c.record("q$it", "a$it") }
        assertEquals(listOf("q3", "q4", "q5"), c.exchanges.map { it.question })
    }

    @Test
    fun blankQuestionsOrAnswersAreIgnored() {
        val c = BubbleConversation()
        c.record("hi", "  ")
        c.record(" ", "answer")
        assertTrue(c.isEmpty)
    }

    @Test
    fun withHistoryAddsEarlierTurns() {
        val c = BubbleConversation()
        assertEquals("what about tomorrow?", c.withHistory("what about tomorrow?"))
        c.record("weather today", "Sunny")
        val prompt = c.withHistory("and tomorrow?")
        assertTrue(prompt.contains("User: weather today\nAssistant: Sunny"))
        assertTrue(prompt.endsWith("User: and tomorrow?"))
    }

    @Test
    fun clearStartsOver() {
        val c = BubbleConversation()
        c.record("a", "b")
        c.clear()
        assertTrue(c.isEmpty)
    }
}
