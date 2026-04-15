package com.example.orbitai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.feature.chat.ChatRepository
import com.example.orbitai.core.common.ModeInferenceSettingsStore
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.feature.chat.Chat
import com.example.orbitai.feature.chat.LlmRepository
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
import com.example.orbitai.core.prompt.GemmaChatPromptBuilder
import com.example.orbitai.feature.automation.parser.EmailDraftParser
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

data class ChatUiState(
    val isModelLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val loadError: String? = null,
    val infoMessage: String? = null,
)

sealed interface ChatUiEvent {
    data object RequestContactsPermission : ChatUiEvent
    data object RequestNotificationsPermission : ChatUiEvent
}

private data class PendingWhatsAppExecution(
    val request: AutomationRequest.DraftWhatsApp,
    val draft: com.example.orbitai.feature.automation.parser.WhatsAppDraft,
)

private data class PendingReminderExecution(
    val draft: com.example.orbitai.feature.automation.parser.ReminderDraft,
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

    private var generationJob: Job? = null
    private var activeGenerationToken: Long = 0L
    private var pendingWhatsAppExecution: PendingWhatsAppExecution? = null
    private var pendingReminderExecution: PendingReminderExecution? = null
    private val reminderTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")

    init {
        refreshAvailableModels()
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
        generationJob?.cancel()
        generationJob = null
        // Force-stop any in-engine generation so next ask can start cleanly.
        viewModelScope.launch(Dispatchers.IO) {
            llmRepo.close()
        }
        _uiState.update { it.copy(isGenerating = false) }
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
            _uiState.update {
                it.copy(
                    loadError = "Notifications permission is required for automatic reminder execution.",
                    infoMessage = null,
                )
            }
            return
        }

        if (pending != null) {
            handleIntentResult(
                reminderScheduler.schedule(pending.draft),
                onLaunched = {
                    _uiState.update {
                        it.copy(
                            loadError = null,
                            infoMessage = "Reminder scheduled for ${formatReminderTime(pending.draft.startTimeMillis)}",
                        )
                    }
                },
                onPermissionRequired = { permissionResult ->
                    _uiState.update { it.copy(loadError = permissionResult.message, infoMessage = null) }
                },
            )
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

    fun sendMessage(chatId: String, userText: String, imageUris: List<Uri> = emptyList()) {
        if (generationJob?.isActive == true) {
            beginNewGenerationToken()
            generationJob?.cancel()
            generationJob = null
            // Ensure old backend stream is torn down before starting a new one.
            viewModelScope.launch(Dispatchers.IO) {
                llmRepo.close()
            }
        }

        val chat = chatRepo.chats.value.find { it.id == chatId } ?: return
        val trimmedText = userText.trim()
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

        if (preferredModelId.isBlank()) {
            _uiState.update {
                it.copy(loadError = "Select a model first from the model picker.")
            }
            return
        }

        val model = currentAvailableModels.find { it.id == preferredModelId } ?: run {
            _uiState.update {
                it.copy(loadError = "Selected model is no longer available. Please choose another model.")
            }
            return
        }
        val activeModeId = _activeModeId.value
        val activeSpaceIds = _activeSpaceIds.value.toList()
        val activeMode = modeRepo.modes.value.find { it.id == activeModeId }
            ?: modeRepo.modes.value.find { it.isDefault }
        val settings = modeInferenceStore.get(activeModeId)
        val generationToken = beginNewGenerationToken()

        generationJob = viewModelScope.launch(Dispatchers.IO) {
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
                is AutomationRequest.DraftEmail -> EmailDraftPromptBuilder.build(
                    messages = history,
                    topicHint = toolRequest.topicHint,
                    memories = memories,
                )
                is AutomationRequest.DraftWhatsApp -> WhatsAppDraftPromptBuilder.build(
                    messages = history,
                    topicHint = toolRequest.topicHint,
                    memories = memories,
                )
                is AutomationRequest.CreateReminder -> ReminderPromptBuilder.build(
                    messages = history,
                    topicHint = toolRequest.topicHint,
                    memories = memories,
                )
                null -> {
                    val ragContext = spaceRepo.searchChunksInSpaces(
                        trimmedText,
                        activeSpaceIds,
                        limit = 5,
                    ).map { it.content }
                    GemmaChatPromptBuilder.build(history, ragContext, memories, systemPrompt)
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
                if (isActiveRequest) {
                    chatRepo.updateMessage(assistantMsg.id, accumulated, isStreaming = false)
                }

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
                            if (automationSettingsStore.isAutomationExecutionEnabled) {
                                when (val result = reminderScheduler.schedule(draft)) {
                                    AutomationExecutionResult.Launched -> {
                                        _uiState.update {
                                            it.copy(
                                                loadError = null,
                                                infoMessage = "Reminder scheduled for ${formatReminderTime(draft.startTimeMillis)}",
                                            )
                                        }
                                    }
                                    is AutomationExecutionResult.Failed -> {
                                        _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                                    }
                                    is AutomationExecutionResult.PermissionRequired -> {
                                        if (result.permission == RuntimeToolPermission.NOTIFICATIONS) {
                                            pendingReminderExecution = PendingReminderExecution(draft)
                                            _events.emit(ChatUiEvent.RequestNotificationsPermission)
                                            _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                                        }
                                    }
                                }
                            } else {
                                when (
                                    val result = automationExecutor.execute(
                                        request = toolRequest,
                                        draft = draft,
                                    )
                                ) {
                                    AutomationExecutionResult.Launched -> {
                                        _uiState.update {
                                            it.copy(
                                                loadError = null,
                                                infoMessage = "Calendar opened for reminder on ${formatReminderTime(draft.startTimeMillis)}",
                                            )
                                        }
                                    }
                                    is AutomationExecutionResult.Failed -> {
                                        _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                                    }
                                    is AutomationExecutionResult.PermissionRequired -> {
                                        _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                                    }
                                }
                            }
                        }
                        null -> Unit
                    }
                }

                if (isActiveRequest) {
                    _uiState.update { it.copy(isGenerating = false) }
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
