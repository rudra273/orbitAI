package com.example.orbitai.feature.chat

/** A message that matched a search, with its chat and an excerpt where `[` `]` mark the matches. */
data class MessageSearchHit(
    val messageId: String,
    val chatId: String,
    val chatTitle: String,
    val snippet: String,
    val timestampMs: Long,
)

object ChatSearch {

    private const val MAX_TERMS = 8

    /**
     * Turns what the user typed into an FTS4 MATCH expression: every word must appear, and the
     * last word may be unfinished ("pine" finds "pineapple"). Quotes and operators the user typed
     * are dropped, so input can never break the query. Null when there is nothing to search for.
     */
    fun matchQuery(input: String): String? {
        val terms = Regex("""[\p{L}\p{M}\p{N}]+""").findAll(input).map { it.value.lowercase() }
            .filter { it !in operators }
            .take(MAX_TERMS)
            .toList()
        if (terms.isEmpty()) return null
        return terms.joinToString(" ") { "$it*" }
    }

    /** FTS4 keywords that would change the query's meaning if passed through. */
    private val operators = setOf("and", "or", "not", "near")
}
