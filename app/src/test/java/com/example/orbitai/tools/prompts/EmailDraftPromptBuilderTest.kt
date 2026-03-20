package com.example.orbitai.tools.prompts

import com.example.orbitai.data.Message
import com.example.orbitai.data.Role
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailDraftPromptBuilderTest {

    @Test
    fun includesExplicitRequestAndConversation() {
        val prompt = EmailDraftPromptBuilder.build(
            messages = listOf(
                Message(role = Role.USER, content = "Need to write to HR about leave tomorrow."),
                Message(role = Role.ASSISTANT, content = "I can help you draft that email."),
            ),
            topicHint = "leave request to HR",
            memories = listOf("User works at Orbit"),
        )

        assertTrue(prompt.contains("Explicit request: leave request to HR"))
        assertTrue(prompt.contains("Useful personal context:"))
        assertTrue(prompt.contains("Recent conversation:"))
        assertTrue(prompt.contains("Subject: <one line subject>"))
    }
}