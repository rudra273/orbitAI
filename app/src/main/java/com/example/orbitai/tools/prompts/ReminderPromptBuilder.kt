package com.example.orbitai.tools.prompts

import com.example.orbitai.data.Message
import com.example.orbitai.data.Role
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ReminderPromptBuilder {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun build(
        messages: List<Message>,
        topicHint: String,
        memories: List<String>,
        now: LocalDateTime = LocalDateTime.now(),
    ): String {
        val sb = StringBuilder()

        sb.append("<start_of_turn>user\n")
        sb.append("You are creating a reminder/calendar event on behalf of the user. ")
        sb.append("Understand what reminder the user wants, including date and time, from the recent conversation.\n\n")
        sb.append("Current local date and time: ")
        sb.append(now.format(dateTimeFormatter))
        sb.append("\n\n")
        sb.append("Return exactly in this format:\n")
        sb.append("Title: <short reminder title>\n")
        sb.append("Description: <optional extra details, may be blank>\n")
        sb.append("Date: <YYYY-MM-DD>\n")
        sb.append("Time: <HH:MM in 24-hour format>\n")
        sb.append("DurationMinutes: <integer number of minutes>\n\n")
        sb.append("Rules:\n")
        sb.append("- Do not mention chat, conversation, context, OrbitAI, or that you are an AI.\n")
        sb.append("- Choose a specific date and time based on the user's request.\n")
        sb.append("- If the user did not specify duration, use 30 minutes.\n")
        sb.append("- Keep the title concise and natural.\n")
        sb.append("- Output only the formatted reminder, with no explanation.\n\n")

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
