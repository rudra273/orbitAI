package com.example.orbitai.feature.automation.executor

import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class ContactMatch(val contact: CallContact, val score: Double)

object ContactNameMatcher {
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

    fun rank(query: String, contacts: List<CallContact>): List<ContactMatch> {
        val name = normalize(query)
        if (name.isBlank()) return emptyList()
        return contacts.mapNotNull { contact ->
            val number = callableNumber(contact.number) ?: return@mapNotNull null
            val score = similarity(name, normalize(contact.name))
            if (score < 0.65) null else ContactMatch(contact.copy(number = number), score)
        }.sortedWith(compareByDescending<ContactMatch> { it.score }
            .thenBy { it.contact.name }.thenBy { it.contact.number })
            .distinctBy { normalize(it.contact.name) to it.contact.number }
    }

    fun closest(query: String, contacts: List<CallContact>): List<CallContact> {
        val matches = rank(query, contacts)
        val best = matches.firstOrNull()?.score ?: return emptyList()
        return matches.takeWhile { best - it.score < 0.12 }.take(30).map { it.contact }
    }

    private fun similarity(query: String, name: String): Double {
        if (name.isBlank()) return 0.0
        if (query == name) return 1.0
        val queryWords = query.split(' ')
        val nameWords = name.split(' ')
        val fullScore = nameSimilarity(query, name)
        // Compare a spoken first name with each saved name, and tolerate word order changes.
        if (queryWords.size == 1) {
            val wordScore = nameWords.maxOf { nameSimilarity(query, it) }
            return max(fullScore, wordScore * 0.95)
        }
        return max(fullScore, editSimilarity(queryWords.sorted().joinToString(" "), nameWords.sorted().joinToString(" ")))
    }

    private fun nameSimilarity(left: String, right: String): Double {
        val score = editSimilarity(left, right)
        val phoneticLeft = phonetic(left)
        val phoneticRight = phonetic(right)
        return if (phoneticLeft.count { it in "bcdfghjklmnpqrstvwxyz" } >= 2 && phoneticLeft == phoneticRight) max(score, 0.87) else score
    }

    private fun phonetic(value: String): String = value
        .replace("ph", "f").replace("sh", "s")
        .replace("dh", "d").replace("th", "t").replace("bh", "b")
        .replace(Regex("[aeiou]+"), "a")
        .replace(Regex("(.)\\1+"), "$1")

    private fun editSimilarity(left: String, right: String): Double {
        if (left == right) return 1.0
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val distance = Array(left.length + 1) { IntArray(right.length + 1) }
        for (i in 0..left.length) distance[i][0] = i
        for (j in 0..right.length) distance[0][j] = j
        for (i in 1..left.length) for (j in 1..right.length) {
            distance[i][j] = minOf(
                distance[i - 1][j] + 1, distance[i][j - 1] + 1,
                distance[i - 1][j - 1] + if (left[i - 1] == right[j - 1]) 0 else 1,
            )
            if (i > 1 && j > 1 && left[i - 1] == right[j - 2] && left[i - 2] == right[j - 1]) {
                distance[i][j] = min(distance[i][j], distance[i - 2][j - 2] + 1)
            }
        }
        return 1.0 - distance[left.length][right.length].toDouble() / max(left.length, right.length)
    }
}
