package com.example.orbitai.core.engine

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Message

/**
 * Wraps a LiteRT-LM [Conversation] to implement the generic [ConversationSession] interface.
 * This allows both bubble and app to use the same multi-turn conversation abstraction.
 */
class LiteRtConversationSession(
    private val conversation: Conversation,
) : ConversationSession {

    override suspend fun streamTurn(
        contents: Any,
        maxDecodedTokens: Int,
        onDelta: suspend (String) -> Unit,
    ): TurnResult {
        val liteRtContents = contents as com.google.ai.edge.litertlm.Contents
        val resolvedMaxTokens = maxDecodedTokens.coerceAtLeast(1)
        var previousText = ""
        var chunkCount = 0
        var reachedLimit = false
        var lastMessage: Message? = null

        conversation.sendMessageAsync(liteRtContents).collect { message ->
            if (reachedLimit) return@collect

            lastMessage = message
            val fullText = extractText(message)
            val delta = if (fullText.startsWith(previousText)) {
                fullText.removePrefix(previousText)
            } else {
                fullText
            }
            previousText = fullText

            if (delta.isNotEmpty()) {
                onDelta(delta)
                chunkCount++
                if (chunkCount >= resolvedMaxTokens) {
                    reachedLimit = true
                    conversation.cancelProcess()
                }
            }
        }

        return TurnResult(
            text = extractText(lastMessage),
            toolCalls = lastMessage?.toolCalls.orEmpty().map { tc ->
                ToolCallResult(
                    name = tc.name,
                    arguments = tc.arguments,
                )
            },
        )
    }

    override fun close() {
        try {
            conversation.close()
        } catch (_: Exception) {
        }
    }

    private fun extractText(message: Message?): String {
        return message?.contents?.contents
            ?.mapNotNull { content -> (content as? Content.Text)?.text }
            ?.joinToString(separator = "")
            .orEmpty()
    }
}
