package com.example.orbitai.feature.chat

/** Short generated chat titles. */
object ChatTitles {

    const val MAX_LENGTH = 48

    fun prompt(firstUserMessage: String, firstReply: String): String = """
        Write a short title (3 to 6 words) for this conversation. Reply with the title only, no quotes.

        User: ${firstUserMessage.take(500)}
        Assistant: ${firstReply.take(500)}
    """.trimIndent()

    /** Cleans model output into a title, or null if nothing usable is left. */
    fun sanitize(raw: String): String? {
        val firstLine = raw.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return null
        var title = firstLine
            .replace(Regex("""(?i)^(?:title|chat title)\s*[:\-]\s*"""), "")
            .replace(Regex("""[*_`#]+"""), "")
            .trim()
            .trim('"', '\'', '“', '”', '‘', '’', '«', '»')
            .trimEnd('.', '!', '?', ':', ';', ',', '…')
            .replace(Regex("\\s+"), " ")
            .trim()
        if (title.length > MAX_LENGTH) {
            title = title.take(MAX_LENGTH).substringBeforeLast(' ').ifBlank { title.take(MAX_LENGTH) }.trimEnd(',', ';', ':', '-')
        }
        return title.takeIf { it.any(Char::isLetterOrDigit) }
    }
}
