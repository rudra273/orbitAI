package com.example.orbitai.feature.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NetworkLogTest {

    @get:Rule val folder = TemporaryFolder()

    @Test
    fun anythingThatIsNotAHostCharacterIsDropped() {
        val file = folder.newFile()
        val log = NetworkLog(file)
        log.record("huggingface.co/secret/path?token=abc", "Model download", 123)
        val saved = file.readText()
        assertFalse(saved.contains("/"))
        assertFalse(saved.contains("?"))
        assertFalse(saved.contains("="))
        assertEquals("huggingface.cosecretpathtokenabc", log.events().single().host)
    }

    @Test
    fun cleanHostFromARealUrlIsKept() {
        val log = NetworkLog(folder.newFile())
        log.record("generativelanguage.googleapis.com", "Gemini request")
        assertEquals("generativelanguage.googleapis.com", log.events().single().host)
    }

    @Test
    fun dropsEventsOlderThanThirtyDaysAndSortsNewestFirst() {
        var now = 100L * 24 * 60 * 60 * 1000
        val log = NetworkLog(folder.newFile()) { now }
        log.record("old.example", "Update check")
        now += 31L * 24 * 60 * 60 * 1000
        log.record("a.example", "Update check")
        now += 1000
        log.record("b.example", "Report")
        assertEquals(listOf("b.example", "a.example"), log.events().map { it.host })
    }

    @Test
    fun clearEmptiesTheLog() {
        val log = NetworkLog(folder.newFile())
        log.record("a.example", "Report")
        log.clear()
        assertTrue(log.events().isEmpty())
    }
}
