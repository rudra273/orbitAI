package com.example.orbitai.feature.automation.parser

object AutomationCommandParser {

    private val draftEmailPatterns = listOf(
        Regex("""(?i)^/(?:mail|email|draft-mail|draft-email)\b\s*(.*)$"""),
        Regex("""(?i)^draft\s+(?:an?\s+)?(?:mail|email)\b[:\-\s]*(.*)$"""),
    )
    private val draftWhatsAppPatterns = listOf(
        Regex("""(?i)^/(?:whatsapp|wa)\b\s*(.*)$"""),
        Regex("""(?i)^(?:draft|write|send)\s+(?:a\s+)?(?:whatsapp|wa)(?:\s+message)?\b[:\-\s]*(.*)$"""),
        Regex("""(?i)^whatsapp\b[:\-\s]*(.*)$"""),
    )
    private val reminderPatterns = listOf(
        Regex("""(?i)^/(?:remind|reminder)\b\s*(.*)$"""),
        Regex("""(?i)^(?:set|create|add)\s+(?:a\s+)?reminder\b[:\-\s]*(.*)$"""),
        Regex("""(?i)^remind\s+me(?:\s+to)?\b[:\-\s]*(.*)$"""),
    )

    fun parse(input: String): AutomationRequest? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val topicHint = draftEmailPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(trimmed)?.groupValues?.getOrNull(1)
        }?.trim()

        if (topicHint != null) {
            return AutomationRequest.DraftEmail(topicHint = topicHint)
        }

        val whatsAppHint = draftWhatsAppPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(trimmed)?.groupValues?.getOrNull(1)
        }?.trim()

        return if (whatsAppHint != null) {
            AutomationRequest.DraftWhatsApp(topicHint = whatsAppHint)
        } else {
            val reminderHint = reminderPatterns.firstNotNullOfOrNull { pattern ->
                pattern.find(trimmed)?.groupValues?.getOrNull(1)
            }?.trim()

            if (reminderHint != null) {
                AutomationRequest.CreateReminder(topicHint = reminderHint)
            } else {
                null
            }
        }
    }
}
