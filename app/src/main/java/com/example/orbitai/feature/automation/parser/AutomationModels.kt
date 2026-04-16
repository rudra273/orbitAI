package com.example.orbitai.feature.automation.parser

sealed interface AutomationRequest {
    data class DraftEmail(val topicHint: String) : AutomationRequest
    data class DraftWhatsApp(val topicHint: String) : AutomationRequest
    data class CreateReminder(val topicHint: String) : AutomationRequest
}

sealed interface AutomationExecutionResult {
    data object Launched : AutomationExecutionResult
    data class Failed(val message: String) : AutomationExecutionResult
    data class PermissionRequired(
        val permission: RuntimeToolPermission,
        val message: String,
    ) : AutomationExecutionResult
}

enum class RuntimeToolPermission {
    CONTACTS,
    NOTIFICATIONS,
}

data class EmailDraft(
    val subject: String,
    val body: String,
)

data class WhatsAppDraft(
    val recipientName: String,
    val message: String,
)

data class ReminderDraft(
    val title: String,
    val description: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
)
