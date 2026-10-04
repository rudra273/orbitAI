package com.example.orbitai.feature.modes

import com.example.orbitai.core.common.InferenceSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeJsonTest {

    @Test
    fun exportImportRoundTrip() {
        val settings = InferenceSettings(temperature = 0.3f, topK = 20, topP = 0.9f, maxDecodedTokens = 1024)
        val json = ModeJson.export("🌐 Translator", "Translate things.", settings, 0x3B82F6)
        val imported = ModeJson.import(json).getOrThrow()
        assertEquals("🌐 Translator", imported.name)
        assertEquals("Translate things.", imported.systemPrompt)
        assertEquals(settings, imported.inference)
        assertEquals(0x3B82F6, imported.accentColor)
    }

    @Test
    fun badFilesGiveClearErrors() {
        fun error(text: String) = ModeJson.import(text).exceptionOrNull()?.message
        assertEquals("This isn't an Orbit mode file.", error("not json"))
        assertEquals("This isn't an Orbit mode file.", error("""{"name":"x","systemPrompt":"y"}"""))
        assertEquals("The mode file has no name.", error("""{"format":"orbit-mode","systemPrompt":"y"}"""))
        assertEquals("The mode file has no instructions.", error("""{"format":"orbit-mode","name":"x","systemPrompt":"  "}"""))
        assertEquals("This file is too large to be an Orbit mode.", error("x".repeat(ModeJson.MAX_BYTES + 1)))
    }

    @Test
    fun valuesAreClampedAndOptionalFieldsDefault() {
        val imported = ModeJson.import("""{"format":"orbit-mode","name":"Hot","systemPrompt":"p","temperature":9,"topK":-3,"accentColor":"red"}""").getOrThrow()
        assertEquals(2f, imported.inference.temperature)
        assertEquals(1, imported.inference.topK)
        assertEquals(InferenceSettings().topP, imported.inference.topP)
        assertNull(imported.accentColor)
    }

    @Test
    fun templatesAreComplete() {
        assertEquals(7, ModeTemplates.all.size)
        ModeTemplates.all.forEach { assertTrue(it.name, it.name.isNotBlank() && it.systemPrompt.length > 40) }
    }
}
