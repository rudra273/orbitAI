package com.example.orbitai.tools.prompts

import com.example.orbitai.data.Message
import com.example.orbitai.data.Role
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReminderPromptBuilderTest {

    @Test
    fun includesDateTimeFormatInstructions() {
        val prompt = ReminderPromptBuilder.build(
            messages = listOf(
                Message(role = Role.USER, content = "Set reminder tomorrow at 6 pm to call mom."),
                Message(role = Role.ASSISTANT, content = "I can create that reminder."),
            ),
            topicHint = "tomorrow at 6 pm to call mom",
            memories = emptyList(),
            now = LocalDateTime.of(2026, 3, 20, 10, 0),
        )

        assertTrue(prompt.contains("Current local date and time: 2026-03-20 10:00"))
        assertTrue(prompt.contains("Date: <YYYY-MM-DD>"))
        assertTrue(prompt.contains("Time: <HH:MM in 24-hour format>"))
        assertTrue(prompt.contains("DurationMinutes: <integer number of minutes>"))
    }
}
