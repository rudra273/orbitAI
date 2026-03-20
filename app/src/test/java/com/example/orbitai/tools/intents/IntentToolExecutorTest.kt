package com.example.orbitai.tools.intents

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentToolExecutorTest {

    @Test
    fun parserStillRecognizesDraftEmailCommand() {
        val result = IntentToolCommandParser.parse("draft email job application follow up")

        assertEquals(
            IntentToolRequest.DraftEmail(topicHint = "job application follow up"),
            result,
        )
    }
}