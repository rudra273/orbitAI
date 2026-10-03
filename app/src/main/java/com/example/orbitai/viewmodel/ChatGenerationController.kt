package com.example.orbitai.viewmodel

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.example.orbitai.core.common.ModeInferenceSettingsStore
import com.example.orbitai.core.common.TokenStore
import com.example.orbitai.core.database.ORBIT_MODE_ID
import com.example.orbitai.core.engine.InferenceInput
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.ModelProvider
import com.example.orbitai.core.model.availableChatModels
import com.example.orbitai.core.prompt.BudgetedPrompt
import com.example.orbitai.core.prompt.ModelPromptBuilder
import com.example.orbitai.core.prompt.PromptBudget
import com.example.orbitai.feature.automation.AutomationRoute
import com.example.orbitai.feature.automation.AutomationRouter
import com.example.orbitai.feature.automation.parser.AutomationRequest
import com.example.orbitai.feature.automation.prompt.EmailDraftPromptBuilder
import com.example.orbitai.feature.automation.prompt.ReminderPromptBuilder
import com.example.orbitai.feature.automation.prompt.WhatsAppDraftPromptBuilder
import com.example.orbitai.feature.chat.Chat
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import com.example.orbitai.feature.memory.MemoryFactExtractor
import com.example.orbitai.feature.memory.MemoryFeatureStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sends chat messages: model selection, prompt building (history, Spaces, memories, mode),
 * streaming into the assistant placeholder, stop, and the generation watchdog.
 * Owns the single generation slot, guarded by a monotonic token.
 */
internal class ChatGenerationController(private val ctx: ChatControllerContext) {

    private val _uiState get() = ctx.uiState
    private val _events get() = ctx.events
    private val viewModelScope get() = ctx.scope
    private val chatRepo = ctx.container.chatRepository
    private val llmRepo = ctx.container.llmRepository
    private val modelDownloader = ctx.container.modelDownloader
    private val spaceRepo = ctx.container.spaceRepository
    private val modeRepo = ctx.container.modeRepository
    private val memoryRepo = ctx.container.memoryRepository
    private val tokenStore = TokenStore(ctx.app)
    private val modeInferenceStore = ModeInferenceSettingsStore(ctx.app)
    private val memoryFeatureStore = MemoryFeatureStore(ctx.app)

    /** Set by [ChatViewModel] after construction (automation needs this controller's stop). */
    lateinit var automation: AutomationFlowController

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

    private val _availableModels = MutableStateFlow<List<LlmModel>>(emptyList())
    val availableModels: StateFlow<List<LlmModel>> = _availableModels.asStateFlow()

    private val _cloudSendRequest = MutableStateFlow<CloudSendRequest?>(null)
    val cloudSendRequest: StateFlow<CloudSendRequest?> = _cloudSendRequest.asStateFlow()
    private var engineCloseJob: Job? = null
    private var generationJob: Job? = null
    private var activeGenerationToken: Long = 0L

    init {
        refreshAvailableModels()
        viewModelScope.launch {
            modeRepo.activeModes.collect { activeModes ->
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

    fun isGenerationTokenActive(token: Long): Boolean = activeGenerationToken == token

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

    /**
     * Stops any reply in progress and runs [block] as the only generation, holding a model lease.
     * Returns the new generation token.
     */
    fun launchExclusive(block: suspend CoroutineScope.(token: Long) -> Unit): Long {
        stopGeneration()
        val closingEngine = engineCloseJob
        val token = beginNewGenerationToken()
        generationJob = viewModelScope.launch(Dispatchers.IO) {
            llmRepo.beginUse()
            try {
                closingEngine?.join()
                block(token)
            } finally {
                llmRepo.endUse()
                if (isGenerationTokenActive(token)) generationJob = null
            }
        }
        return token
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
            sendMessage(request.chatId, request.text, request.imageUris, request.modelId, request.resendExisting)
        }
    }

    private val isBusy: Boolean
        get() = generationJob?.isActive == true || _uiState.value.isGenerating || _uiState.value.isModelLoading

    /** Generates a new reply to the last user message, replacing the reply after it. */
    fun regenerate(chatId: String) {
        if (isBusy) return
        viewModelScope.launch {
            val chat = chatRepo.getChat(chatId) ?: return@launch
            val lastUser = chat.messages.lastOrNull { it.role == Role.USER } ?: return@launch
            if (lastUser.imageUris.isEmpty() && automation.isDeviceCommand(lastUser.content)) {
                _uiState.update { it.copy(infoMessage = "That reply came from a device action. Send the command again to repeat it.") }
                return@launch
            }
            chatRepo.deleteMessagesAfter(chatId, lastUser)
            sendMessage(chatId, lastUser.content, lastUser.imageUris.map(Uri::parse), resendExisting = true)
        }
    }

    /** Replaces a user message with [newText] and generates a new reply; later messages are removed. */
    fun editAndResend(chatId: String, messageId: String, newText: String) {
        val text = newText.trim()
        if (isBusy || text.isBlank()) return
        viewModelScope.launch {
            val chat = chatRepo.getChat(chatId) ?: return@launch
            val original = chat.messages.firstOrNull { it.id == messageId && it.role == Role.USER } ?: return@launch
            if (original.imageUris.isEmpty() && automation.isDeviceCommand(text)) {
                // Device commands add their own user message.
                chatRepo.deleteMessagesFrom(chatId, original)
                sendMessage(chatId, text)
                return@launch
            }
            chatRepo.updateMessageContent(original.id, text)
            chatRepo.deleteMessagesAfter(chatId, original)
            sendMessage(chatId, text, original.imageUris.map(Uri::parse), resendExisting = true)
        }
    }

    fun deleteMessage(messageId: String) {
        if (isBusy) return
        viewModelScope.launch { chatRepo.deleteMessage(messageId) }
    }

    fun sendMessage(
        chatId: String,
        userText: String,
        imageUris: List<Uri> = emptyList(),
        approvedCloudModelId: String? = null,
        resendExisting: Boolean = false,
    ) {
        viewModelScope.launch {
            val chat = chatRepo.getChat(chatId) ?: return@launch
            sendMessageForChat(chat, userText, imageUris, approvedCloudModelId, resendExisting)
        }
    }

    private fun sendMessageForChat(
        chat: Chat,
        userText: String,
        imageUris: List<Uri>,
        approvedCloudModelId: String?,
        resendExisting: Boolean = false,
    ) {
        val chatId = chat.id
        val trimmedText = userText.trim().ifBlank {
            if (imageUris.isNotEmpty()) "Describe the attached image(s)." else ""
        }
        if (!resendExisting && imageUris.isEmpty() && automation.handleDeviceCommand(chatId, trimmedText)) return
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
            _cloudSendRequest.value = CloudSendRequest(chatId, userText, imageUris.toList(), model.id, model.displayName, resendExisting)
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
        startGenerationWatchdog(generationToken)

        generationJob = viewModelScope.launch(Dispatchers.IO) {
            llmRepo.beginUse()
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

                // 1. Add user message (already present when regenerating or resending an edit)
                if (!resendExisting) {
                    chatRepo.addMessage(chatId, Message(role = Role.USER, content = trimmedText, imageUris = uriStrings))
                }

                // 1b. Auto-detect and save memorable facts from user message
                if (memoryEnabled && !resendExisting) {
                    MemoryFactExtractor.extract(trimmedText).forEach { fact ->
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
                val toolInstruction = when (toolRequest) {
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
                    null -> null
                }
                val (prompt, structuredPrompt) = if (toolInstruction != null) {
                    ModelPromptBuilder.wrapInstructionPrompt(model.promptStyle, toolInstruction) to
                        ModelPromptBuilder.structuredInstruction(toolInstruction)
                } else {
                    val ragContext = spaceRepo.searchChunksInSpaces(
                        trimmedText,
                        activeSpaceIds,
                        limit = 5,
                    ).map { it.content }
                    val budgeted = PromptBudget.fit(
                        messages = history,
                        ragContext = ragContext,
                        memories = memories,
                        systemPrompt = systemPrompt,
                        contextTokens = model.contextTokens,
                        reservedOutputTokens = settings.maxDecodedTokens,
                        imageCount = imageUris.size,
                    )
                    reportContextTrimming(chatId, budgeted)
                    ModelPromptBuilder.buildChatPrompt(
                        promptStyle = model.promptStyle,
                        messages = budgeted.messages,
                        ragContext = budgeted.ragContext,
                        memories = budgeted.memories,
                        systemPrompt = systemPrompt,
                        includeImageTokens = model.format == com.example.orbitai.core.model.ModelFormat.ONNX_GENAI && model.supportsVision,
                    ) to ModelPromptBuilder.buildStructuredChat(
                        messages = budgeted.messages,
                        ragContext = budgeted.ragContext,
                        memories = budgeted.memories,
                        systemPrompt = systemPrompt,
                    )
                }

                // Build InferenceInput
                val bitmaps = mutableListOf<Bitmap>()
                for (uri in imageUris) {
                    try {
                        val source = ImageDecoder.createSource(ctx.app.contentResolver, uri)
                        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // Models often require software bitmaps
                        }
                        bitmaps.add(bitmap)
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "Failed to decode image from uri: $uri", e)
                    }
                }
                val inferenceInput = InferenceInput(prompt = prompt, images = bitmaps, structured = structuredPrompt)

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
                        automation.handleDraftResult(chatId, toolRequest, accumulated)
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
                llmRepo.endUse()
                if (isGenerationTokenActive(generationToken)) {
                    _uiState.update { it.copy(isGenerating = false, isModelLoading = false) }
                    generationJob = null
                }
            }
        }
    }


    /** Stops a reply that runs longer than [MAX_GENERATION_MS] (e.g. stuck native inference). */
    private fun startGenerationWatchdog(token: Long) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(MAX_GENERATION_MS)
            if (isGenerationTokenActive(token) && generationJob?.isActive == true) {
                android.util.Log.w("ChatViewModel", "Generation exceeded ${MAX_GENERATION_MS / 60_000} min; stopping")
                stopGeneration()
                _uiState.update {
                    it.copy(loadError = "The reply took longer than ${MAX_GENERATION_MS / 60_000} minutes and was stopped. Try again or use a smaller model.")
                }
            }
        }
    }

    private val chatsWithTrimmedContext: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    private fun reportContextTrimming(chatId: String, budgeted: BudgetedPrompt) {
        val message = when {
            budgeted.latestMessageTruncated ->
                "Your message was too long for this model and was shortened. Try a model with a larger context, or split the message."
            budgeted.droppedMessageCount > 0 && chatsWithTrimmedContext.add(chatId) ->
                "Earlier messages in this chat no longer fit in the model's memory and aren't used for new replies."
            else -> return
        }
        _uiState.update { it.copy(infoMessage = message) }
    }

    private companion object {
        const val MAX_GENERATION_MS = 5 * 60_000L
    }
}
