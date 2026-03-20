package com.example.orbitai.tools.intents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailDraftParserTest {

    @Test
    fun parsesStructuredModelOutput() {
        val draft = EmailDraftParser.parse(
            modelOutput = """
                Subject: Interview follow-up
                Body:
                Hi HR Team,

                I wanted to follow up on my interview and thank you for your time.

                Best regards,
                Rudra
            """.trimIndent(),
            topicHint = "",
        )

        assertEquals("Interview follow-up", draft.subject)
        assertTrue(draft.body.startsWith("Hi HR Team,"))
    }

    @Test
    fun fallsBackToTopicWhenSubjectMissing() {
        val draft = EmailDraftParser.parse(
            modelOutput = """
                Hi Team,

                Please find the requested update below.
            """.trimIndent(),
            topicHint = "project update",
        )

        assertEquals("project update", draft.subject)
        assertTrue(draft.body.contains("Please find the requested update below."))
    }
}
