package com.example.orbitai.tools.intents

object EmailDraftParser {

    private val subjectRegex = Regex("(?im)^subject\\s*:\\s*(.+)$")
    private val bodyRegex = Regex("(?is)\\bbody\\s*:\\s*(.+)$")

    fun parse(modelOutput: String, topicHint: String): EmailDraft {
        val trimmedOutput = modelOutput.trim()
        val parsedSubject = subjectRegex.find(trimmedOutput)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
        val parsedBody = bodyRegex.find(trimmedOutput)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()

        val cleanedBody = when {
            parsedBody.isNotBlank() -> parsedBody
            trimmedOutput.isNotBlank() -> trimmedOutput
                .replace(subjectRegex, "")
                .replace(Regex("(?im)^body\\s*:\\s*"), "")
                .trim()
            else -> ""
        }

        val fallbackSubject = topicHint
            .trim()
            .take(72)
            .ifBlank {
                cleanedBody
                    .lineSequence()
                    .firstOrNull()
                    ?.trim()
                    ?.take(72)
                    .orEmpty()
            }
            .ifBlank { "Draft email" }

        return EmailDraft(
            subject = parsedSubject.ifBlank { fallbackSubject },
            body = cleanedBody,
        )
    }
}
