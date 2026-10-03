package com.example.orbitai.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.core.database.Mode
import com.example.orbitai.core.database.ReminderEntity
import com.example.orbitai.core.database.Space
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.feature.automation.executor.CallContact
import com.example.orbitai.feature.automation.parser.ReminderDraft
import com.example.orbitai.feature.automation.replies.ReplyNotification
import com.example.orbitai.feature.chat.Chat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * UI entry point for chat, reminders, calls and message replies. The work lives in the
 * controllers below; this class wires them together and exposes their state to Compose.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx = ChatControllerContext(application, viewModelScope)
    private val generation = ChatGenerationController(ctx)
    private val reminderFlow = ReminderFlowController(ctx)
    private val callFlow = CallFlowController(ctx, generation::stopGeneration)
    private val automationFlow = AutomationFlowController(ctx, callFlow, reminderFlow, generation::stopGeneration)
    private val notificationReplies = NotificationReplyController(ctx, generation)

    val chatRepo = ctx.container.chatRepository
    val memoryRepo = ctx.container.memoryRepository

    init {
        generation.automation = automationFlow
        reminderFlow.restoreOnStart()
    }

    // ── Shared UI state ───────────────────────────────────────────────────────
    val uiState: StateFlow<ChatUiState> = ctx.uiState.asStateFlow()
    val events: SharedFlow<ChatUiEvent> = ctx.events.asSharedFlow()
    val chats: StateFlow<List<Chat>> = chatRepo.chats

    /** All available spaces — observed by the chat screen for the space selector. */
    val spaces: StateFlow<List<Space>> = ctx.container.spaceRepository.spaces

    /** Active modes only — observed by the chat screen for the mode selector. */
    val modes: StateFlow<List<Mode>> = ctx.container.modeRepository.activeModes

    // ── Chat management ───────────────────────────────────────────────────────
    private val newChatMutex = Mutex()

    /** Returns the latest chat without user messages, or creates one. Serialized so rapid taps can't create duplicates. */
    suspend fun createNewChat(): String = newChatMutex.withLock {
        chatRepo.findReusableEmptyChatId() ?: chatRepo.createChat().id
    }

    fun openNewChat(onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(createNewChat()) }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) { chatRepo.deleteChat(chatId) }
    }

    // ── Generation ────────────────────────────────────────────────────────────
    val activeSpaceIds: StateFlow<Set<String>> = generation.activeSpaceIds
    val activeModeId: StateFlow<String> = generation.activeModeId
    val availableModels: StateFlow<List<LlmModel>> = generation.availableModels
    val cloudSendRequest: StateFlow<CloudSendRequest?> = generation.cloudSendRequest

    fun toggleSpace(id: String) = generation.toggleSpace(id)
    fun selectMode(id: String) = generation.selectMode(id)
    fun refreshAvailableModels() = generation.refreshAvailableModels()
    fun selectModel(chatId: String, model: LlmModel) = generation.selectModel(chatId, model)
    fun onCloudSendConsent(granted: Boolean) = generation.onCloudSendConsent(granted)
    fun stopGeneration() = generation.stopGeneration()
    fun regenerate(chatId: String) = generation.regenerate(chatId)
    fun editAndResend(chatId: String, messageId: String, newText: String) = generation.editAndResend(chatId, messageId, newText)
    fun deleteMessage(messageId: String) = generation.deleteMessage(messageId)
    fun sendMessage(
        chatId: String,
        userText: String,
        imageUris: List<Uri> = emptyList(),
        approvedCloudModelId: String? = null,
    ) = generation.sendMessage(chatId, userText, imageUris, approvedCloudModelId)

    // ── Reminders ─────────────────────────────────────────────────────────────
    val reminderReview: StateFlow<ReminderReviewRequest?> = reminderFlow.reminderReview
    val reminders: StateFlow<List<ReminderEntity>> = reminderFlow.reminders

    fun dismissReminderReview() = reminderFlow.dismissReminderReview()
    fun confirmReminder(draft: ReminderDraft, useLocalReminder: Boolean) = reminderFlow.confirmReminder(draft, useLocalReminder)
    fun editReminder(reminder: ReminderEntity? = null) = reminderFlow.editReminder(reminder)
    fun cancelReminder(id: String) = reminderFlow.cancelReminder(id)
    fun snoozeReminder(id: String) = reminderFlow.snoozeReminder(id)
    fun completeReminder(id: String) = reminderFlow.completeReminder(id)
    fun refreshReminders() = reminderFlow.refreshReminders()
    fun onNotificationsPermissionResult(granted: Boolean) = reminderFlow.onNotificationsPermissionResult(granted)

    // ── Calls ─────────────────────────────────────────────────────────────────
    val callReview: StateFlow<CallReviewRequest?> = callFlow.callReview
    val voiceCallPrompt: StateFlow<VoiceCallPrompt?> = callFlow.voiceCallPrompt

    fun dismissCallReview() = callFlow.dismissCallReview()
    fun cancelVoiceCall(chatId: String) = callFlow.cancelVoiceCall(chatId)
    fun onVoiceCallPromptSpoken(id: Long) = callFlow.onVoiceCallPromptSpoken(id)
    fun showCallReviewForVoiceFailure(chatId: String) = callFlow.showCallReviewForVoiceFailure(chatId)
    fun handleVoiceCall(chatId: String, text: String): Boolean = callFlow.handleVoiceCall(chatId, text)
    fun confirmCall(contact: CallContact, dialOnly: Boolean = false) = callFlow.confirmCall(contact, dialOnly)
    fun onCallContactsPermissionResult(granted: Boolean) = callFlow.onCallContactsPermissionResult(granted)
    fun onCallPermissionResult(granted: Boolean) = callFlow.onCallPermissionResult(granted)

    // ── Automation ────────────────────────────────────────────────────────────
    fun onContactsPermissionResult(granted: Boolean) = automationFlow.onContactsPermissionResult(granted)

    // ── Message replies ───────────────────────────────────────────────────────
    val notificationReplyDraft: StateFlow<NotificationReplyDraftState> = notificationReplies.notificationReplyDraft

    fun clearNotificationReplyDraft() = notificationReplies.clearNotificationReplyDraft()
    fun draftNotificationReply(preview: ReplyNotification, instruction: String) =
        notificationReplies.draftNotificationReply(preview, instruction)

    override fun onCleared() {
        super.onCleared()
        // The model is shared with the bubble; free it only if nothing else is using it.
        ctx.container.appScope.launch { ctx.container.llmRepository.releaseIfIdle() }
    }
}
