package com.example.orbitai.viewmodel

import com.example.orbitai.feature.automation.executor.AutomationExecutor
import com.example.orbitai.feature.automation.parser.AutomationExecutionResult
import com.example.orbitai.feature.automation.parser.AutomationRequest
import com.example.orbitai.feature.automation.parser.EmailDraft
import com.example.orbitai.feature.automation.parser.EmailDraftParser
import com.example.orbitai.feature.automation.parser.LocalReminderParser
import com.example.orbitai.feature.automation.parser.ReminderDraftParser
import com.example.orbitai.feature.automation.parser.RuntimeToolPermission
import com.example.orbitai.feature.automation.parser.WhatsAppDraft
import com.example.orbitai.feature.automation.parser.WhatsAppDraftParser
import com.example.orbitai.feature.automation.tools.ChatToolAction
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Typed device commands (call, remember, remind) and executing the email / WhatsApp / reminder
 * drafts a model produced, including resuming after a permission grant.
 */
internal class AutomationFlowController(
    private val ctx: ChatControllerContext,
    private val calls: CallFlowController,
    private val reminders: ReminderFlowController,
    private val stopGeneration: () -> Unit,
) {

    private val _uiState get() = ctx.uiState
    private val _events get() = ctx.events
    private val viewModelScope get() = ctx.scope
    private val chatRepo = ctx.container.chatRepository
    private val memoryRepo = ctx.container.memoryRepository
    private val automationExecutor = AutomationExecutor(ctx.app)
    private var pendingWhatsAppExecution: PendingWhatsAppExecution? = null

    fun handleDeviceCommand(chatId: String, text: String): Boolean {
        val normalized = com.example.orbitai.feature.automation.parser.DeviceCommandParser.normalize(text)
        val command = com.example.orbitai.feature.automation.parser.DeviceCommandParser.parse(text)
        val reminder = com.example.orbitai.feature.automation.parser.AutomationCommandParser.parse(normalized) as? AutomationRequest.CreateReminder
        if (command == null && reminder == null) return false
        stopGeneration()
        _uiState.update { it.copy(loadError = null, infoMessage = null) }
        viewModelScope.launch {
            try {
                chatRepo.addMessage(chatId, Message(role = Role.USER, content = text))
                val response = when (command) {
                    is com.example.orbitai.feature.automation.parser.DeviceCommand.Call -> {
                        calls.resolveCall(chatId, command.recipient)
                        return@launch
                    }
                    is com.example.orbitai.feature.automation.parser.DeviceCommand.Remember -> {
                        if (memoryRepo.addMemory(command.content, source = "manual"))
                            "Saved to Orbit Memory: ${command.content}"
                        else "Memory is disabled. Enable it in Settings > Memory, then ask me to remember this again."
                    }
                    null -> {
                        val draft = com.example.orbitai.feature.automation.parser.LocalReminderParser.parse(reminder!!.topicHint)
                        reminders.showReview(ReminderReviewRequest(chatId, draft, true))
                        "Review the reminder below, then save it in Orbit. Check the suggested date and time; you can change them before saving."
                    }
                }
                chatRepo.addMessage(chatId, Message(role = Role.ASSISTANT, content = response))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(loadError = "Couldn't complete this action. Please try again.") }
            }
        }
        return true
    }

    /** True when [text] is handled by [handleDeviceCommand] rather than the model. */
    fun isDeviceCommand(text: String): Boolean {
        val normalized = com.example.orbitai.feature.automation.parser.DeviceCommandParser.normalize(text)
        return com.example.orbitai.feature.automation.parser.DeviceCommandParser.parse(text) != null ||
            com.example.orbitai.feature.automation.parser.AutomationCommandParser.parse(normalized) is AutomationRequest.CreateReminder
    }

    /** Parses a finished tool-draft reply and hands it to the matching Android intent or review dialog. */
    suspend fun handleDraftResult(chatId: String, toolRequest: AutomationRequest?, accumulated: String) {
        when (toolRequest) {
            is AutomationRequest.DraftEmail -> {
                when (
                    val result = automationExecutor.execute(
                        request = toolRequest,
                        draft = EmailDraftParser.parse(
                            modelOutput = accumulated,
                            topicHint = toolRequest.topicHint,
                        ),
                    )
                ) {
                    AutomationExecutionResult.Launched -> Unit
                    is AutomationExecutionResult.Failed -> {
                        _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                    }
                    is AutomationExecutionResult.PermissionRequired -> {
                        _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                    }
                }
            }
            is AutomationRequest.DraftWhatsApp -> executeWhatsApp(
                toolRequest,
                WhatsAppDraftParser.parse(
                    modelOutput = accumulated,
                    topicHint = toolRequest.topicHint,
                ),
            )
            is AutomationRequest.CreateReminder -> {
                val draft = ReminderDraftParser.parse(
                    modelOutput = accumulated,
                    topicHint = toolRequest.topicHint,
                )
                reminders.showReview(ReminderReviewRequest(
                    chatId, draft, true,
                ))
            }
            null -> Unit
        }
    }

    private suspend fun executeWhatsApp(request: AutomationRequest.DraftWhatsApp, draft: WhatsAppDraft) {
        when (val result = automationExecutor.execute(request = request, draft = draft)) {
            AutomationExecutionResult.Launched -> Unit
            is AutomationExecutionResult.Failed -> {
                _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
            }
            is AutomationExecutionResult.PermissionRequired -> {
                if (result.permission == RuntimeToolPermission.CONTACTS) {
                    pendingWhatsAppExecution = PendingWhatsAppExecution(request = request, draft = draft)
                    _events.emit(ChatUiEvent.RequestContactsPermission)
                    _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                }
            }
        }
    }

    /**
     * Runs a phone action the model chose with a native tool call, replacing the
     * assistant placeholder [assistantMessageId] with what Orbit did.
     */
    suspend fun handleToolAction(chatId: String, assistantMessageId: String, action: ChatToolAction) {
        val reply = when (action) {
            is ChatToolAction.SendWhatsApp -> {
                executeWhatsApp(
                    AutomationRequest.DraftWhatsApp(action.message),
                    WhatsAppDraft(recipientName = action.recipient, message = action.message),
                )
                val to = action.recipient.takeIf { it.isNotBlank() }?.let { " for $it" }.orEmpty()
                "Opening WhatsApp$to with this message:\n\n${action.message}"
            }
            is ChatToolAction.DraftEmail -> {
                val result = automationExecutor.execute(
                    request = AutomationRequest.DraftEmail(action.subject),
                    draft = EmailDraft(subject = action.subject, body = action.body),
                )
                if (result is AutomationExecutionResult.Failed) {
                    _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                }
                "Opening your email app with a draft.\n\n**${action.subject}**\n\n${action.body}"
            }
            is ChatToolAction.SetReminder -> {
                reminders.showReview(ReminderReviewRequest(chatId, LocalReminderParser.parse(action.request), true))
                "Review the reminder below, then save it in Orbit. Check the suggested date and time; you can change them before saving."
            }
            is ChatToolAction.Call -> {
                // The call flow posts its own messages.
                chatRepo.deleteMessage(assistantMessageId)
                withContext(Dispatchers.Main) { calls.resolveCall(chatId, action.contact) }
                return
            }
            is ChatToolAction.Remember -> {
                if (com.example.orbitai.feature.chat.ChatRepository.isTemporaryChat(chatId)) "Temporary chats don't save anything to Orbit Memory."
                else if (memoryRepo.addMemory(action.fact, source = "manual")) "Saved to Orbit Memory: ${action.fact}"
                else "Memory is disabled. Enable it in Settings > Memory, then ask me to remember this again."
            }
        }
        chatRepo.updateMessage(assistantMessageId, reply, isStreaming = false)
    }

    fun onContactsPermissionResult(granted: Boolean) {
        val pending = pendingWhatsAppExecution
        pendingWhatsAppExecution = null

        if (!granted) {
            _uiState.update {
                it.copy(
                    loadError = "Contacts permission is required to use WhatsApp by contact name.",
                    infoMessage = null,
                )
            }
            return
        }

        if (pending != null) {
            viewModelScope.launch(Dispatchers.IO) {
                ctx.handleIntentResult(
                    automationExecutor.execute(
                        request = pending.request,
                        draft = pending.draft,
                    ),
                    onLaunched = null,
                    onPermissionRequired = { permissionResult ->
                        _uiState.update { it.copy(loadError = permissionResult.message, infoMessage = null) }
                    },
                )
            }
        }
    }
}
