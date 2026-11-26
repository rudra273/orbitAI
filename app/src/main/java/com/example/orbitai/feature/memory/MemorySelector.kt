package com.example.orbitai.feature.memory

import com.example.orbitai.feature.spaces.Retrieval

/** A memory as the selector sees it. */
data class MemoryCandidate(
    val id: String,
    val content: String,
    val source: String,
    val embedding: FloatArray?,
)

/**
 * Picks the memories to send with a message: profile facts (name, how to address the user,
 * reply language) always, then the few memories relevant to the message by keyword overlap or
 * embedding similarity, within a small token budget.
 */
object MemorySelector {

    const val MAX_RELEVANT = 5
    const val TOKEN_BUDGET = 150
    const val MIN_COSINE = 0.3f

    private val profilePrefixes = listOf("name is", "the user's name is", "prefers to be called", "prefers replies in")

    fun isProfile(memory: MemoryCandidate): Boolean =
        memory.source == MemoryRepository.USER_PROFILE_SOURCE ||
            profilePrefixes.any { memory.content.trim().lowercase().startsWith(it) }

    fun select(
        memories: List<MemoryCandidate>,
        query: String,
        queryEmbedding: FloatArray?,
        maxRelevant: Int = MAX_RELEVANT,
        tokenBudget: Int = TOKEN_BUDGET,
    ): List<MemoryCandidate> {
        val (profile, others) = memories.partition(::isProfile)

        val queryTerms = Retrieval.tokenize(query).filter { it !in Retrieval.stopWords }.toSet()
        val keywordRanking = others.indices
            .map { i -> i to Retrieval.tokenize(others[i].content).count { it in queryTerms } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
        val semanticRanking = queryEmbedding?.let(Retrieval::normalize)?.let { q ->
            others.indices
                .mapNotNull { i -> others[i].embedding?.let { i to Retrieval.dot(q, Retrieval.normalize(it)) } }
                .filter { it.second >= MIN_COSINE }
                .sortedByDescending { it.second }
                .map { it.first }
        }.orEmpty()
        val relevant = Retrieval.reciprocalRankFusion(listOf(keywordRanking, semanticRanking))
            .take(maxRelevant)
            .map { others[it] }

        var tokens = 0
        return (profile + relevant).filter { memory ->
            val cost = estimateTokens(memory.content)
            (tokens + cost <= tokenBudget).also { fits -> if (fits) tokens += cost }
        }
    }

    /** Rough token count, including the "[Memory n]" label the prompt adds. */
    fun estimateTokens(text: String): Int = text.length / 4 + 4
}
