package com.example.orbitai.feature.spaces

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RetrievalIndexTest {

    private fun chunk(id: String, content: String, embedding: FloatArray? = null, doc: String = "notes.pdf") =
        IndexChunk(id, "doc-$doc", doc, 0, content, embedding)

    private val chunks = listOf(
        chunk("router", "The router error code ERR-4012 means the firmware update failed. Restart and retry.", floatArrayOf(1f, 0f, 0f, 0f)),
        chunk("leave", "Employees get 24 days of paid leave per year. Unused leave carries over.", floatArrayOf(0f, 1f, 0f, 0f)),
        chunk("travel", "Travel claims must be filed within 30 days with receipts attached.", floatArrayOf(0f, 0f, 1f, 0f)),
    )
    private val index = RetrievalIndex(chunks)

    @Test
    fun exactTermsWinEvenWithUnrelatedEmbedding() {
        // The query vector points at "leave", but the exact error code must still come first.
        val results = index.search("what does ERR-4012 mean", floatArrayOf(0f, 1f, 0f, 0f), limit = 2)
        assertEquals("router", results.first().id)
    }

    @Test
    fun semanticMatchWorksWithoutSharedWords() {
        val results = index.search("vacation allowance", floatArrayOf(0.1f, 0.95f, 0f, 0f), limit = 1)
        assertEquals(listOf("leave"), results.map { it.id })
    }

    @Test
    fun smallTalkInjectsNothing() {
        listOf("hi", "hello, how are you?", "thanks!", "ok good morning").forEach { query ->
            assertTrue(query, index.search(query, lowSimilarity(), limit = 5).isEmpty())
        }
    }

    @Test
    fun documentNameMatches() {
        val named = RetrievalIndex(listOf(chunk("a", "Quarterly numbers look fine.", doc = "budget-2026.pdf"), chunk("b", "Unrelated text.")))
        assertEquals("a", named.search("budget", null, limit = 1).single().id)
    }

    @Test
    fun reciprocalRankFusionRewardsAgreement() {
        // Item 2 is second in both lists, so it beats items that top only one list.
        assertEquals(2, Retrieval.reciprocalRankFusion(listOf(listOf(1, 2, 3), listOf(4, 2, 5))).first())
        assertEquals(listOf(7, 8), Retrieval.reciprocalRankFusion(listOf(listOf(7, 8), emptyList())))
    }

    @Test
    fun dedupeDropsRepeatedChunks() {
        val text = "Employees get 24 days of paid leave per year and unused leave carries over to next year"
        val ranked = listOf(chunk("1", text), chunk("2", "$text."), chunk("3", "Travel claims need receipts within 30 days"))
        assertEquals(listOf("1", "3"), Retrieval.dedupe(ranked, limit = 5).map { it.id })
        assertEquals(listOf("1"), Retrieval.dedupe(ranked, limit = 1).map { it.id })
    }

    @Test
    fun fiveThousandChunksSearchQuickly() {
        val random = Random(7)
        val vocabulary = (1..3000).map { "word$it" }
        val big = RetrievalIndex((1..5000).map { i ->
            chunk("c$i", (1..300).joinToString(" ") { vocabulary[random.nextInt(vocabulary.size)] }, FloatArray(100) { random.nextFloat() - 0.5f })
        })
        val query = FloatArray(100) { random.nextFloat() - 0.5f }
        repeat(3) { big.search("word12 word400 word2999", query, limit = 5) } // warm-up
        val started = System.nanoTime()
        val runs = 20
        repeat(runs) { big.search("word12 word400 word2999", query, limit = 5) }
        val perQueryMs = (System.nanoTime() - started) / 1_000_000.0 / runs
        println("RetrievalIndex: 5000 chunks, %.1f ms per query (JVM)".format(perQueryMs))
        assertTrue("took $perQueryMs ms", perQueryMs < 150)
    }

    /** A query vector below the similarity threshold for every test chunk. */
    private fun lowSimilarity() = floatArrayOf(0.1f, 0.1f, 0.1f, 1f)
}
