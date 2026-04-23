package com.example.orbitai.feature.automation.prompt

import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role

object EmailDraftPromptBuilder {

    fun build(
        messages: List<Message>,
        topicHint: String,
        memories: List<String>,
    ): String {
        val sb = StringBuilder()

        sb.append("You are drafting an email on behalf of the user. ")
        sb.append("Understand what the user wants to send from the recent conversation and write the email for them.\n\n")
        sb.append("Return exactly in this format:\n")
        sb.append("Subject: <one line subject>\n")
        sb.append("Body:\n")
        sb.append("<full email body>\n\n")
        sb.append("Rules:\n")
        sb.append("- Do not mention chat, conversation, context, OrbitAI, or that you are an AI.\n")
        sb.append("- Write the actual email the user wants to send.\n")
        sb.append("- Infer the recipient, tone, and purpose from the user's request when possible.\n")
        sb.append("- Keep the subject concise and natural.\n")
        sb.append("- Output only the formatted draft, with no explanation.\n\n")

        if (topicHint.isNotBlank()) {
            sb.append("Explicit request: ")
            sb.append(topicHint)
            sb.append("\n\n")
        }

        if (memories.isNotEmpty()) {
            sb.append("Useful personal context:\n")
            memories.forEachIndexed { index, fact ->
                sb.append("[")
                sb.append(index + 1)
                sb.append("] ")
                sb.append(fact)
                sb.append("\n")
            }
            sb.append("\n")
        }

        sb.append("Recent conversation:\n")
        messages.takeLast(10).forEach { message ->
            val speaker = if (message.role == Role.USER) "User" else "Assistant"
            sb.append(speaker)
            sb.append(": ")
            sb.append(message.content)
            sb.append("\n")
        }
        return sb.toString()
    }
}
