package com.example.orbitai.tools.intents

import com.example.orbitai.feature.automation.executor.CallContact
import com.example.orbitai.feature.automation.parser.CallConversation
import com.example.orbitai.feature.automation.parser.CallReply
import org.junit.Assert.*
import org.junit.Test

class CallConversationTest {
    private val rahul = CallContact("Rahul Sharma", "9876543210")
    private val ravi = CallContact("Ravi Sharma", "9876543211")
    private val contacts = listOf(rahul, ravi)

    @Test fun yesRequiresASelectedContact() {
        assertEquals(CallReply.Repeat, CallConversation.reply("yes", contacts, null))
        assertEquals(CallReply.Confirm, CallConversation.reply("Yes, please!", contacts, rahul))
        assertEquals(CallReply.Repeat, CallConversation.reply("yes but don't call yet", contacts, rahul))
        assertEquals(CallReply.Repeat, CallConversation.reply("yes", contacts, CallContact("Unknown", "9876543212")))
    }

    @Test fun choosingANameOrOptionRequiresAnotherConfirmation() {
        assertEquals(CallReply.Select(ravi), CallConversation.reply("the second one", contacts, null))
        assertEquals(CallReply.Select(rahul), CallConversation.reply("Rhaul", contacts, null))
        assertEquals(CallReply.Repeat, CallConversation.reply("option three", contacts, null))
        val fourth = CallContact("Priya", "9876543214")
        assertEquals(CallReply.Select(fourth), CallConversation.reply("option 4", contacts + rahul.copy(number = "9876543213") + fourth, null))
    }

    @Test fun noCancelsAndDialerNeedsASelectedNumber() {
        assertEquals(CallReply.Cancel, CallConversation.reply("No thanks", contacts, rahul))
        assertEquals(CallReply.Cancel, CallConversation.reply("cancel", contacts, null))
        assertEquals(CallReply.OpenDialer, CallConversation.reply("open the dialer", contacts, rahul))
        assertEquals(CallReply.Repeat, CallConversation.reply("open dialer", contacts, null))
    }

    @Test fun sameNameNumbersAreSpokenSeparately() {
        val work = rahul.copy(number = "9876541234")
        val choices = listOf(rahul, work)
        val question = CallConversation.question(choices, null)
        assertTrue(question.contains("3 2 1 0"))
        assertTrue(question.contains("1 2 3 4"))
        assertEquals(CallReply.Repeat, CallConversation.reply("Rahul", choices, null))
        assertEquals(CallReply.Select(work), CallConversation.reply("two", choices, null))
        assertTrue(CallConversation.question(choices, work).contains("1 2 3 4"))
    }
}
