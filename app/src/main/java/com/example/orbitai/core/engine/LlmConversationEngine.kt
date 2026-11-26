package com.example.orbitai.core.engine

import com.example.orbitai.core.common.InferenceSettings

/**
 * Engine that supports multi-turn conversations with optional tool calling.
 * This is an extension of the basic [LlmInferenceEngine] for richer interaction.
 *
 * Currently implemented by [LiteRtLmEngine]. Gemini support planned for later.
 */
interface LlmConversationEngine {

    fun createConversation(
        systemInstruction: String,
        settings: InferenceSettings,
        toolSchemas: List<Any> = emptyList(),
        /** Earlier turns to start the conversation with. */
        history: List<PromptTurn> = emptyList(),
    ): ConversationSession

    fun close()
}

/**
 * A single multi-turn conversation session. Must be closed after use.
 */
interface ConversationSession : AutoCloseable {

    /**
     * Send a message and stream back deltas.
     * Returns the final [TurnResult] which may include tool calls.
     */
    suspend fun streamTurn(
        contents: Any,
        maxDecodedTokens: Int,
        onDelta: suspend (String) -> Unit,
    ): TurnResult

    /** Stops an in-progress turn in the native engine. Safe to call from any thread. */
    fun cancel()
}

/**
 * Result of a single conversation turn.
 */
data class TurnResult(
    val text: String,
    val toolCalls: List<ToolCallResult> = emptyList(),
)

/**
 * A structured tool call returned by the model.
 */
data class ToolCallResult(
    val name: String,
    val arguments: Map<String, Any?>,
)
