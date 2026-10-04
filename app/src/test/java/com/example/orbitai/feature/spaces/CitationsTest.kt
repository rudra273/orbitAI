package com.example.orbitai.feature.spaces

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CitationsTest {

    private fun chunk(id: String, content: String, doc: String = "Q3-report.pdf") = IndexChunk(id, "doc-$doc", doc, 0, content, null)

    @Test
    fun pagesComeFromPdfMarkers() {
        assertEquals(12, Citations.page("[Page 12]\nRevenue grew 8%. [Page 13]\nCosts fell."))
        assertNull(Citations.page("Plain text notes"))
    }

    @Test
    fun sourcesFollowPromptOrderAndSkipUnsentChunks() {
        val a = chunk("a", "[Page 3]\nAlpha text")
        val b = chunk("b", "Beta text", doc = "notes.md")
        val c = chunk("c", "Gamma text")
        // Only b and a were sent (budget dropped c), in that order.
        val sources = Citations.sources(listOf(a, b, c), listOf("Beta text", "[Page 3]\nAlpha text"))
        assertEquals(listOf("b", "a"), sources.map { it.chunkId })
        assertEquals(listOf(1, 2), sources.map { it.rank })
        assertEquals("1 · notes.md", Citations.label(sources[0]))
        assertEquals("2 · Q3-report.pdf · p.3", Citations.label(sources[1]))
        assertEquals("Alpha text", sources[1].excerpt)
    }

    @Test
    fun noSentChunksMeansNoSources() {
        assertTrue(Citations.sources(listOf(chunk("a", "text")), emptyList()).isEmpty())
    }

    @Test
    fun longExcerptsAreShortenedAtAWord() {
        val excerpt = Citations.excerpt("word ".repeat(400), maxLength = 50)
        assertTrue(excerpt, excerpt.length <= 51 && excerpt.endsWith("…"))
    }
}
