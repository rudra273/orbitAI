package com.example.orbitai.tools.intents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntentToolCommandParserTest {

    @Test
    fun parsesSlashMailCommand() {
        val result = IntentToolCommandParser.parse("/mail leave request tomorrow")

        assertEquals(
            IntentToolRequest.DraftEmail(topicHint = "leave request tomorrow"),
            result,
        )
    }

    @Test
    fun parsesNaturalLanguageDraftEmailCommand() {
        val result = IntentToolCommandParser.parse("draft an email project update for client")

        assertEquals(
            IntentToolRequest.DraftEmail(topicHint = "project update for client"),
            result,
        )
    }

    @Test
    fun ignoresNormalChatMessage() {
        val result = IntentToolCommandParser.parse("tell me about android intents")

        assertNull(result)
    }
}
