package com.example.orbitai.tools.prompts

import com.example.orbitai.data.Message
import com.example.orbitai.data.Role

object WhatsAppDraftPromptBuilder {

    fun build(
        messages: List<Message>,
        topicHint: String,
        memories: List<String>,
    ): String {
        val sb = StringBuilder()

        sb.append("<start_of_turn>user\n")
        sb.append("You are drafting a WhatsApp message on behalf of the user. ")
        sb.append("Understand what the user wants to send from the recent conversation and write the final WhatsApp message for them.\n\n")
        sb.append("Return exactly in this format:\n")
        sb.append("Recipient: <contact name if the user mentioned one, otherwise leave blank>\n")
        sb.append("Message: <final whatsapp message>\n\n")
        sb.append("Rules:\n")
        sb.append("- Do not mention chat, conversation, context, OrbitAI, or that you are an AI.\n")
        sb.append("- Write the actual WhatsApp message the user wants to send.\n")
        sb.append("- Extract the recipient contact name if the user mentioned a person by name.\n")
        sb.append("- Keep it natural, concise, and suitable for WhatsApp.\n")
        sb.append("- Infer tone and purpose from the user's request when possible.\n")
        sb.append("- Output only the formatted message, with no explanation.\n\n")

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
