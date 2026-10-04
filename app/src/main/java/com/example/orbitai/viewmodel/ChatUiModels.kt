package com.example.orbitai.viewmodel

import android.net.Uri
import com.example.orbitai.feature.automation.executor.CallContact
import com.example.orbitai.feature.automation.parser.AutomationRequest
import com.example.orbitai.feature.automation.parser.ReminderDraft

data class NotificationReplyDraftState(
    val key: String = "",
    val revision: Long = 0,
    val text: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

data class ChatUiState(
    val isModelLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val loadError: String? = null,
    val infoMessage: String? = null,
    val memorySuggestion: MemorySuggestion? = null,
    val deviceActionRequest: DeviceActionRequest? = null,
)

/** A phone action waiting for the user's OK. [replyMessageId] is the reply to update (tool calls). */
data class DeviceActionRequest(
    val chatId: String,
    val action: com.example.orbitai.feature.automation.device.DeviceAction,
    val replyMessageId: String? = null,
)

/** A fact found in a user message that Orbit offers to remember. */
data class MemorySuggestion(
    val chatId: String,
    val messageId: String,
    val fact: String,
)

data class CloudSendRequest(
    val chatId: String,
    val text: String,
    val imageUris: List<Uri>,
    val modelId: String,
    val modelName: String,
    /** True when the user message is already in the chat (regenerate / edit & resend). */
    val resendExisting: Boolean = false,
)

sealed interface ChatUiEvent {
    data object RequestContactsPermission : ChatUiEvent
    data object RequestNotificationsPermission : ChatUiEvent
    data object RequestCallContactsPermission : ChatUiEvent
    data object RequestCallPermission : ChatUiEvent
}

internal data class PendingWhatsAppExecution(
    val request: AutomationRequest.DraftWhatsApp,
    val draft: com.example.orbitai.feature.automation.parser.WhatsAppDraft,
)

data class CallReviewRequest(
    val chatId: String,
    val contacts: List<CallContact>,
    val voice: Boolean = false,
    val selected: CallContact? = contacts.singleOrNull(),
)

data class VoiceCallPrompt(
    val id: Long,
    val chatId: String,
    val text: String,
    val listenAfter: Boolean = true,
)

internal data class CallLookup(val chatId: String, val recipient: String, val voice: Boolean)

data class ReminderReviewRequest(
    val chatId: String,
    val draft: ReminderDraft,
    val useLocalReminder: Boolean,
)
