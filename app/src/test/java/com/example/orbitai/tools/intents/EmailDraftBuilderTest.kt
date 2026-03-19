package com.example.orbitai.tools.intents

import com.example.orbitai.data.Message
import com.example.orbitai.data.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailDraftBuilderTest {

    @Test
    fun usesExplicitTopicWhenProvided() {
        val draft = EmailDraftBuilder.build(
            messages = listOf(
                Message(role = Role.USER, content = "We need to update the client on delivery dates."),
                Message(role = Role.ASSISTANT, content = "I can summarize that for you."),
            ),
            topicHint = "delivery update",
        )

        assertEquals("delivery update", draft.subject)
        assertTrue(draft.body.contains("I want to send an email about: delivery update"))
    }

    @Test
    fun fallsBackToLatestUserTopicFromChatContext() {
        val draft = EmailDraftBuilder.build(
            messages = listOf(
                Message(role = Role.USER, content = "draft mail"),
                Message(role = Role.USER, content = "Need to ask HR about interview timing"),
                Message(role = Role.ASSISTANT, content = "You can mention your availability this week."),
            ),
            topicHint = "",
        )

        assertEquals("Need to ask HR about interview timing", draft.subject)
        assertTrue(draft.body.contains("Context from OrbitAI chat:"))
    }
}