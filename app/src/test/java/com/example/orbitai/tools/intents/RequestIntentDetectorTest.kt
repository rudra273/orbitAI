package com.example.orbitai.tools.intents

import com.example.orbitai.feature.automation.parser.AutomationCommandParser
import com.example.orbitai.feature.automation.parser.AutomationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestIntentDetectorTest {

    private fun parse(text: String) = AutomationCommandParser.parse(text)

    @Test
    fun whatsAppRequestsAnywhereInTheSentence() {
        listOf(
            "send hi to aditya on whatsapp",
            "send a msg to aditya on whtsapp saying I'm late",
            "can you message Aditya on WhatsApp that the meeting moved",
            "please text mom on whats app good night",
            "tell aditya on watsapp to call me",
            "Hey Orbit, let Priya know on whatsapp I'm outside",
        ).forEach { assertTrue(it, parse(it) is AutomationRequest.DraftWhatsApp) }
        assertEquals(AutomationRequest.DraftWhatsApp("send hi to aditya on whatsapp"), parse("send hi to aditya on whatsapp"))
    }

    @Test
    fun emailAndReminderRequests() {
        assertTrue(parse("can you write an email to HR asking for leave on Friday") is AutomationRequest.DraftEmail)
        assertTrue(parse("please send a mail to Ravi about the invoice") is AutomationRequest.DraftEmail)
        assertEquals(AutomationRequest.CreateReminder("at 5 pm to call mom"), parse("can you please remind me at 5 pm to call mom"))
        assertEquals(AutomationRequest.CreateReminder("tomorrow pay rent"), parse("tomorrow remnd me to pay rent"))
        assertEquals(AutomationRequest.CreateReminder("tomorrow 7 am gym"), parse("tomorrow set a remnder for 7 am gym"))
    }

    @Test
    fun questionsAndChatAreLeftToTheModel() {
        listOf(
            "how do I send a whatsapp message from a laptop",
            "what is whatsapp",
            "is it safe to send money on whatsapp?",
            "tell me about whatsapp privacy",
            "whatsup bro",
            "you remind me of my brother",
            "set the remainder of the budget aside",
            "write a poem about email",
            "explain how to write a formal email",
            "emails are piling up, any tips",
        ).forEach { assertNull(it, parse(it)) }
    }
}
