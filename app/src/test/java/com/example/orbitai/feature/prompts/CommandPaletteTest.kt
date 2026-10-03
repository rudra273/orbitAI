package com.example.orbitai.feature.prompts

import com.example.orbitai.feature.automation.OrbitCommands
import com.example.orbitai.feature.automation.parser.AutomationCommandParser
import com.example.orbitai.feature.automation.parser.AutomationRequest
import com.example.orbitai.feature.automation.parser.DeviceCommand
import com.example.orbitai.feature.automation.parser.DeviceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandPaletteTest {

    @Test fun everyRegistryNameAndAliasIsUnderstoodByTheParsers() {
        for (cmd in OrbitCommands.all) {
            for (name in listOf(cmd.name) + cmd.aliases) {
                val input = "/$name something"
                val parsed: Any? = AutomationCommandParser.parse(input) ?: DeviceCommandParser.parse(input)
                val ok = when (cmd.name) {
                    "mail" -> parsed is AutomationRequest.DraftEmail
                    "whatsapp" -> parsed is AutomationRequest.DraftWhatsApp
                    "remind" -> parsed is AutomationRequest.CreateReminder
                    "call" -> parsed is DeviceCommand.Call
                    "remember" -> parsed is DeviceCommand.Remember
                    else -> false
                }
                assertTrue("/$name not parsed as ${cmd.name}: $parsed", ok)
            }
        }
    }

    @Test fun menuOpensOnlyForALoneSlashWord() {
        assertEquals("", CommandPalette.query("/"))
        assertEquals("ma", CommandPalette.query("/ma"))
        assertNull(CommandPalette.query("/mail hi"))
        assertNull(CommandPalette.query("hello"))
    }

    @Test fun filtersCommandsAndSavedPrompts() {
        val prompts = listOf(SavedPrompt("1", "Summarize", "Summarize in 3 bullets: {{input}}"), SavedPrompt("2", "Hindi", "Translate to Hindi"))
        assertEquals(OrbitCommands.all.size + 2, CommandPalette.items("", prompts).size)
        assertEquals(listOf("mail"), CommandPalette.items("ma", prompts).filterIsInstance<PaletteItem.Command>().map { it.command.name })
        assertEquals(listOf("whatsapp"), CommandPalette.items("wa", prompts).filterIsInstance<PaletteItem.Command>().map { it.command.name })
        assertEquals(listOf("1"), CommandPalette.items("summ", prompts).filterIsInstance<PaletteItem.Prompt>().map { it.prompt.id })
    }

    @Test fun insertionPlacesCursorAtPlaceholder() {
        assertEquals(Insertion("/mail ", 6), CommandPalette.insertion(PaletteItem.Command(OrbitCommands.byName("mail"))))
        assertEquals(
            Insertion("Translate  to Hindi", 10),
            CommandPalette.insertion(PaletteItem.Prompt(SavedPrompt("x", "t", "Translate {{input}} to Hindi"))),
        )
        assertEquals(Insertion("Explain simply", 14), CommandPalette.insertion(PaletteItem.Prompt(SavedPrompt("y", "t", "Explain simply"))))
    }
}
