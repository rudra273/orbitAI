package com.example.orbitai.tools.intents

sealed interface IntentToolRequest {
    data class DraftEmail(val topicHint: String) : IntentToolRequest
}

sealed interface IntentToolExecutionResult {
    data object Launched : IntentToolExecutionResult
    data class Failed(val message: String) : IntentToolExecutionResult
}

data class EmailDraft(
    val subject: String,
    val body: String,
)
