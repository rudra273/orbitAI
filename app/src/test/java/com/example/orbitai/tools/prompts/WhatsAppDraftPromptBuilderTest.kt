package com.example.orbitai.tools.prompts

import com.example.orbitai.data.Message
import com.example.orbitai.data.Role
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppDraftPromptBuilderTest {

    @Test
    fun includesExplicitRequestAndFormatInstructions() {
        val prompt = WhatsAppDraftPromptBuilder.build(
            messages = listOf(
                Message(role = Role.USER, content = "Need to message my friend that I will be late."),
                Message(role = Role.ASSISTANT, content = "I can help draft that."),
            ),
            topicHint = "tell him I will be late by 10 minutes",
            memories = emptyList(),
        )

        assertTrue(prompt.contains("Recipient: <contact name if the user mentioned one, otherwise leave blank>"))
        assertTrue(prompt.contains("Message: <final whatsapp message>"))
        assertTrue(prompt.contains("Explicit request: tell him I will be late by 10 minutes"))
        assertTrue(prompt.contains("Recent conversation:"))
    }
}
