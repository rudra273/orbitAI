package com.example.orbitai.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTitlesTest {

    @Test
    fun sanitizerStripsQuotesLabelsAndPunctuation() {
        assertEquals("Planning a Goa trip", ChatTitles.sanitize("\"Planning a Goa trip.\""))
        assertEquals("Kotlin coroutine basics", ChatTitles.sanitize("Title: **Kotlin coroutine basics**"))
        assertEquals("Fixing router error", ChatTitles.sanitize("\n  Fixing router error!\nThis conversation is about..."))
        assertEquals("Budget tips", ChatTitles.sanitize("“Budget tips”"))
    }

    @Test
    fun longTitlesAreCutAtAWord() {
        val title = ChatTitles.sanitize("A very long title that keeps going well beyond the forty eight character limit")!!
        assertTrue(title, title.length <= ChatTitles.MAX_LENGTH)
        assertTrue(title, !title.endsWith(" "))
        assertEquals("A very long title that keeps going well beyond", title)
    }

    @Test
    fun emptyOutputGivesNoTitle() {
        assertNull(ChatTitles.sanitize(""))
        assertNull(ChatTitles.sanitize("  \"...\"  "))
    }

    @Test
    fun statsLabel() {
        val stats = GenerationStats(firstTokenMs = 1200, streamingMs = 2000, approxTokens = 37, backend = "GPU", modelName = "Gemma 4 E2B")
        assertEquals("1.2s to first token · ≈18.5 tok/s · GPU · Gemma 4 E2B", stats.label())
        assertEquals("Gemini", GenerationStats(null, 0, 0, null, "Gemini").label())
    }
}
