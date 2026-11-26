package com.example.orbitai.core.engine

import com.example.orbitai.core.prompt.AiSafetyPolicy
import com.example.orbitai.core.prompt.ModelPromptBuilder
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import com.google.genai.types.Content
import com.google.genai.types.Part
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiContentsTest {

    private fun Content.roleName() = role().get()
    private fun Content.texts() = parts().get().mapNotNull { it.text().orElse(null) }

    @Test
    fun chatHistoryBecomesRealTurnsWithoutGemmaTokens() {
        val messages = listOf(
            Message(role = Role.USER, content = "What is Kotlin?"),
            Message(role = Role.ASSISTANT, content = "A JVM language."),
            Message(role = Role.USER, content = "Who makes it?"),
        )
        val structured = ModelPromptBuilder.buildStructuredChat(
            messages, ragContext = listOf("Doc text"), memories = listOf("Likes Android"), systemPrompt = "Be brief",
        )
        val contents = buildGeminiContents("unused", structured, emptyList())

        assertEquals(listOf("user", "model", "user"), contents.map { it.roleName() })
        assertEquals(listOf("What is Kotlin?", "A JVM language.", "Who makes it?"), contents.flatMap { it.texts() })
        contents.flatMap { it.texts() }.forEach {
            assertFalse(it.contains("<start_of_turn>"))
            assertFalse(it.contains("<end_of_turn>"))
        }
    }

    @Test
    fun systemInstructionCarriesSafetyModePromptMemoriesAndDocuments() {
        val structured = ModelPromptBuilder.buildStructuredChat(
            listOf(Message(role = Role.USER, content = "Hi")),
            ragContext = listOf("Doc text"), memories = listOf("Likes Android"), systemPrompt = "Be brief",
        )
        val system = structured.systemInstruction
        assertTrue(system.startsWith(AiSafetyPolicy.instruction))
        assertTrue(system.contains("Be brief"))
        assertTrue(system.contains("Likes Android"))
        assertTrue(system.contains("Doc text"))
        assertFalse(system.contains("<start_of_turn>"))
    }

    @Test
    fun imagesAttachToTheLastUserTurn() {
        val image = Part.fromBytes(byteArrayOf(1, 2, 3), "image/jpeg")
        val structured = StructuredPrompt(
            "system",
            listOf(PromptTurn(true, "first"), PromptTurn(false, "reply"), PromptTurn(true, "what is this?")),
        )
        val contents = buildGeminiContents("unused", structured, listOf(image))
        assertEquals(1, contents[0].parts().get().size)
        assertEquals(2, contents[2].parts().get().size)
        assertTrue(contents[2].parts().get().first().inlineData().isPresent)
    }

    @Test
    fun leadingModelTurnsAndEmptyTurnsAreDroppedAndSameRoleTurnsMerged() {
        val structured = StructuredPrompt(
            "system",
            listOf(
                PromptTurn(false, "orphan reply"),
                PromptTurn(true, "a"),
                PromptTurn(true, "b"),
                PromptTurn(false, ""),
                PromptTurn(false, "answer"),
            ),
        )
        val contents = buildGeminiContents("unused", structured, emptyList())
        assertEquals(listOf("user", "model"), contents.map { it.roleName() })
        assertEquals("a\n\nb", contents[0].texts().single())
    }

    @Test
    fun withoutStructuredInputThePromptIsOneUserTurn() {
        val contents = buildGeminiContents("plain prompt", null, emptyList())
        assertEquals(listOf("user"), contents.map { it.roleName() })
        assertEquals("plain prompt", contents.single().texts().single())
    }
}
