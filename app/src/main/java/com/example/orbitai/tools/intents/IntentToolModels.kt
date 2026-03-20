package com.example.orbitai.tools.intents

sealed interface IntentToolRequest {
    data class DraftEmail(val topicHint: String) : IntentToolRequest
    data class DraftWhatsApp(val topicHint: String) : IntentToolRequest
}

sealed interface IntentToolExecutionResult {
    data object Launched : IntentToolExecutionResult
    data class Failed(val message: String) : IntentToolExecutionResult
    data class PermissionRequired(
        val permission: RuntimeToolPermission,
        val message: String,
    ) : IntentToolExecutionResult
}

enum class RuntimeToolPermission {
    CONTACTS,
}

data class EmailDraft(
    val subject: String,
    val body: String,
)

data class WhatsAppDraft(
    val recipientName: String,
    val message: String,
)
