package com.example.orbitai.tools.intents

object IntentToolCommandParser {

    private val draftEmailPatterns = listOf(
        Regex("""(?i)^/(?:mail|email|draft-mail|draft-email)\b\s*(.*)$"""),
        Regex("""(?i)^draft\s+(?:an?\s+)?(?:mail|email)\b[:\-\s]*(.*)$"""),
    )
    private val draftWhatsAppPatterns = listOf(
        Regex("""(?i)^/(?:whatsapp|wa)\b\s*(.*)$"""),
        Regex("""(?i)^(?:draft|write|send)\s+(?:a\s+)?(?:whatsapp|wa)(?:\s+message)?\b[:\-\s]*(.*)$"""),
        Regex("""(?i)^whatsapp\b[:\-\s]*(.*)$"""),
    )

    fun parse(input: String): IntentToolRequest? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val topicHint = draftEmailPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(trimmed)?.groupValues?.getOrNull(1)
        }?.trim()

        if (topicHint != null) {
            return IntentToolRequest.DraftEmail(topicHint = topicHint)
        }

        val whatsAppHint = draftWhatsAppPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(trimmed)?.groupValues?.getOrNull(1)
        }?.trim()

        return if (whatsAppHint != null) {
            IntentToolRequest.DraftWhatsApp(topicHint = whatsAppHint)
        } else {
            null
        }
    }
}
