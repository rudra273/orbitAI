package com.example.orbitai.core.prompt

import com.example.orbitai.core.model.PromptStyle
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role

object ModelPromptBuilder {

    fun buildChatPrompt(
        promptStyle: PromptStyle,
        messages: List<Message>,
        ragContext: List<String> = emptyList(),
        memories: List<String> = emptyList(),
        systemPrompt: String? = null,
        includeImageTokens: Boolean = false,
    ): String {
        return when (promptStyle) {
            PromptStyle.GEMMA -> buildGemmaPrompt(messages, ragContext, memories, systemPrompt, includeImageTokens)
            PromptStyle.PHI -> buildPhiPrompt(messages, ragContext, memories, systemPrompt)
            PromptStyle.LLAMA3 -> buildLlama3Prompt(messages, ragContext, memories, systemPrompt)
            PromptStyle.QWEN -> buildQwenPrompt(messages, ragContext, memories, systemPrompt)
        }
    }

    fun wrapInstructionPrompt(
        promptStyle: PromptStyle,
        instruction: String,
    ): String {
        return when (promptStyle) {
            PromptStyle.GEMMA -> "<start_of_turn>user\n$instruction<end_of_turn>\n<start_of_turn>model\n"
            PromptStyle.PHI -> "<|user|>\n$instruction<|end|>\n<|assistant|>\n"
            PromptStyle.LLAMA3 -> "<|begin_of_text|><|start_header_id|>user<|end_header_id|>\n\n$instruction<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
            PromptStyle.QWEN -> "<|im_start|>user\n$instruction<|im_end|>\n<|im_start|>assistant\n"
        }
    }

    private fun buildGemmaPrompt(
        messages: List<Message>,
        ragContext: List<String>,
        memories: List<String>,
        systemPrompt: String?,
        includeImageTokens: Boolean,
    ): String {
        val sb = StringBuilder()

        if (!systemPrompt.isNullOrBlank()) {
            sb.append("<start_of_turn>user\n")
            sb.append("System instructions: $systemPrompt\n")
            sb.append("<end_of_turn>\n")
            sb.append("<start_of_turn>model\nUnderstood.<end_of_turn>\n")
        }

        val hasContext = memories.isNotEmpty() || ragContext.isNotEmpty()
        if (hasContext) {
            sb.append("<start_of_turn>user\n")
            if (memories.isNotEmpty()) {
                sb.append("The following is personal context you know about the user. Always use this when relevant:\n\n")
                memories.forEachIndexed { index, fact ->
                    sb.append("[Memory ${index + 1}] $fact\n")
                }
                sb.append("\n")
            }
            if (ragContext.isNotEmpty()) {
                sb.append("Use the following reference documents to answer the question. ")
                sb.append("If the answer is not in the documents, say so.\n\n")
                ragContext.forEachIndexed { index, chunk ->
                    sb.append("[Document ${index + 1}]\n$chunk\n\n")
                }
            }
            sb.append("Now answer the user's questions using the above context.<end_of_turn>\n")
            sb.append("<start_of_turn>model\nUnderstood. I'll use the provided context to answer your questions.<end_of_turn>\n")
        }

        messages.forEach { message ->
            when (message.role) {
                Role.USER -> {
                    sb.append("<start_of_turn>user\n")
                    if (includeImageTokens && message.imageUris.isNotEmpty()) {
                        repeat(message.imageUris.size) {
                            sb.append("<start_of_image>\n")
                        }
                    }
                    sb.append(message.content)
                    sb.append("<end_of_turn>\n")
                }
                Role.ASSISTANT -> sb.append("<start_of_turn>model\n${message.content}<end_of_turn>\n")
            }
        }
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }

    private fun buildPhiPrompt(
        messages: List<Message>,
        ragContext: List<String>,
        memories: List<String>,
        systemPrompt: String?,
    ): String {
        val sb = StringBuilder()
        val systemBlock = buildSystemContext(memories, ragContext, systemPrompt)
        if (systemBlock.isNotBlank()) {
            sb.append("<|system|>\n")
            sb.append(systemBlock)
            sb.append("<|end|>\n")
        }
        messages.forEach { message ->
            when (message.role) {
                Role.USER -> {
                    sb.append("<|user|>\n")
                    sb.append(message.content)
                    sb.append("<|end|>\n")
                }
                Role.ASSISTANT -> {
                    sb.append("<|assistant|>\n")
                    sb.append(message.content)
                    sb.append("<|end|>\n")
                }
            }
        }
        sb.append("<|assistant|>\n")
        return sb.toString()
    }

    private fun buildLlama3Prompt(
        messages: List<Message>,
        ragContext: List<String>,
        memories: List<String>,
        systemPrompt: String?,
    ): String {
        val sb = StringBuilder()
        sb.append("<|begin_of_text|>")
        val systemBlock = buildSystemContext(memories, ragContext, systemPrompt)
        if (systemBlock.isNotBlank()) {
            appendLlama3Turn(sb, "system", systemBlock)
        }
        messages.forEach { message ->
            when (message.role) {
                Role.USER -> appendLlama3Turn(sb, "user", message.content)
                Role.ASSISTANT -> appendLlama3Turn(sb, "assistant", message.content)
            }
        }
        sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        return sb.toString()
    }

    private fun buildQwenPrompt(
        messages: List<Message>,
        ragContext: List<String>,
        memories: List<String>,
        systemPrompt: String?,
    ): String {
        val sb = StringBuilder()
        val systemBlock = buildSystemContext(memories, ragContext, systemPrompt)
        if (systemBlock.isNotBlank()) {
            appendQwenTurn(sb, "system", systemBlock)
        }
        messages.forEach { message ->
            when (message.role) {
                Role.USER -> appendQwenTurn(sb, "user", message.content)
                Role.ASSISTANT -> appendQwenTurn(sb, "assistant", message.content)
            }
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    private fun buildSystemContext(
        memories: List<String>,
        ragContext: List<String>,
        systemPrompt: String?,
    ): String {
        val sections = mutableListOf<String>()

        if (!systemPrompt.isNullOrBlank()) {
            sections += systemPrompt.trim()
        }

        if (memories.isNotEmpty()) {
            sections += buildString {
                append("Personal context about the user:\n")
                memories.forEachIndexed { index, fact ->
                    append("[Memory ")
                    append(index + 1)
                    append("] ")
                    append(fact)
                    append('\n')
                }
            }.trim()
        }

        if (ragContext.isNotEmpty()) {
            sections += buildString {
                append("Reference documents. Use them when relevant, and say so if the answer is not in them:\n")
                ragContext.forEachIndexed { index, chunk ->
                    append("[Document ")
                    append(index + 1)
                    append("]\n")
                    append(chunk)
                    append("\n\n")
                }
            }.trim()
        }

        return sections.joinToString(separator = "\n\n")
    }

    private fun appendLlama3Turn(builder: StringBuilder, role: String, content: String) {
        builder.append("<|start_header_id|>")
        builder.append(role)
        builder.append("<|end_header_id|>\n\n")
        builder.append(content)
        builder.append("<|eot_id|>")
    }

    private fun appendQwenTurn(builder: StringBuilder, role: String, content: String) {
        builder.append("<|im_start|>")
        builder.append(role)
        builder.append('\n')
        builder.append(content)
        builder.append("<|im_end|>\n")
    }
}
