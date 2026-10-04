package com.example.orbitai.feature.automation.digest

import java.util.concurrent.ConcurrentHashMap

/** One collected notification: only app, sender/title and text. Kept in memory, never written to disk. */
data class DigestItem(val key: String, val app: String, val title: String, val text: String, val postedAt: Long)

/** Keeps one-time codes and money messages out of the digest before anything is stored. */
object DigestFilter {

    private val sensitive = listOf(
        Regex("""(?i)\b(otp|one[- ]?time|verification|verify|passcode|password|passwd|cvv|pin|security code|login code|2fa)\b"""),
        Regex("""(?i)\b(do not share|don'?t share|never share)\b"""),
        Regex("""(?i)\b(debited|credited|a/c|acct|account (no|number|balance)|upi|txn|transaction id|card ending|available balance)\b"""),
        Regex("""\b\d{4,8}\b.{0,40}\b(is your|use this|do not share)\b""", RegexOption.IGNORE_CASE),
        Regex("""(ओटीपी|वेरिफ़िकेशन|पासवर्ड|पिन कोड|खाते)"""),
    )

    fun isSensitive(title: String, text: String): Boolean {
        val combined = "$title $text"
        return sensitive.any { it.containsMatchIn(combined) }
    }
}

/**
 * Notifications gathered for the next digest: newest 150, last 24 hours, one per notification key.
 * Lives only in memory and is emptied when the listener disconnects or the user clears it.
 */
class DigestBuffer(private val maxItems: Int = 150, private val clock: () -> Long = System::currentTimeMillis) {

    private val items = ConcurrentHashMap<String, DigestItem>()

    @Volatile var skippedSensitive: Int = 0
        private set

    /** Adds the notification unless it looks sensitive or empty. Returns whether it was kept. */
    fun add(item: DigestItem): Boolean {
        if (item.title.isBlank() && item.text.isBlank()) return false
        if (DigestFilter.isSensitive(item.title, item.text)) {
            skippedSensitive++
            items.remove(item.key)
            return false
        }
        items[item.key] = item
        trim()
        return true
    }

    fun snapshot(): List<DigestItem> {
        val cutoff = clock() - DAY_MS
        items.values.removeIf { it.postedAt < cutoff }
        return items.values.sortedBy { it.postedAt }
    }

    val size: Int get() = snapshot().size

    fun clear() {
        items.clear()
        skippedSensitive = 0
    }

    private fun trim() {
        if (items.size <= maxItems) return
        items.values.sortedBy { it.postedAt }.take(items.size - maxItems).forEach { items.remove(it.key) }
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000

        /** The buffer the notification listener fills and the digest screen reads. */
        val shared = DigestBuffer()
    }
}

object DigestPrompt {

    private const val MAX_CHARS = 6000

    fun build(items: List<DigestItem>): String {
        val lines = StringBuilder()
        for (item in items.asReversed()) { // newest first so the cut drops the oldest
            val line = "[${item.app}] ${item.title.trim()}: ${item.text.trim().replace(Regex("\\s+"), " ")}\n"
            if (lines.length + line.length > MAX_CHARS) break
            lines.append(line)
        }
        return "Write a short digest of these phone notifications. Group by app or person, " +
            "mention what needs a reply or action first, and keep it to a few bullet points. " +
            "Use only the notifications below and do not invent anything. " +
            "Reply in the language most of them are written in.\n\nNotifications:\n$lines"
    }
}
