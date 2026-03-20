package com.example.orbitai.tools.intents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppDraftParserTest {

    @Test
    fun parsesStructuredWhatsAppOutput() {
        val draft = WhatsAppDraftParser.parse(
            modelOutput = "Recipient: Aditya\nMessage: Hey, I will reach in 10 minutes.",
            topicHint = "",
        )

        assertEquals("Aditya", draft.recipientName)
        assertEquals("Hey, I will reach in 10 minutes.", draft.message)
    }

    @Test
    fun fallsBackToPlainTextOutput() {
        val draft = WhatsAppDraftParser.parse(
            modelOutput = "Hey, just checking if we are still meeting today.",
            topicHint = "",
        )

        assertEquals("", draft.recipientName)
        assertTrue(draft.message.contains("still meeting today"))
    }
}
