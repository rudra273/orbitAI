package com.example.orbitai.tools.intents

import com.example.orbitai.feature.automation.parser.AutomationCommandParser
import com.example.orbitai.feature.automation.parser.AutomationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutomationCommandParserTest {

    @Test
    fun parsesSlashMailCommand() {
        val result = AutomationCommandParser.parse("/mail leave request tomorrow")

        assertEquals(
            AutomationRequest.DraftEmail(topicHint = "leave request tomorrow"),
            result,
        )
    }

    @Test
    fun parsesNaturalLanguageDraftEmailCommand() {
        val result = AutomationCommandParser.parse("draft an email project update for client")

        assertEquals(
            AutomationRequest.DraftEmail(topicHint = "project update for client"),
            result,
        )
    }

    @Test
    fun ignoresNormalChatMessage() {
        val result = AutomationCommandParser.parse("tell me about android intents")

        assertNull(result)
    }

    @Test
    fun parsesWhatsAppCommand() {
        val result = AutomationCommandParser.parse("/whatsapp tell him I will be late")

        assertEquals(
            AutomationRequest.DraftWhatsApp(topicHint = "tell him I will be late"),
            result,
        )
    }

    @Test
    fun parsesReminderCommand() {
        val result = AutomationCommandParser.parse("set reminder tomorrow at 6 pm to call mom")

        assertEquals(
            AutomationRequest.CreateReminder(topicHint = "tomorrow at 6 pm to call mom"),
            result,
        )
    }
}
