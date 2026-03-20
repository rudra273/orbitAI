package com.example.orbitai.prompts

import com.example.orbitai.data.Message
import com.example.orbitai.data.Role

object GemmaChatPromptBuilder {

    fun build(
        messages: List<Message>,
        ragContext: List<String> = emptyList(),
        memories: List<String> = emptyList(),
        systemPrompt: String? = null,
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
                Role.USER -> sb.append("<start_of_turn>user\n${message.content}<end_of_turn>\n")
                Role.ASSISTANT -> sb.append("<start_of_turn>model\n${message.content}<end_of_turn>\n")
            }
        }
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }
}
