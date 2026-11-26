package com.example.orbitai.core.prompt

import com.example.orbitai.feature.chat.Message

/** Prompt inputs trimmed to fit a model's context window. */
data class BudgetedPrompt(
    val messages: List<Message>,
    val ragContext: List<String>,
    val memories: List<String>,
    val droppedMessageCount: Int,
    val latestMessageTruncated: Boolean,
)

/**
 * Selects which history, RAG chunks and memories fit in a model's context window.
 *
 * Fill order: system prompt and the newest message always, then RAG context,
 * then memories, then as many earlier turns as fit (newest first).
 * Token counts are estimates (chars / 3), deliberately conservative.
 */
object PromptBudget {

    const val TRUNCATION_NOTICE = "\n\n[Message truncated to fit the model's context window.]"
    const val IMAGE_TOKENS = 260
    private const val TURN_OVERHEAD_TOKENS = 8
    private const val TEMPLATE_OVERHEAD_TOKENS = 192
    private const val MIN_LATEST_MESSAGE_TOKENS = 32

    fun estimateTokens(text: String): Int = (text.length + 2) / 3

    fun fit(
        messages: List<Message>,
        ragContext: List<String>,
        memories: List<String>,
        systemPrompt: String?,
        contextTokens: Int,
        reservedOutputTokens: Int,
        imageCount: Int = 0,
    ): BudgetedPrompt {
        if (messages.isEmpty()) return BudgetedPrompt(emptyList(), emptyList(), emptyList(), 0, false)

        val outputReserve = reservedOutputTokens.coerceAtMost(contextTokens / 2)
        var remaining = contextTokens - outputReserve - TEMPLATE_OVERHEAD_TOKENS -
            estimateTokens(AiSafetyPolicy.withInstructions(systemPrompt)) - imageCount * IMAGE_TOKENS

        // The newest message is always kept; it is truncated only if it can't fit on its own.
        var latest = messages.last()
        val latestTruncated = cost(latest.content) > remaining
        if (latestTruncated) {
            val allowedTokens = (remaining - TURN_OVERHEAD_TOKENS - estimateTokens(TRUNCATION_NOTICE))
                .coerceAtLeast(MIN_LATEST_MESSAGE_TOKENS)
            latest = latest.copy(content = latest.content.take(allowedTokens * 3) + TRUNCATION_NOTICE)
        }
        remaining -= cost(latest.content)

        val keptRag = mutableListOf<String>()
        for (chunk in ragContext) {
            if (cost(chunk) > remaining) break
            keptRag += chunk
            remaining -= cost(chunk)
        }

        val keptMemories = mutableListOf<String>()
        for (memory in memories) {
            if (cost(memory) > remaining) break
            keptMemories += memory
            remaining -= cost(memory)
        }

        val earlier = messages.dropLast(1)
        val keptEarlier = ArrayDeque<Message>()
        for (message in earlier.asReversed()) {
            if (cost(message.content) > remaining) break
            keptEarlier.addFirst(message)
            remaining -= cost(message.content)
        }

        return BudgetedPrompt(
            messages = keptEarlier + latest,
            ragContext = keptRag,
            memories = keptMemories,
            droppedMessageCount = earlier.size - keptEarlier.size,
            latestMessageTruncated = latestTruncated,
        )
    }

    private fun cost(text: String): Int = estimateTokens(text) + TURN_OVERHEAD_TOKENS
}
