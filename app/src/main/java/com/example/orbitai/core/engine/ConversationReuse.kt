package com.example.orbitai.core.engine

/**
 * Decides whether a live conversation can take the next turn as-is (only the new message is
 * prefilled) or must be rebuilt from the chat history.
 */
object ConversationReuse {

    /** Share of the model's context a live conversation may fill before it is rebuilt (dropping old per-turn context). */
    const val MAX_CONTEXT_SHARE = 0.75

    /**
     * Reuse only when the session belongs to the same chat, model, settings and system prompt
     * ([key]), it holds exactly the messages before the new one ([historyIds], e.g. not after an
     * edit, regenerate or trimmed history), and it still has room.
     */
    fun canReuse(
        liveKey: String?,
        liveMessageIds: List<String>,
        liveTokens: Int,
        key: String,
        historyIds: List<String>,
        contextTokens: Int,
    ): Boolean = liveKey == key && liveMessageIds == historyIds && liveTokens < contextTokens * MAX_CONTEXT_SHARE

    fun estimateTokens(text: String): Int = text.length / 4 + 4
}
