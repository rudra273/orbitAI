package com.example.orbitai.tools.prompts

import com.example.orbitai.data.Message
import com.example.orbitai.data.Role

object EmailDraftPromptBuilder {

    fun build(
        messages: List<Message>,
        topicHint: String,
        memories: List<String>,
    ): String {
        val sb = StringBuilder()

        sb.append("<start_of_turn>user\n")
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
        sb.append("<end_of_turn>\n")
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }
}
