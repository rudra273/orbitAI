package com.example.orbitai.feature.automation.device

import com.example.orbitai.feature.automation.parser.CommandSpelling

/** Picks the installed app whose name best matches what the user said. */
object AppMatcher {

    /** Index of the best match in [labels], or null if nothing is close enough. */
    fun best(query: String, labels: List<String>): Int? {
        val q = normalize(query)
        if (q.isEmpty()) return null
        var bestIndex: Int? = null
        var bestScore = 0
        labels.forEachIndexed { index, label ->
            val l = normalize(label)
            val score = when {
                l == q -> 100
                l.replace(" ", "") == q.replace(" ", "") -> 95
                l.startsWith("$q ") || l.startsWith(q) && q.length >= 3 -> 80
                l.split(" ").any { it == q } -> 70
                q.length >= 4 && l.contains(q) -> 60
                q.length >= 5 && !q.contains(' ') && l.split(" ").any { CommandSpelling.distance(it, q) == 1 } -> 50
                else -> 0
            }
            // Prefer the shorter label on ties ("Maps" over "Maps Go").
            if (score > bestScore || score == bestScore && score > 0 && bestIndex != null && label.length < labels[bestIndex!!].length) {
                bestScore = score
                bestIndex = index
            }
        }
        return bestIndex.takeIf { bestScore > 0 }
    }

    private fun normalize(text: String): String =
        text.lowercase().replace(Regex("""[^\p{L}\p{M}\p{N}]+"""), " ").trim()
}
