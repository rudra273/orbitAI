package com.example.orbitai.tools.intents

import com.example.orbitai.data.Message
import com.example.orbitai.data.Role

object EmailDraftBuilder {

    fun build(messages: List<Message>, topicHint: String): EmailDraft {
        val recentMessages = messages
            .filter { it.content.isNotBlank() }
            .takeLast(8)

        val latestUserTopic = recentMessages
            .asReversed()
            .firstOrNull { message ->
                message.role == Role.USER && IntentToolCommandParser.parse(message.content) == null
            }
            ?.content
            ?.lineSequence()
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

        val chosenTopic = topicHint.ifBlank { latestUserTopic }
        val subject = chosenTopic.take(72).ifBlank { "Draft email" }

        val contextBlock = recentMessages.joinToString(separator = "\n\n") { message ->
            val speaker = if (message.role == Role.USER) "User" else "Assistant"
            "$speaker: ${message.content.trim()}"
        }

        val body = buildString {
            append("Hi,\n\n")
            if (chosenTopic.isNotBlank()) {
                append("I want to send an email about: ")
                append(chosenTopic)
                append("\n\n")
            }
            if (contextBlock.isNotBlank()) {
                append("Context from OrbitAI chat:\n")
                append(contextBlock)
                append("\n\n")
            }
            append("Best regards,\n")
        }

        return EmailDraft(subject = subject, body = body)
    }
}
