package com.example.orbitai.feature.spaces

import kotlin.math.ln
import kotlin.math.sqrt

/** A chunk as the index sees it: text, owning document and an optional embedding. */
data class IndexChunk(
    val id: String,
    val docId: String,
    val docName: String,
    val chunkIndex: Int,
    val content: String,
    val embedding: FloatArray?,
)

/**
 * In-memory hybrid index over the chunks of the active Spaces: BM25 keyword ranking plus
 * cosine similarity, merged with reciprocal-rank fusion. Built once per Space set and reused
 * until documents change.
 */
class RetrievalIndex(private val chunks: List<IndexChunk>) {

    private val termCounts: List<Map<String, Int>> = chunks.map { chunk ->
        Retrieval.tokenize(chunk.content + " " + chunk.docName).groupingBy { it }.eachCount()
    }
    private val lengths = termCounts.map { it.values.sum() }
    private val averageLength = lengths.average().takeIf { it > 0 } ?: 1.0
    private val documentFrequency: Map<String, Int> = HashMap<String, Int>().apply {
        termCounts.forEach { counts -> counts.keys.forEach { merge(it, 1, Int::plus) } }
    }
    private val unitVectors: List<FloatArray?> = chunks.map { it.embedding?.let(Retrieval::normalize) }

    val size: Int get() = chunks.size

    /**
     * Returns up to [limit] relevant chunks. A chunk qualifies through a keyword match or a cosine
     * similarity of at least [minCosine]; with neither (small talk), nothing is returned.
     */
    fun search(query: String, queryEmbedding: FloatArray?, limit: Int, minCosine: Float = Retrieval.MIN_COSINE): List<IndexChunk> {
        val keywordRanking = bm25(Retrieval.tokenize(query).filter { it !in Retrieval.stopWords }.distinct())
        val semanticRanking = queryEmbedding?.let(Retrieval::normalize)?.let { q ->
            unitVectors.indices.mapNotNull { i -> unitVectors[i]?.let { i to Retrieval.dot(q, it) } }
                .filter { it.second >= minCosine }
                .sortedByDescending { it.second }
                .take(CANDIDATES)
                .map { it.first }
        }.orEmpty()
        val fused = Retrieval.reciprocalRankFusion(listOf(keywordRanking, semanticRanking))
        return Retrieval.dedupe(fused.map { chunks[it] }, limit)
    }

    private fun bm25(terms: List<String>): List<Int> {
        if (terms.isEmpty()) return emptyList()
        val n = chunks.size.toDouble()
        val idf = terms.associateWith { term ->
            val df = documentFrequency[term] ?: 0
            ln(1 + (n - df + 0.5) / (df + 0.5))
        }
        return termCounts.indices.mapNotNull { i ->
            var score = 0.0
            for (term in terms) {
                val tf = termCounts[i][term] ?: continue
                score += idf.getValue(term) * tf * (K1 + 1) / (tf + K1 * (1 - B + B * lengths[i] / averageLength))
            }
            if (score > 0) i to score else null
        }.sortedByDescending { it.second }.take(CANDIDATES).map { it.first }
    }

    private companion object {
        const val K1 = 1.2
        const val B = 0.75
        const val CANDIDATES = 50
    }
}

object Retrieval {

    /** Universal Sentence Encoder scores unrelated text around 0–0.2. */
    const val MIN_COSINE = 0.3f
    /** Chunks sharing more than this share of words with a better-ranked chunk are dropped. */
    const val DUPLICATE_OVERLAP = 0.6

    val stopWords = setOf(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "shall", "should",
        "may", "might", "must", "can", "could", "i", "me", "my", "mine", "we", "our",
        "you", "your", "he", "him", "his", "she", "her", "it", "its", "they", "them",
        "their", "this", "that", "these", "those", "what", "which", "who", "whom",
        "how", "when", "where", "why", "of", "in", "to", "for", "with", "on", "at",
        "from", "by", "about", "as", "into", "through", "during", "before", "after",
        "and", "but", "or", "nor", "not", "so", "if", "then", "than", "too", "very",
        "just", "also", "tell", "give", "show", "get", "find", "know", "let", "make",
        "hi", "hello", "hey", "thanks", "thank", "ok", "okay", "please", "yes", "no",
        "good", "morning", "night", "doing", "up", "there", "here", "some", "any",
    )

    fun tokenize(text: String): List<String> =
        text.lowercase().split(Regex("""[^\p{L}\p{M}\p{N}]+""")).filter { it.length > 1 || it.firstOrNull()?.isDigit() == true }

    /** Merges rankings (lists of item indices, best first); items found by several rankings rise. */
    fun reciprocalRankFusion(rankings: List<List<Int>>, k: Int = 60): List<Int> {
        val scores = HashMap<Int, Double>()
        rankings.forEach { ranking -> ranking.forEachIndexed { rank, item -> scores.merge(item, 1.0 / (k + rank + 1), Double::plus) } }
        return scores.entries.sortedByDescending { it.value }.map { it.key }
    }

    /** Keeps the first [limit] chunks, skipping ones that mostly repeat an already kept chunk. */
    fun dedupe(ranked: List<IndexChunk>, limit: Int): List<IndexChunk> {
        val kept = mutableListOf<Pair<IndexChunk, Set<String>>>()
        for (chunk in ranked) {
            if (kept.size >= limit) break
            val words = tokenize(chunk.content).filterTo(HashSet()) { it !in stopWords }
            val duplicate = kept.any { (_, other) ->
                val smaller = minOf(words.size, other.size)
                smaller > 0 && words.intersect(other).size.toDouble() / smaller > DUPLICATE_OVERLAP
            }
            if (!duplicate) kept += chunk to words
        }
        return kept.map { it.first }
    }

    fun normalize(v: FloatArray): FloatArray {
        var norm = 0f
        for (x in v) norm += x * x
        if (norm == 0f) return v
        val inv = 1f / sqrt(norm)
        return FloatArray(v.size) { v[it] * inv }
    }

    fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in 0 until minOf(a.size, b.size)) sum += a[i] * b[i]
        return sum
    }
}
