package com.example.orbitai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.data.AVAILABLE_MODELS
import com.example.orbitai.data.Chat
import com.example.orbitai.data.ChatRepository
import com.example.orbitai.data.InferenceSettingsStore
import com.example.orbitai.data.LlmModel
import com.example.orbitai.data.LlmRepository
import com.example.orbitai.data.MemoryFeatureStore
import com.example.orbitai.data.Message
import com.example.orbitai.data.Role
import com.example.orbitai.data.ModeRepository
import com.example.orbitai.data.ModelDownloader
import com.example.orbitai.data.SpaceRepository
import com.example.orbitai.data.db.Mode
import com.example.orbitai.data.db.ORBIT_MODE_ID
import com.example.orbitai.data.db.Space
import com.example.orbitai.data.memory.MemoryRepository
import com.example.orbitai.prompts.GemmaChatPromptBuilder
import com.example.orbitai.tools.intents.EmailDraftParser
import com.example.orbitai.tools.intents.IntentToolExecutionResult
import com.example.orbitai.tools.intents.IntentToolExecutor
import com.example.orbitai.tools.intents.IntentToolRequest
import com.example.orbitai.tools.intents.RuntimeToolPermission
import com.example.orbitai.tools.intents.WhatsAppDraftParser
import com.example.orbitai.tools.prompts.EmailDraftPromptBuilder
import com.example.orbitai.tools.prompts.WhatsAppDraftPromptBuilder
import com.example.orbitai.tools.router.ToolRoute
import com.example.orbitai.tools.router.ToolRouter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
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
)

sealed interface ChatUiEvent {
    data object RequestContactsPermission : ChatUiEvent
}

private data class PendingWhatsAppExecution(
    val request: IntentToolRequest.DraftWhatsApp,
    val draft: com.example.orbitai.tools.intents.WhatsAppDraft,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val chatRepo = ChatRepository(application)
    private val llmRepo = LlmRepository(application)
    private val modelDownloader = ModelDownloader(application)
    private val settingsStore = InferenceSettingsStore(application)
    private val spaceRepo = SpaceRepository(application)
    private val modeRepo = ModeRepository(application)
    private val memoryFeatureStore = MemoryFeatureStore(application)
    val memoryRepo = MemoryRepository(application)
    private val intentToolExecutor = IntentToolExecutor(application)

    /** All available spaces — observed by the chat screen for the space selector. */
    val spaces: StateFlow<List<Space>> = spaceRepo.spaces

    /** All available modes — observed by the chat screen for the mode selector. */
    val modes: StateFlow<List<Mode>> = modeRepo.modes

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

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<ChatUiEvent>()
    val events: SharedFlow<ChatUiEvent> = _events.asSharedFlow()

    private var generationJob: Job? = null
    private var pendingWhatsAppExecution: PendingWhatsAppExecution? = null

    fun stopGeneration() {
        generationJob?.cancel()
    }

    fun onContactsPermissionResult(granted: Boolean) {
        val pending = pendingWhatsAppExecution
        pendingWhatsAppExecution = null

        if (!granted) {
            _uiState.update {
                it.copy(loadError = "Contacts permission is required to use WhatsApp by contact name.")
            }
            return
        }

        if (pending != null) {
            viewModelScope.launch(Dispatchers.IO) {
                handleIntentResult(
                    intentToolExecutor.execute(
                        request = pending.request,
                        draft = pending.draft,
                    ),
                    onPermissionRequired = { permissionResult ->
                        _uiState.update { it.copy(loadError = permissionResult.message) }
                    },
                )
            }
        }
    }

    // ── Chat management ───────────────────────────────────────────────────────

    fun createNewChat(): String {
        // Navigation needs the ID synchronously; Room insert on IO is fast (< 1 ms).
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            chatRepo.createChat().id
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) { chatRepo.deleteChat(chatId) }
    }

    fun selectModel(chatId: String, model: LlmModel) {
        viewModelScope.launch(Dispatchers.IO) { chatRepo.updateChatModel(chatId, model.id) }
    }

    // ── Inference ─────────────────────────────────────────────────────────────

    fun sendMessage(chatId: String, userText: String) {
        val chat = chatRepo.chats.value.find { it.id == chatId } ?: return
        val trimmedText = userText.trim()

        if (trimmedText.isEmpty()) return

        val route = ToolRouter.route(trimmedText)
        val toolRequest = (route as? ToolRoute.ToolOnly)?.request

        val selectedModel = AVAILABLE_MODELS.find { it.id == chat.modelId }
        val model = when {
            selectedModel != null && modelDownloader.isDownloaded(selectedModel) -> selectedModel
            else -> AVAILABLE_MODELS.firstOrNull { modelDownloader.isDownloaded(it) }
        } ?: run {
            _uiState.update {
                it.copy(loadError = "No downloaded model found. Go to Settings > Model and download one.")
            }
            return
        }
        val settings = settingsStore.get()

        generationJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(loadError = null) }

            if (chat.modelId != model.id) {
                chatRepo.updateChatModel(chatId, model.id)
            }

            val memoryEnabled = memoryFeatureStore.isEnabled

            // 1. Add user message
            chatRepo.addMessage(chatId, Message(role = Role.USER, content = trimmedText))

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
            val systemPrompt = modeRepo.modes.value
                .find { it.id == _activeModeId.value }
                ?.systemPrompt
                ?: modeRepo.modes.value.find { it.isDefault }?.systemPrompt
            val prompt = when (toolRequest) {
                is IntentToolRequest.DraftEmail -> EmailDraftPromptBuilder.build(
                    messages = history,
                    topicHint = toolRequest.topicHint,
                    memories = memories,
                )
                is IntentToolRequest.DraftWhatsApp -> WhatsAppDraftPromptBuilder.build(
                    messages = history,
                    topicHint = toolRequest.topicHint,
                    memories = memories,
                )
                null -> {
                    val ragContext = spaceRepo.searchChunksInSpaces(
                        trimmedText,
                        _activeSpaceIds.value.toList(),
                        limit = 5,
                    ).map { it.content }
                    GemmaChatPromptBuilder.build(history, ragContext, memories, systemPrompt)
                }
            }

            // 4. Add empty assistant message (streaming placeholder)
            val assistantMsg = Message(role = Role.ASSISTANT, content = "", isStreaming = true)
            chatRepo.addMessage(chatId, assistantMsg)
            _uiState.update { it.copy(isGenerating = true) }

            // 5. Stream response
            var accumulated = ""
            try {
                llmRepo.generateResponseStream(prompt, settings.maxDecodedTokens).collect { token ->
                    accumulated += token
                    chatRepo.updateLastMessage(chatId, accumulated, isStreaming = true)
                }
            } catch (e: CancellationException) {
                // Stopped by user — keep accumulated partial text, re-throw for coroutine framework
                throw e
            } catch (e: Exception) {
                accumulated = "Error: ${e.message}"
            } finally {
                chatRepo.updateLastMessage(chatId, accumulated, isStreaming = false)

                if (!accumulated.startsWith("Error:")) {
                    when (toolRequest) {
                        is IntentToolRequest.DraftEmail -> {
                            when (
                                val result = intentToolExecutor.execute(
                                    request = toolRequest,
                                    draft = EmailDraftParser.parse(
                                        modelOutput = accumulated,
                                        topicHint = toolRequest.topicHint,
                                    ),
                                )
                            ) {
                                IntentToolExecutionResult.Launched -> Unit
                                is IntentToolExecutionResult.Failed -> {
                                    _uiState.update { it.copy(loadError = result.message) }
                                }
                                is IntentToolExecutionResult.PermissionRequired -> {
                                    _uiState.update { it.copy(loadError = result.message) }
                                }
                            }
                        }
                        is IntentToolRequest.DraftWhatsApp -> {
                            val draft = WhatsAppDraftParser.parse(
                                modelOutput = accumulated,
                                topicHint = toolRequest.topicHint,
                            )
                            when (
                                val result = intentToolExecutor.execute(
                                    request = toolRequest,
                                    draft = draft,
                                )
                            ) {
                                IntentToolExecutionResult.Launched -> Unit
                                is IntentToolExecutionResult.Failed -> {
                                    _uiState.update { it.copy(loadError = result.message) }
                                }
                                is IntentToolExecutionResult.PermissionRequired -> {
                                    if (result.permission == RuntimeToolPermission.CONTACTS) {
                                        pendingWhatsAppExecution = PendingWhatsAppExecution(
                                            request = toolRequest,
                                            draft = draft,
                                        )
                                        _events.emit(ChatUiEvent.RequestContactsPermission)
                                        _uiState.update { it.copy(loadError = result.message) }
                                    }
                                }
                            }
                        }
                        null -> Unit
                    }
                }

                _uiState.update { it.copy(isGenerating = false) }
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
        result: IntentToolExecutionResult,
        onPermissionRequired: (IntentToolExecutionResult.PermissionRequired) -> Unit,
    ) {
        when (result) {
            IntentToolExecutionResult.Launched -> Unit
            is IntentToolExecutionResult.Failed -> {
                _uiState.update { it.copy(loadError = result.message) }
            }
            is IntentToolExecutionResult.PermissionRequired -> onPermissionRequired(result)
        }
    }
}