package com.example.orbitai.feature.automation.parser

/**
 * Finds WhatsApp, email and reminder requests phrased freely, e.g. "send hi to aditya on whtsapp"
 * or "can you please remind me at 5 to call mom". Used after the strict command patterns so
 * every model, with or without native tool calling, can run these actions. Questions about
 * the apps ("how do I send a WhatsApp message?") are left to the model.
 */
object RequestIntentDetector {

    private val questionStart = Regex("""(?i)^(?:how|what|why|when|where|which|who|is|are|does|do|did|should|explain|tell me about)\b""")
    private val messageVerb = Regex("""(?i)\b(?:send|message|msg|text|tell|say|ping|write|drop|reply|wish|ask|inform|let)\b""")
    private val emailVerb = Regex("""(?i)send|write|draft|compose|prepare""")
    private val remindMe = Regex("""(?i)\b([A-Za-z]+)\s+me\b(?!\s+of\b)(?:\s+to\b)?""")
    private val reminderVerb = Regex("""(?i)\b(?:set|create|add|make|put|schedule)\b""")

    fun detect(input: String): AutomationRequest? {
        val text = DeviceCommandParser.normalize(input).replace(Regex("""(?i)^please\s+"""), "")
        if (text.isBlank() || questionStart.containsMatchIn(text) || text.trimEnd().endsWith("?") && !text.contains(Regex("(?i)\\b(can|could|will|would) you\\b"))) {
            return null
        }
        val words = Regex("""[A-Za-z']+""").findAll(text).map { it.value }.toList()

        val mentionsWhatsApp = words.any { CommandSpelling.matches(it, "whatsapp") || it.equals("wa", true) } ||
            Regex("""(?i)\bwhats\s*app\b""").containsMatchIn(text)
        if (mentionsWhatsApp && messageVerb.containsMatchIn(text)) return AutomationRequest.DraftWhatsApp(text)

        // The verb must come just before the email word: "write an email", "send a quick mail".
        val draftsEmail = words.indices.any { i ->
            (CommandSpelling.matches(words[i], "email") || words[i].equals("mail", true)) &&
                words.subList(maxOf(0, i - 3), i).any { emailVerb.matches(it) }
        }
        if (draftsEmail) return AutomationRequest.DraftEmail(text)

        remindMe.findAll(text).firstOrNull { CommandSpelling.matches(it.groupValues[1], "remind") }?.let { match ->
            return AutomationRequest.CreateReminder(text.removeRange(match.range).replace(Regex("\\s+"), " ").trim())
        }
        if (reminderVerb.containsMatchIn(text) && words.any { CommandSpelling.matches(it, "reminder") }) {
            val hint = Regex("""(?i)\b(?:set|create|add|make|put|schedule)\s+(?:me\s+)?(?:an?\s+|the\s+)?([A-Za-z]+)(?:\s+(?:for|to|about|that))?""")
                .findAll(text).firstOrNull { CommandSpelling.matches(it.groupValues[1], "reminder") }
                ?.let { text.removeRange(it.range).replace(Regex("\\s+"), " ").trim() } ?: text
            return AutomationRequest.CreateReminder(hint)
        }
        return null
    }
}
