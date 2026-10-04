package com.example.orbitai.tools.intents

import com.example.orbitai.feature.automation.parser.AutomationCommandParser
import com.example.orbitai.feature.automation.parser.AutomationRequest
import com.example.orbitai.feature.automation.parser.CommandSpelling
import com.example.orbitai.feature.automation.parser.DeviceCommand
import com.example.orbitai.feature.automation.parser.DeviceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommandSpellingTest {

    @Test
    fun misspelledWhatsAppStillRoutes() {
        listOf("whtsapp aditya say hi", "watsapp aditya say hi", "Whatsap aditya say hi", "whatsapp aditya say hi").forEach {
            assertEquals(it, AutomationRequest.DraftWhatsApp("aditya say hi"), AutomationCommandParser.parse(it))
        }
        assertEquals(
            AutomationRequest.DraftWhatsApp("to Mom I'm late"),
            AutomationCommandParser.parse("send a whtasapp to Mom I'm late"),
        )
    }

    @Test
    fun misspelledReminderEmailAndRememberRoute() {
        assertEquals(AutomationRequest.CreateReminder("call the bank at 6 pm"), AutomationCommandParser.parse("remnd me to call the bank at 6 pm"))
        assertEquals(AutomationRequest.CreateReminder("tomorrow 9 am standup"), AutomationCommandParser.parse("set a remnider tomorrow 9 am standup"))
        assertEquals(AutomationRequest.DraftEmail("to Priya about the report"), AutomationCommandParser.parse("draft an emial to Priya about the report"))
        assertEquals(DeviceCommand.Remember("my bike is on level 2"), DeviceCommandParser.parse("remmber my bike is on level 2"))
    }

    @Test
    fun normalWordsAreLeftAlone() {
        listOf("whatsup bro", "emails are slow today", "remembered the milk?", "remained calm", "rewind the story", "craft a poem")
            .forEach { assertEquals(it, CommandSpelling.fix(it)) }
        assertNull(AutomationCommandParser.parse("whatsup bro"))
        assertNull(AutomationCommandParser.parse("what is the best reminder app"))
    }

    @Test
    fun onlyTheCommandWordIsChanged() {
        assertEquals("whatsapp Aditya: remnd him later", CommandSpelling.fix("whtsapp Aditya: remnd him later"))
        assertEquals("send a whatsapp to Remnd", CommandSpelling.fix("send a whtsapp to Remnd"))
        assertEquals("tell Aditya to emial me", CommandSpelling.fix("tell Aditya to emial me"))
    }
}
