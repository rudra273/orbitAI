package com.example.orbitai.feature.automation.parser

/**
 * Fixes small typos in command words ("whtsapp", "remnd", "emial") so the command parsers
 * still match. Only the command word is checked: the first word, or the word after a verb
 * such as "send" or "set a". It must be 5+ letters and one edit from a keyword (a swap of
 * two neighbouring letters counts as one edit).
 */
object CommandSpelling {

    private val keywords = listOf("whatsapp", "reminder", "remind", "remember", "email", "draft")
    private val leadVerbs = setOf("send", "write", "draft", "set", "create", "add")
    private val articles = setOf("a", "an")
    /** Real words one edit from a keyword. */
    private val realWords = setOf("rewind", "rebind", "remand", "remainder", "craft", "drift", "graft")

    fun fix(input: String): String {
        val words = Regex("""[A-Za-z']+""").findAll(input).toList()
        val first = words.firstOrNull() ?: return input
        val firstFixed = correct(first.value)
        val target = if (firstFixed.lowercase() in leadVerbs) {
            words.drop(1).firstOrNull { it.value.lowercase() !in articles }
        } else null
        val result = StringBuilder(input)
        // Replace from the end so earlier ranges stay valid.
        target?.let { result.replace(it.range.first, it.range.last + 1, correct(it.value)) }
        result.replace(first.range.first, first.range.last + 1, firstFixed)
        return result.toString()
    }

    private fun correct(word: String): String {
        val lower = word.lowercase()
        if (lower.length < 4 || lower in keywords || lower in realWords) return word
        // Leave inflected forms ("emails", "remembered") alone.
        if (keywords.any { lower.startsWith(it) }) return word
        return keywords.firstOrNull { it.length >= 5 && distance(lower, it) == 1 } ?: word
    }

    /** Optimal string alignment distance: insert, delete, substitute or swap neighbours. */
    internal fun distance(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 1) return 2
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }
}
