package com.example.orbitai.core.prompt

import com.example.orbitai.core.model.PromptStyle
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import org.junit.Assert.assertTrue
import org.junit.Test

class QwenPromptTest {

    @Test
    fun chatPromptUsesChatMlTurnsAndEndsOpenForTheReply() {
        val prompt = ModelPromptBuilder.buildChatPrompt(
            promptStyle = PromptStyle.QWEN,
            messages = listOf(
                Message(role = Role.USER, content = "hello"),
                Message(role = Role.ASSISTANT, content = "hi there"),
                Message(role = Role.USER, content = "what is 2+2?"),
            ),
        )
        assertTrue(prompt.contains("<|im_start|>system\n"))
        assertTrue(prompt.contains("<|im_start|>user\nhello<|im_end|>"))
        assertTrue(prompt.contains("<|im_start|>assistant\nhi there<|im_end|>"))
        assertTrue(prompt.trimEnd().endsWith("<|im_start|>assistant"))
    }

    @Test
    fun instructionPromptIsOneUserTurn() {
        val prompt = ModelPromptBuilder.wrapInstructionPrompt(PromptStyle.QWEN, "translate this")
        assertTrue(prompt.startsWith("<|im_start|>user\n"))
        assertTrue(prompt.endsWith("<|im_start|>assistant\n"))
    }
}
