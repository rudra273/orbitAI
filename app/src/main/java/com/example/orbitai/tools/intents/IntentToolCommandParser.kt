package com.example.orbitai.tools.intents

object IntentToolCommandParser {

    private val draftEmailPatterns = listOf(
        Regex("""(?i)^/(?:mail|email|draft-mail|draft-email)\b\s*(.*)$"""),
        Regex("""(?i)^draft\s+(?:an?\s+)?(?:mail|email)\b[:\-\s]*(.*)$"""),
    )

    fun parse(input: String): IntentToolRequest? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val topicHint = draftEmailPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(trimmed)?.groupValues?.getOrNull(1)
        }?.trim()

        return if (topicHint != null) {
            IntentToolRequest.DraftEmail(topicHint = topicHint)
        } else {
            null
        }
    }
}
