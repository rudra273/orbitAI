package com.example.orbitai.tools.intents

object WhatsAppDraftParser {

    private val recipientRegex = Regex("(?im)^recipient\\s*:\\s*(.*)$")
    private val messageRegex = Regex("(?is)^message\\s*:\\s*(.+)$")

    fun parse(modelOutput: String, topicHint: String): WhatsAppDraft {
        val trimmedOutput = modelOutput.trim()
        val recipientName = recipientRegex.find(trimmedOutput)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
        val parsedMessage = messageRegex.find(trimmedOutput)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()

        val cleanedMessage = parsedMessage.ifBlank {
            trimmedOutput
                .replace(recipientRegex, "")
                .replace(Regex("(?im)^message\\s*:\\s*"), "")
                .trim()
        }.ifBlank {
            topicHint.trim().ifBlank { "Hello" }
        }

        return WhatsAppDraft(
            recipientName = recipientName,
            message = cleanedMessage,
        )
    }
}