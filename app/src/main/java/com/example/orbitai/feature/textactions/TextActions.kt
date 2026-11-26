package com.example.orbitai.feature.textactions

/** What Orbit can do with text selected in another app. */
enum class TextAction(val label: String, val replacesText: Boolean) {
    PROOFREAD("Proofread", true),
    REWRITE_FORMAL("Rewrite: formal", true),
    REWRITE_FRIENDLY("Rewrite: friendly", true),
    REWRITE_SHORTER("Rewrite: shorter", true),
    TRANSLATE("Translate", true),
    SUMMARIZE("Summarize", false),
    EXPLAIN("Explain", false),
    REPLY("Reply to this", false),
}

object TextActionPrompts {

    private const val OUTPUT_ONLY = "Reply with the result only: no introduction, no quotes, no notes."

    fun instruction(action: TextAction, text: String, language: String): String {
        val task = when (action) {
            TextAction.PROOFREAD -> "Fix spelling, grammar and punctuation in the text below. Keep the meaning, tone, language and formatting. $OUTPUT_ONLY"
            TextAction.REWRITE_FORMAL -> "Rewrite the text below in a clear, formal, professional tone, in the same language. $OUTPUT_ONLY"
            TextAction.REWRITE_FRIENDLY -> "Rewrite the text below in a warm, friendly, natural tone, in the same language. $OUTPUT_ONLY"
            TextAction.REWRITE_SHORTER -> "Rewrite the text below to be about half as long, keeping the key points, in the same language. $OUTPUT_ONLY"
            TextAction.TRANSLATE -> "Translate the text below into $language. If it is already in $language, translate it into English. $OUTPUT_ONLY"
            TextAction.SUMMARIZE -> "Summarize the text below in a few short bullet points."
            TextAction.EXPLAIN -> "Explain the text below in simple words, briefly."
            TextAction.REPLY -> "Write a short, natural reply to the message below, as the person who received it, in the message's language. $OUTPUT_ONLY"
        }
        return "$task\n\nText:\n${text.take(MAX_INPUT_CHARS)}"
    }

    /** Removes wrapping quotes or a leading label the model may add despite instructions. */
    fun cleanResult(raw: String): String = raw.trim()
        .replace(Regex("""(?i)^(?:here(?:'s| is) (?:the |your )?[^:\n]{0,40}:|corrected text:|translation:|rewritten text:)\s*"""), "")
        .trim()
        .removeSurrounding("\"")
        .trim()

    const val MAX_INPUT_CHARS = 6000
}
