package com.example.orbitai.feature.textactions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextActionPromptsTest {

    @Test
    fun everyActionIncludesTheText() {
        TextAction.entries.forEach { action ->
            val prompt = TextActionPrompts.instruction(action, "hello wrld", "Hindi")
            assertTrue(action.name, prompt.endsWith("Text:\nhello wrld"))
        }
        assertTrue(TextActionPrompts.instruction(TextAction.TRANSLATE, "x", "Hindi").contains("into Hindi"))
    }

    @Test
    fun longSelectionsAreCapped() {
        val prompt = TextActionPrompts.instruction(TextAction.SUMMARIZE, "a".repeat(20_000), "English")
        assertTrue(prompt.length < TextActionPrompts.MAX_INPUT_CHARS + 200)
    }

    @Test
    fun resultLabelsAndQuotesAreRemoved() {
        assertEquals("Hello world.", TextActionPrompts.cleanResult("Here is the corrected text:\n\"Hello world.\""))
        assertEquals("नमस्ते दुनिया", TextActionPrompts.cleanResult("Translation: नमस्ते दुनिया"))
        assertEquals("Keep: this colon", TextActionPrompts.cleanResult("Keep: this colon"))
    }
}
