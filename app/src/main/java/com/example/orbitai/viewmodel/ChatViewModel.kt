package com.example.orbitai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.feature.chat.ChatRepository
import com.example.orbitai.core.common.ModeInferenceSettingsStore
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.feature.chat.Chat
import com.example.orbitai.core.engine.LlmRepository
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import com.example.orbitai.feature.memory.MemoryFeatureStore
import com.example.orbitai.feature.modes.ModeRepository
import com.example.orbitai.core.model.ModelDownloader
import com.example.orbitai.feature.spaces.SpaceRepository
import com.example.orbitai.core.common.TokenStore
import com.example.orbitai.core.model.availableChatModels
import com.example.orbitai.core.database.Mode
import com.example.orbitai.core.database.ORBIT_MODE_ID
import com.example.orbitai.core.database.Space
import com.example.orbitai.feature.automation.AutomationRoute
import com.example.orbitai.feature.automation.AutomationRouter
import com.example.orbitai.feature.automation.AutomationSettingsStore
import com.example.orbitai.feature.automation.executor.AutomationExecutor
import com.example.orbitai.feature.automation.parser.AutomationExecutionResult
import com.example.orbitai.feature.automation.parser.AutomationRequest
import com.example.orbitai.feature.memory.MemoryRepository
import com.example.orbitai.core.prompt.ModelPromptBuilder
import com.example.orbitai.feature.automation.parser.EmailDraftParser
import com.example.orbitai.feature.automation.parser.ReminderDraft
import com.example.orbitai.feature.automation.parser.ReminderDraftParser
import com.example.orbitai.feature.automation.parser.RuntimeToolPermission
import com.example.orbitai.feature.automation.parser.WhatsAppDraftParser
import com.example.orbitai.feature.automation.prompt.EmailDraftPromptBuilder
import com.example.orbitai.feature.automation.prompt.ReminderPromptBuilder
import com.example.orbitai.feature.automation.prompt.WhatsAppDraftPromptBuilder
import com.example.orbitai.feature.automation.reminder.ReminderScheduler
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import com.example.orbitai.core.model.ModelProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.example.orbitai.core.engine.InferenceInput
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

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
)

data class CloudSendRequest(
    val chatId: String,
    val text: String,
    val imageUris: List<Uri>,
    val modelId: String,
    val modelName: String,
)

sealed interface ChatUiEvent {
    data object RequestContactsPermission : ChatUiEvent
    data object RequestNotificationsPermission : ChatUiEvent
    data object RequestCallContactsPermission : ChatUiEvent
    data object RequestCallPermission : ChatUiEvent
}

private data class PendingWhatsAppExecution(
    val request: AutomationRequest.DraftWhatsApp,
    val draft: com.example.orbitai.feature.automation.parser.WhatsAppDraft,
)

data class CallReviewRequest(
    val chatId: String,
    val contacts: List<com.example.orbitai.feature.automation.executor.CallContact>,
)

data class ReminderReviewRequest(
    val chatId: String,
    val draft: ReminderDraft,
    val useLocalReminder: Boolean,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val chatRepo = ChatRepository(application)
    private val llmRepo = LlmRepository(application)
    private val modelDownloader = ModelDownloader(application)
    private val tokenStore = TokenStore(application)
    private val modeInferenceStore = ModeInferenceSettingsStore(application)
    private val spaceRepo = SpaceRepository(application)
    private val modeRepo = ModeRepository(application)
    private val memoryFeatureStore = MemoryFeatureStore(application)
    private val automationSettingsStore = AutomationSettingsStore(application)
    val memoryRepo = MemoryRepository(application)
    private val automationExecutor = AutomationExecutor(application)
    private val reminderScheduler = ReminderScheduler(application)

    /** All available spaces — observed by the chat screen for the space selector. */
    val spaces: StateFlow<List<Space>> = spaceRepo.spaces

    /** Active modes only — observed by the chat screen for the mode selector. */
    val modes: StateFlow<List<Mode>> = modeRepo.activeModes

    private val _activeSpaceIds = MutableStateFlow<Set<String>>(emptySet())
    val activeSpaceIds: StateFlow<Set<String>> = _activeSpaceIds.asStateFlow()

    /** ID of the currently active mode; defaults to Orbit. */
    private val _activeModeId = MutableStateFlow(ORBIT_MODE_ID)
    val activeModeId: StateFlow<String> = _activeModeId.asStateFlow()

    fun toggleSpace(id: String) {
        _activeSpaceIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun selectMode(id: String) {
        _activeModeId.value = id
    }

    val chats: StateFlow<List<Chat>> = chatRepo.chats

    private val _availableModels = MutableStateFlow<List<LlmModel>>(emptyList())
    val availableModels: StateFlow<List<LlmModel>> = _availableModels.asStateFlow()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<ChatUiEvent>()
    val events: SharedFlow<ChatUiEvent> = _events.asSharedFlow()

    private val _cloudSendRequest = MutableStateFlow<CloudSendRequest?>(null)
    val cloudSendRequest: StateFlow<CloudSendRequest?> = _cloudSendRequest.asStateFlow()
    private var engineCloseJob: Job? = null
    private var generationJob: Job? = null
    private var activeGenerationToken: Long = 0L
    private var pendingWhatsAppExecution: PendingWhatsAppExecution? = null
    private var pendingReminderExecution: ReminderReviewRequest? = null
    private val _reminderReview = MutableStateFlow<ReminderReviewRequest?>(null)
    val reminderReview = _reminderReview.asStateFlow()
    val reminders = reminderScheduler.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissReminderReview() { _reminderReview.value = null }

    fun cancelReminder(id: String) {
        viewModelScope.launch {
            try {
                reminderScheduler.cancel(id)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(loadError = "Couldn't remove the reminder. Please try again.") }
            }
        }
    }

    fun confirmReminder(draft: ReminderDraft, useLocalReminder: Boolean) {
        val reviewed = _reminderReview.value?.copy(draft = draft, useLocalReminder = useLocalReminder) ?: return
        _reminderReview.value = null
        viewModelScope.launch {
            try {
                if (useLocalReminder) {
                    scheduleReviewedReminder(reviewed)
                } else {
                    val result = automationExecutor.execute(AutomationRequest.CreateReminder(draft.title), draft)
                    if (result !is AutomationExecutionResult.Launched) _reminderReview.value = reviewed
                    handleIntentResult(
                        result,
                        onLaunched = { _uiState.update { it.copy(loadError = null, infoMessage = "Calendar opened. Save the event there to finish.") } },
                        onPermissionRequired = { result -> _uiState.update { it.copy(loadError = result.message) } },
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _reminderReview.value = reviewed
                _uiState.update { it.copy(loadError = "Couldn't save the reminder. Please try again.") }
            }
        }
    }

    private suspend fun scheduleReviewedReminder(reviewed: ReminderReviewRequest, requestPermission: Boolean = true) {
        when (val result = reminderScheduler.schedule(reviewed.draft)) {
            AutomationExecutionResult.Launched -> {
                _uiState.update { it.copy(loadError = null, infoMessage = "Reminder saved in Orbit for ${formatReminderTime(reviewed.draft.startTimeMillis)}${if (reminderScheduler.exactAlarmsEnabled()) "." else " (approximate; enable Alarms & reminders for precise timing)."}") }
            }
            is AutomationExecutionResult.Failed -> {
                _reminderReview.value = reviewed
                _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
            }
            is AutomationExecutionResult.PermissionRequired -> {
                _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                if (requestPermission) {
                    pendingReminderExecution = reviewed
                    _events.emit(ChatUiEvent.RequestNotificationsPermission)
                } else {
                    _reminderReview.value = reviewed
                }
            }
        }
    }

    fun editReminder(reminder: com.example.orbitai.core.database.ReminderEntity? = null) {
        val start = reminder?.triggerAt ?: System.currentTimeMillis() + 3_600_000L
        _uiState.update { it.copy(loadError = null, infoMessage = null) }
        _reminderReview.value = ReminderReviewRequest("", ReminderDraft(
            reminder?.title.orEmpty(), reminder?.description.orEmpty(), start, start + 1_800_000L,
            reminder?.id, reminder?.repeat ?: "NONE",
        ), true)
    }

    fun snoozeReminder(id: String) = updateReminder { reminderScheduler.snooze(id) }
    fun completeReminder(id: String) = updateReminder { reminderScheduler.complete(id) }
    fun refreshReminders() = updateReminder { reminderScheduler.restore() }
    private fun updateReminder(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action(); _uiState.update { it.copy(loadError = null) } }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(loadError = "Couldn't update the reminder. Please try again.") }
            }
        }
    }

    private val _callReview = MutableStateFlow<CallReviewRequest?>(null)
    val callReview = _callReview.asStateFlow()
    private var pendingCallLookup: Pair<String, String>? = null
    private var pendingCall: com.example.orbitai.feature.automation.executor.CallContact? = null

    fun dismissCallReview() { _callReview.value = null; pendingCall = null }

    private fun resolveCall(chatId: String, recipient: String) {
        val app = getApplication<Application>()
        val number = com.example.orbitai.feature.automation.executor.callableNumber(recipient)
        if (number != null) {
            _callReview.value = CallReviewRequest(chatId, listOf(com.example.orbitai.feature.automation.executor.CallContact(recipient, number)))
            return
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(app, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pendingCallLookup = chatId to recipient
            viewModelScope.launch { _events.emit(ChatUiEvent.RequestCallContactsPermission) }
            return
        }
        viewModelScope.launch {
            try {
                val contacts = withContext(Dispatchers.IO) {
                    com.example.orbitai.feature.automation.executor.ContactResolver(app).findCallContacts(recipient)
                }
                if (contacts.isEmpty()) _uiState.update { it.copy(loadError = "No phone number found for $recipient. Try the saved contact name or a phone number.") }
                else _callReview.value = CallReviewRequest(chatId, contacts)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(loadError = "Couldn't read contacts. Check contacts permission in Android Settings.") }
            }
        }
    }

    fun onCallContactsPermissionResult(granted: Boolean) {
        val lookup = pendingCallLookup ?: return
        pendingCallLookup = null
        if (granted) resolveCall(lookup.first, lookup.second)
        else _uiState.update { it.copy(loadError = "Contacts permission was denied. You can call using a phone number instead.") }
    }

    fun confirmCall(contact: com.example.orbitai.feature.automation.executor.CallContact, dialOnly: Boolean = false) {
        if (_callReview.value?.contacts?.contains(contact) != true) return
        val app = getApplication<Application>()
        if (!dialOnly && contact.number.count(Char::isDigit) >= 7 &&
            androidx.core.content.ContextCompat.checkSelfPermission(app, android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pendingCall = contact
            viewModelScope.launch { _events.emit(ChatUiEvent.RequestCallPermission) }
            return
        }
        val error = com.example.orbitai.feature.automation.executor.CallExecutor(app).launch(contact, dialOnly)
        _uiState.update { it.copy(loadError = error, infoMessage = if (error == null) "Phone app opened for ${contact.name}." else null) }
        if (error == null) dismissCallReview()
    }

    fun onCallPermissionResult(granted: Boolean) {
        val contact = pendingCall ?: return
        pendingCall = null
        if (granted) confirmCall(contact)
        else _uiState.update { it.copy(loadError = "Phone permission was denied. Choose Open dialer to continue manually.") }
    }

    private fun handleDeviceCommand(chatId: String, text: String): Boolean {
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
                        resolveCall(chatId, command.recipient)
                        "Choose and confirm the phone number to call."
                    }
                    is com.example.orbitai.feature.automation.parser.DeviceCommand.Remember -> {
                        if (memoryRepo.addMemory(command.content, source = "manual"))
                            "Saved to Orbit Memory: ${command.content}"
                        else "Memory is disabled. Enable it in Settings > Memory, then ask me to remember this again."
                    }
                    null -> {
                        val draft = com.example.orbitai.feature.automation.parser.LocalReminderParser.parse(reminder!!.topicHint)
                        _reminderReview.value = ReminderReviewRequest(chatId, draft, true)
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

    private val _notificationReplyDraft = MutableStateFlow(NotificationReplyDraftState())
    val notificationReplyDraft = _notificationReplyDraft.asStateFlow()
    private var notificationGenerationToken: Long? = null

    fun clearNotificationReplyDraft() {
        if (notificationGenerationToken == activeGenerationToken) stopGeneration()
        notificationGenerationToken = null
        _notificationReplyDraft.value = NotificationReplyDraftState()
    }

    fun draftNotificationReply(preview: com.example.orbitai.feature.automation.replies.ReplyNotification, instruction: String) {
        if (instruction.isBlank()) return
        val model = com.example.orbitai.feature.automation.replies.selectLocalReplyModel(
            availableChatModels(modelDownloader, tokenStore), tokenStore.lastSelectedModelId,
        )
        if (model == null) {
            _notificationReplyDraft.value = NotificationReplyDraftState(preview.key, preview.revision,
                error = "Download a local model in Settings > Model, or write your reply below.")
            return
        }
        stopGeneration()
        val closingEngine = engineCloseJob
        val token = beginNewGenerationToken()
        notificationGenerationToken = token
        _notificationReplyDraft.value = NotificationReplyDraftState(preview.key, preview.revision, loading = true)
        generationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                closingEngine?.join()
                val settings = com.example.orbitai.core.common.InferenceSettings(maxDecodedTokens = 256)
                llmRepo.loadModel(model, settings)
                val prompt = ModelPromptBuilder.wrapInstructionPrompt(model.promptStyle,
                    com.example.orbitai.feature.automation.replies.ReplyDraftPrompt.build(preview.message, instruction))
                var text = ""
                llmRepo.generateResponseStream(InferenceInput(prompt), settings.maxDecodedTokens).collect { part ->
                    if (!isGenerationTokenActive(token)) throw CancellationException("Stale reply draft")
                    text += part
                }
                if (isGenerationTokenActive(token)) {
                    _notificationReplyDraft.value = NotificationReplyDraftState(preview.key, preview.revision,
                        text = text.trim().take(4000), error = if (text.isBlank()) "No draft was generated. Try again or write a reply." else null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (isGenerationTokenActive(token)) {
                    _notificationReplyDraft.value = NotificationReplyDraftState(preview.key, preview.revision,
                        error = "Couldn't generate a local draft. Try again or write your reply.")
                }
            } finally {
                if (isGenerationTokenActive(token)) generationJob = null
            }
        }
    }

    private val reminderTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")

    init {
        refreshAvailableModels()
        viewModelScope.launch {
            try {
                reminderScheduler.restore()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(loadError = "Couldn't restore saved reminders. Reopen Orbit to retry.") }
            }
        }
        viewModelScope.launch {
            modes.collect { activeModes ->
                if (activeModes.none { it.id == _activeModeId.value }) {
                    _activeModeId.value = activeModes.firstOrNull()?.id ?: ORBIT_MODE_ID
                }
            }
        }
    }

    fun refreshAvailableModels() {
        _availableModels.value = availableChatModels(modelDownloader, tokenStore)
    }

    private fun beginNewGenerationToken(): Long {
        activeGenerationToken += 1
        return activeGenerationToken
    }

    private fun isGenerationTokenActive(token: Long): Boolean = activeGenerationToken == token

    fun stopGeneration() {
        beginNewGenerationToken()
        val stoppedGeneration = generationJob
        stoppedGeneration?.cancel()
        generationJob = null
        val previousClose = engineCloseJob
        engineCloseJob = viewModelScope.launch(Dispatchers.IO) {
            previousClose?.join()
            llmRepo.close()
            stoppedGeneration?.join()
            llmRepo.close()
        }
        _uiState.update { it.copy(isGenerating = false, isModelLoading = false) }
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
                handleIntentResult(
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

    fun onNotificationsPermissionResult(granted: Boolean) {
        val pending = pendingReminderExecution
        pendingReminderExecution = null

        if (!granted) {
            _reminderReview.value = pending
            _uiState.update {
                it.copy(
                    loadError = "Enable notifications in Android Settings, or choose calendar handoff.",
                    infoMessage = null,
                )
            }
            return
        }

        if (pending != null) {
            viewModelScope.launch {
                try {
                    scheduleReviewedReminder(pending, requestPermission = false)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _reminderReview.value = pending
                    _uiState.update { it.copy(loadError = "Couldn't save the reminder. Please try again.") }
                }
            }
        }
    }

    // ── Chat management ───────────────────────────────────────────────────────

    fun createNewChat(): String {
        // Navigation needs the ID synchronously; Room insert on IO is fast (< 1 ms).
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            chatRepo.findReusableEmptyChatId() ?: chatRepo.createChat().id
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) { chatRepo.deleteChat(chatId) }
    }

    fun selectModel(chatId: String, model: LlmModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAvailable = availableChatModels(modelDownloader, tokenStore)
            _availableModels.value = currentAvailable
            if (currentAvailable.none { it.id == model.id }) return@launch
            tokenStore.lastSelectedModelId = model.id
            chatRepo.updateChatModel(chatId, model.id)
        }
    }

    // ── Inference ─────────────────────────────────────────────────────────────

    fun onCloudSendConsent(granted: Boolean) {
        val request = _cloudSendRequest.value ?: return
        _cloudSendRequest.value = null
        if (granted) {
            sendMessage(request.chatId, request.text, request.imageUris, request.modelId)
        }
    }

    fun sendMessage(
        chatId: String,
        userText: String,
        imageUris: List<Uri> = emptyList(),
        approvedCloudModelId: String? = null,
    ) {
        viewModelScope.launch {
            val chat = chatRepo.getChat(chatId) ?: return@launch
            sendMessageForChat(chat, userText, imageUris, approvedCloudModelId)
        }
    }

    private fun sendMessageForChat(
        chat: Chat,
        userText: String,
        imageUris: List<Uri>,
        approvedCloudModelId: String?,
    ) {
        val chatId = chat.id
        val trimmedText = userText.trim().ifBlank {
            if (imageUris.isNotEmpty()) "Describe the attached image(s)." else ""
        }
        if (imageUris.isEmpty() && handleDeviceCommand(chatId, trimmedText)) return
        val currentAvailableModels = availableChatModels(modelDownloader, tokenStore)
        _availableModels.value = currentAvailableModels

        if (trimmedText.isEmpty()) return

        val route = AutomationRouter.route(trimmedText)
        val toolRequest = (route as? AutomationRoute.ToolOnly)?.request

        val preferredModelId = when {
            chat.modelId.isNotBlank() -> chat.modelId
            tokenStore.lastSelectedModelId.isNotBlank() -> tokenStore.lastSelectedModelId
            else -> ""
        }

        if (currentAvailableModels.isEmpty()) {
            _uiState.update {
                it.copy(loadError = "No available model found. Download one or configure Gemini in Settings > Model.")
            }
            return
        }

        val preferredModel = currentAvailableModels.find { it.id == preferredModelId }
        val model = when {
            preferredModelId.isBlank() -> currentAvailableModels.singleOrNull() ?: currentAvailableModels.firstOrNull()
            else -> preferredModel ?: currentAvailableModels.firstOrNull()
        } ?: run {
            _uiState.update {
                it.copy(
                    loadError = if (preferredModelId.isBlank()) {
                        "Select a model first from the model picker."
                    } else {
                        "Selected model is no longer available. Please choose another model."
                    }
                )
            }
            return
        }
        if (preferredModelId.isNotBlank() && preferredModel == null) {
            tokenStore.lastSelectedModelId = model.id
            _uiState.update {
                it.copy(
                    loadError = null,
                    infoMessage = "Switched to ${model.displayName} because the previous model is no longer available.",
                )
            }
        }
        if (imageUris.isNotEmpty() && !model.supportsVision) {
            _uiState.update {
                it.copy(loadError = "${model.displayName} does not support image input. Pick a vision-capable model.")
            }
            return
        }
        if (model.provider == ModelProvider.GEMINI && approvedCloudModelId != model.id) {
            _cloudSendRequest.value = CloudSendRequest(chatId, userText, imageUris.toList(), model.id, model.displayName)
            return
        }
        val previousGeneration = generationJob
        previousGeneration?.cancel()
        val closingEngine = engineCloseJob
        val activeModeId = _activeModeId.value
        val activeSpaceIds = _activeSpaceIds.value.toList()
        val activeMode = modeRepo.modes.value.find { it.id == activeModeId }
            ?: modeRepo.modes.value.find { it.isDefault }
        val settings = modeInferenceStore.get(activeModeId)
        val generationToken = beginNewGenerationToken()

        generationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                closingEngine?.join()
                if (previousGeneration != null) {
                    llmRepo.close()
                    previousGeneration.join()
                    llmRepo.close()
                }
                _uiState.update { it.copy(loadError = null, infoMessage = null) }

                if (chat.modelId != model.id) {
                    chatRepo.updateChatModel(chatId, model.id)
                }

                val memoryEnabled = memoryFeatureStore.isEnabled
            
                // Resolve image uris to safe persisted strings
                val uriStrings = imageUris.map { it.toString() }

                // 1. Add user message
                chatRepo.addMessage(chatId, Message(role = Role.USER, content = trimmedText, imageUris = uriStrings))

                // 1b. Auto-detect and save memorable facts from user message
                if (memoryEnabled) {
                    extractMemoryFacts(trimmedText).forEach { fact ->
                        memoryRepo.addMemory(fact, source = "auto")
                    }
                }

                // 2. Load model if settings or model changed
                if (!llmRepo.isModelLoaded(model.id, settings)) {
                    _uiState.update { it.copy(isModelLoading = true, loadError = null) }
                    try {
                        llmRepo.loadModel(model, settings)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(isModelLoading = false, loadError = "Failed to load model: ${e.message}")
                        }
                        return@launch
                    }
                    _uiState.update { it.copy(isModelLoading = false) }
                }

                // 3. Build prompt from history + RAG context + memories
                val history = chatRepo.getChat(chatId)?.messages ?: emptyList()
                val memories = if (memoryEnabled) {
                    memoryRepo.getAllMemories().map { it.content }
                } else {
                    emptyList()
                }
                val systemPrompt = activeMode?.systemPrompt
                val prompt = when (toolRequest) {
                    is AutomationRequest.DraftEmail -> ModelPromptBuilder.wrapInstructionPrompt(
                        promptStyle = model.promptStyle,
                        instruction = EmailDraftPromptBuilder.build(
                            messages = history,
                            topicHint = toolRequest.topicHint,
                            memories = memories,
                        ),
                    )
                    is AutomationRequest.DraftWhatsApp -> ModelPromptBuilder.wrapInstructionPrompt(
                        promptStyle = model.promptStyle,
                        instruction = WhatsAppDraftPromptBuilder.build(
                            messages = history,
                            topicHint = toolRequest.topicHint,
                            memories = memories,
                        ),
                    )
                    is AutomationRequest.CreateReminder -> ModelPromptBuilder.wrapInstructionPrompt(
                        promptStyle = model.promptStyle,
                        instruction = ReminderPromptBuilder.build(
                            messages = history,
                            topicHint = toolRequest.topicHint,
                            memories = memories,
                        ),
                    )
                    null -> {
                        val ragContext = spaceRepo.searchChunksInSpaces(
                            trimmedText,
                            activeSpaceIds,
                            limit = 5,
                        ).map { it.content }
                        ModelPromptBuilder.buildChatPrompt(
                            promptStyle = model.promptStyle,
                            messages = history,
                            ragContext = ragContext,
                            memories = memories,
                            systemPrompt = systemPrompt,
                            includeImageTokens = model.format == com.example.orbitai.core.model.ModelFormat.ONNX_GENAI && model.supportsVision,
                        )
                    }
                }
            
                // Build InferenceInput
                val bitmaps = mutableListOf<Bitmap>()
                for (uri in imageUris) {
                    try {
                        val source = ImageDecoder.createSource(getApplication<Application>().contentResolver, uri)
                        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // Models often require software bitmaps
                        }
                        bitmaps.add(bitmap)
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "Failed to decode image from uri: $uri", e)
                    }
                }
                val inferenceInput = InferenceInput(prompt = prompt, images = bitmaps)

                // 4. Add empty assistant message (streaming placeholder)
                val assistantMsg = Message(
                    role = Role.ASSISTANT,
                    content = "",
                    modeName = activeMode?.name ?: "Orbit",
                    isStreaming = true,
                )
                chatRepo.addMessage(chatId, assistantMsg)
                _uiState.update { it.copy(isGenerating = true) }

                // 5. Stream response
                var accumulated = ""
                var wasCancelled = false
                try {
                    llmRepo.generateResponseStream(inferenceInput, settings.maxDecodedTokens).collect { token ->
                        if (!isGenerationTokenActive(generationToken)) {
                            throw CancellationException("Stale generation request")
                        }
                        accumulated += token
                        chatRepo.updateMessage(assistantMsg.id, accumulated, isStreaming = true)
                    }
                } catch (e: CancellationException) {
                    wasCancelled = true
                } catch (e: Exception) {
                    accumulated = "Error: ${e.message}"
                } finally {
                    val isActiveRequest = isGenerationTokenActive(generationToken)
                    withContext(NonCancellable) {
                        chatRepo.updateMessage(assistantMsg.id, accumulated, isStreaming = false)
                    }
                    bitmaps.forEach { if (!it.isRecycled) it.recycle() }

                    if (isActiveRequest && !wasCancelled && !accumulated.startsWith("Error:")) {
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
                            is AutomationRequest.DraftWhatsApp -> {
                                val draft = WhatsAppDraftParser.parse(
                                    modelOutput = accumulated,
                                    topicHint = toolRequest.topicHint,
                                )
                                when (
                                    val result = automationExecutor.execute(
                                        request = toolRequest,
                                        draft = draft,
                                    )
                                ) {
                                    AutomationExecutionResult.Launched -> Unit
                                    is AutomationExecutionResult.Failed -> {
                                        _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                                    }
                                    is AutomationExecutionResult.PermissionRequired -> {
                                        if (result.permission == RuntimeToolPermission.CONTACTS) {
                                            pendingWhatsAppExecution = PendingWhatsAppExecution(
                                                request = toolRequest,
                                                draft = draft,
                                            )
                                            _events.emit(ChatUiEvent.RequestContactsPermission)
                                            _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                                        }
                                    }
                                }
                            }
                            is AutomationRequest.CreateReminder -> {
                                val draft = ReminderDraftParser.parse(
                                    modelOutput = accumulated,
                                    topicHint = toolRequest.topicHint,
                                )
                                _reminderReview.value = ReminderReviewRequest(
                                    chatId, draft, true,
                                )
                            }
                            null -> Unit
                        }
                    }

                    if (isActiveRequest) {
                        _uiState.update { it.copy(isGenerating = false) }
                        generationJob = null
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isGenerationTokenActive(generationToken)) {
                    _uiState.update { it.copy(loadError = "Unable to process message: ${e.message}") }
                }
            } finally {
                if (isGenerationTokenActive(generationToken)) {
                    _uiState.update { it.copy(isGenerating = false, isModelLoading = false) }
                    generationJob = null
                }
            }
        }
    }

    /**
     * Pattern-based extraction of memorable personal facts from user messages.
     * Returns a list of human-readable fact strings to persist in memory.
     */
    private fun extractMemoryFacts(text: String): List<String> {
        val t = text.trim()
        val facts = mutableListOf<String>()

        val patterns = listOf(
            Regex("(?i)my name is ([\\w\\s]+)", RegexOption.IGNORE_CASE)             to { m: MatchResult -> "User's name is ${m.groupValues[1].trim()}" },
            Regex("(?i)i(?:'m| am) ([\\w\\s]+?) years old")                          to { m: MatchResult -> "User is ${m.groupValues[1].trim()} years old" },
            Regex("(?i)i(?:'m| am) a ([\\w\\s]+)")                                   to { m: MatchResult -> "User is a ${m.groupValues[1].trim()}" },
            Regex("(?i)i work (?:at|for|in) ([\\w\\s]+)")                            to { m: MatchResult -> "User works at ${m.groupValues[1].trim()}" },
            Regex("(?i)i(?:'m| am) (?:based in|from|living in|located in) ([\\w\\s,]+)") to { m: MatchResult -> "User is from/lives in ${m.groupValues[1].trim()}" },
            Regex("(?i)i live(?:s)? in ([\\w\\s,]+)")                                to { m: MatchResult -> "User lives in ${m.groupValues[1].trim()}" },
            Regex("(?i)i (?:love|really like|enjoy|prefer) ([\\w\\s]+)")             to { m: MatchResult -> "User likes/loves ${m.groupValues[1].trim()}" },
            Regex("(?i)i (?:hate|dislike|don't like|do not like) ([\\w\\s]+)")       to { m: MatchResult -> "User dislikes ${m.groupValues[1].trim()}" },
            Regex("(?i)(?:remember(?: that)?|don'?t forget)[:\\s]+(.+)")             to { m: MatchResult -> m.groupValues.getOrNull(1)?.trim().orEmpty() },
            Regex("(?i)(?:keep in mind)[:\\s]+(.+)")                                 to { m: MatchResult -> m.groupValues[1].trim() },
            Regex("(?i)my (?:favourite|favorite) ([\\w\\s]+) is ([\\w\\s]+)")        to { m: MatchResult -> "User's favorite ${m.groupValues[1].trim()} is ${m.groupValues[2].trim()}" },
        )

        for ((regex, transform) in patterns) {
            val match = regex.find(t)
            if (match != null) {
                val fact = transform(match)
                if (fact.isNotBlank() && fact.length < 200) {
                    facts += fact
                }
            }
        }
        return facts
    }

    override fun onCleared() {
        super.onCleared()
        llmRepo.close()
    }

    private fun handleIntentResult(
        result: AutomationExecutionResult,
        onLaunched: (() -> Unit)?,
        onPermissionRequired: (AutomationExecutionResult.PermissionRequired) -> Unit,
    ) {
        when (result) {
            AutomationExecutionResult.Launched -> onLaunched?.invoke()
            is AutomationExecutionResult.Failed -> {
                _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
            }
            is AutomationExecutionResult.PermissionRequired -> onPermissionRequired(result)
        }
    }

    private fun formatReminderTime(timeMillis: Long): String {
        return Instant.ofEpochMilli(timeMillis)
            .atZone(ZoneId.systemDefault())
            .format(reminderTimeFormatter)
    }
}
