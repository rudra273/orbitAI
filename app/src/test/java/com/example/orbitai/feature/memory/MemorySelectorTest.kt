package com.example.orbitai.feature.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorySelectorTest {

    private fun memory(id: String, content: String, source: String = "explicit", embedding: FloatArray? = null) =
        MemoryCandidate(id, content, source, embedding)

    private val profile = memory("name", "The user's name is Rudra.", source = MemoryRepository.USER_PROFILE_SOURCE)
    private val language = memory("lang", "Prefers replies in Hindi")
    private val bike = memory("bike", "Bike is parked on level 2", embedding = floatArrayOf(1f, 0f, 0f))
    private val python = memory("py", "Uses Python 3.12 with Poetry at work", embedding = floatArrayOf(0f, 1f, 0f))
    private val diet = memory("veg", "Is vegetarian", embedding = floatArrayOf(0f, 0f, 1f))
    private val all = listOf(profile, language, bike, python, diet)

    @Test
    fun profileIsAlwaysIncluded() {
        val selected = MemorySelector.select(all, "hello", null)
        assertEquals(listOf("name", "lang"), selected.map { it.id })
    }

    @Test
    fun relevantMemoriesByKeywordOrEmbedding() {
        assertEquals(listOf("name", "lang", "py"), MemorySelector.select(all, "How do I upgrade Python?", null).map { it.id })
        // No shared words, but the embedding points at the diet memory.
        assertEquals(listOf("name", "lang", "veg"), MemorySelector.select(all, "suggest a dinner recipe", floatArrayOf(0.1f, 0f, 0.9f)).map { it.id })
    }

    @Test
    fun similarityBelowThresholdIsIgnored() {
        val selected = MemorySelector.select(all, "suggest a dinner recipe", floatArrayOf(-1f, -1f, -1f))
        assertEquals(listOf("name", "lang"), selected.map { it.id })
    }

    @Test
    fun twoHundredMemoriesStayWithinTheBudget() {
        val many = listOf(profile) + (1..200).map { i ->
            memory("m$i", "Project note $i: the deadline for project item $i is Friday and the owner is teammate $i")
        }
        val selected = MemorySelector.select(many, "when is the project deadline", null)
        assertTrue(selected.first() == profile)
        assertTrue(selected.size <= 1 + MemorySelector.MAX_RELEVANT)
        val tokens = selected.sumOf { MemorySelector.estimateTokens(it.content) }
        assertTrue("used $tokens tokens", tokens <= MemorySelector.TOKEN_BUDGET)
    }
}
