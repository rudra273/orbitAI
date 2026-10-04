package com.example.orbitai.viewmodel

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.example.orbitai.core.common.ModeInferenceSettingsStore
import com.example.orbitai.core.common.TokenStore
import com.example.orbitai.core.database.ORBIT_MODE_ID
import com.example.orbitai.core.engine.InferenceInput
import com.example.orbitai.core.engine.StructuredPrompt
import com.example.orbitai.core.engine.TurnResult
import com.example.orbitai.core.model.DeviceFit
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.ModelFit
import com.example.orbitai.core.model.ModelProvider
import com.example.orbitai.core.model.availableChatModels
import com.example.orbitai.core.prompt.BudgetedPrompt
import com.example.orbitai.core.prompt.ModelPromptBuilder
import com.example.orbitai.core.prompt.PromptBudget
import com.example.orbitai.feature.automation.AutomationRoute
import com.example.orbitai.feature.automation.AutomationRouter
import com.example.orbitai.feature.automation.parser.AutomationRequest
import com.example.orbitai.feature.automation.tools.ChatToolAction
import com.example.orbitai.feature.automation.tools.ChatToolActions
import com.example.orbitai.feature.automation.tools.OrbitChatToolSchema
import com.example.orbitai.feature.automation.prompt.EmailDraftPromptBuilder
import com.example.orbitai.feature.automation.prompt.ReminderPromptBuilder
import com.example.orbitai.feature.automation.prompt.WhatsAppDraftPromptBuilder
import com.example.orbitai.feature.chat.Chat
import com.example.orbitai.feature.chat.ChatTitles
import com.example.orbitai.feature.chat.GenerationStats
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import com.example.orbitai.feature.memory.MemorySuggester
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
    private val deviceSpecs = com.example.orbitai.core.model.DeviceSpecs(ctx.app)

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
    /** Background title generation after a chat's first reply; cancelled by any new run. */
    private var titleJob: Job? = null
    private val chatSettings = com.example.orbitai.feature.chat.ChatSettingsStore(ctx.app)

    private val _generationStats = MutableStateFlow<Map<String, GenerationStats>>(emptyMap())
    /** Stats of replies generated in this session, by message id. */
    val generationStats: StateFlow<Map<String, GenerationStats>> = _generationStats.asStateFlow()
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
        titleJob?.cancel()
        beginNewGenerationToken()
        val stoppedGeneration = generationJob
        stoppedGeneration?.cancel()
        generationJob = null
        val previousClose = engineCloseJob
        engineCloseJob = viewModelScope.launch(Dispatchers.IO) {
            previousClose?.join()
            awaitStopped(stoppedGeneration)
        }
        _uiState.update { it.copy(isGenerating = false, isModelLoading = false) }
    }

    /**
     * Waits for a cancelled run to finish. Engines stop on cancellation (LiteRT via cancelProcess,
     * ONNX per token, MediaPipe/Gemini by closing their session or stream), so the model stays
     * loaded for the next message. If a run is stuck in native code, unload the model as a fallback.
     */
    private suspend fun awaitStopped(job: Job?) {
        if (job == null) return
        val stopped = kotlinx.coroutines.withTimeoutOrNull(STOP_GRACE_MS) { job.join() }
        if (stopped == null) {
            android.util.Log.w("ChatGeneration", "Generation didn't stop within ${STOP_GRACE_MS}ms; unloading model")
            llmRepo.close()
            job.join()
        }
    }

    /**
     * Stops any reply in progress and runs [block] as the only generation, holding a model lease.
     * Returns the new generation token.
     */
    fun launchExclusive(block: suspend CoroutineScope.(token: Long) -> Unit): Long {
        val previousTitle = titleJob
        stopGeneration()
        val closingEngine = engineCloseJob
        val token = beginNewGenerationToken()
        generationJob = viewModelScope.launch(Dispatchers.IO) {
            llmRepo.beginUse()
            try {
                closingEngine?.join()
                previousTitle?.join()
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
        val previousTitle = titleJob
        previousTitle?.cancel()
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
                awaitStopped(previousGeneration)
                previousTitle?.join()
                _uiState.update { it.copy(loadError = null, infoMessage = null) }

                if (chat.modelId != model.id) {
                    chatRepo.updateChatModel(chatId, model.id)
                }

                val memoryEnabled = memoryFeatureStore.isEnabled
            
                // Resolve image uris to safe persisted strings
                val uriStrings = imageUris.map { it.toString() }

                // 1. Add user message (already present when regenerating or resending an edit)
                if (!resendExisting) {
                    val userMessage = Message(role = Role.USER, content = trimmedText, imageUris = uriStrings)
                    chatRepo.addMessage(chatId, userMessage)
                    // 1b. Offer to remember a personal fact from the message; nothing is saved without a tap.
                    suggestMemory(chatId, userMessage.id, trimmedText, memoryEnabled)
                }

                // 2. Load model if settings or model changed
                if (!llmRepo.isModelLoaded(model.id, settings)) {
                    if (DeviceFit.fitOf(model, deviceSpecs.totalRamBytes) == ModelFit.TOO_LARGE) {
                        _uiState.update {
                            it.copy(infoMessage = "${model.displayName} may be too large for this phone's memory. If Orbit closes, pick a smaller model in Settings > Models.")
                        }
                    }
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
                    memoryRepo.selectForPrompt(trimmedText).map { it.content }
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
                var usedMemories = memories
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
                    usedMemories = budgeted.memories
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
                        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // Models often require software bitmaps
                            // Camera photos are 12+ MP; models only use a small input, so downscale to save memory.
                            val scale = MAX_IMAGE_SIDE.toFloat() / maxOf(info.size.width, info.size.height)
                            if (scale < 1f) {
                                decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                            }
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
                    usedMemories = usedMemories,
                )
                chatRepo.addMessage(chatId, assistantMsg)
                _uiState.update { it.copy(isGenerating = true) }

                // 5. Stream response. Plain chat on LiteRT models also offers phone tools the model can call.
                var accumulated = ""
                var wasCancelled = false
                var toolAction: ChatToolAction? = null
                val streamStartedAt = System.currentTimeMillis()
                var firstTokenAt: Long? = null
                try {
                    val onToken: suspend (String) -> Unit = { token ->
                        if (!isGenerationTokenActive(generationToken)) {
                            throw CancellationException("Stale generation request")
                        }
                        if (firstTokenAt == null) firstTokenAt = System.currentTimeMillis()
                        accumulated += token
                        chatRepo.updateMessage(assistantMsg.id, accumulated, isStreaming = true)
                    }
                    val toolTurn = if (toolRequest == null && bitmaps.isEmpty()) {
                        streamWithTools(structuredPrompt, settings, onToken)
                    } else null
                    if (toolTurn != null) {
                        toolAction = toolTurn.toolCalls.firstNotNullOfOrNull { ChatToolActions.from(it.name, it.arguments) }
                        if (accumulated.isBlank()) accumulated = toolTurn.text
                        if (toolAction == null && accumulated.isBlank() && toolTurn.toolCalls.isNotEmpty()) {
                            accumulated = "I couldn't complete that action. Try rephrasing, or use the / menu."
                        }
                    } else {
                        llmRepo.generateResponseStream(inferenceInput, settings.maxDecodedTokens).collect { onToken(it) }
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
                        val finishedAt = System.currentTimeMillis()
                        _generationStats.update {
                            it + (assistantMsg.id to GenerationStats(
                                firstTokenMs = firstTokenAt?.let { first -> first - streamStartedAt },
                                streamingMs = firstTokenAt?.let { first -> finishedAt - first } ?: 0,
                                approxTokens = GenerationStats.estimateTokens(accumulated),
                                backend = if (model.provider == ModelProvider.GEMINI) "Cloud" else llmRepo.activeBackendLabel(),
                                modelName = model.displayName,
                            ))
                        }
                        if (toolRequest == null && toolAction == null) {
                            maybeGenerateTitle(chatId, model, settings, accumulated)
                        }
                        val action = toolAction
                        if (action != null) automation.handleToolAction(chatId, assistantMsg.id, action)
                        else automation.handleDraftResult(chatId, toolRequest, accumulated)
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


    /**
     * Streams one reply through a LiteRT conversation that offers [OrbitChatToolSchema].
     * Returns null when the loaded model has no conversation support or can't start one
     * with tools, so the caller falls back to plain streaming.
     */
    private suspend fun streamWithTools(
        structured: StructuredPrompt,
        settings: com.example.orbitai.core.common.InferenceSettings,
        onToken: suspend (String) -> Unit,
    ): TurnResult? {
        val engine = llmRepo.conversationEngine() ?: return null
        val turns = structured.turns.filter { it.text.isNotBlank() }
        val latest = turns.lastOrNull()?.takeIf { it.isUser } ?: return null
        val session = try {
            engine.createConversation(
                systemInstruction = structured.systemInstruction + "\n\n" + ChatToolActions.INSTRUCTION,
                settings = settings,
                toolSchemas = listOf(OrbitChatToolSchema()),
                history = turns.dropLast(1),
            )
        } catch (e: Exception) {
            android.util.Log.w("ChatGeneration", "Tool conversation unavailable; using plain streaming", e)
            return null
        }
        return session.use {
            it.streamTurn(com.google.ai.edge.litertlm.Contents.of(latest.text), settings.maxDecodedTokens, onToken)
        }
    }

    /**
     * After a chat's first reply, asks the local model for a 3–6 word title. Runs as its own job
     * so a new message cancels it; the first-message title stays if it fails or is cancelled.
     */
    private fun maybeGenerateTitle(
        chatId: String,
        model: LlmModel,
        settings: com.example.orbitai.core.common.InferenceSettings,
        reply: String,
    ) {
        if (!chatSettings.smartTitles || model.provider == ModelProvider.GEMINI) return
        if (com.example.orbitai.feature.chat.ChatRepository.isTemporaryChat(chatId) || reply.isBlank()) return
        titleJob = viewModelScope.launch(Dispatchers.IO) {
            llmRepo.beginUse()
            try {
                val chat = chatRepo.getChat(chatId) ?: return@launch
                val userMessages = chat.messages.filter { it.role == Role.USER }
                if (userMessages.size != 1 || !chatRepo.canAutoTitle(chatId)) return@launch
                if (!llmRepo.isModelLoaded(model.id, settings)) return@launch
                val instruction = ChatTitles.prompt(userMessages.single().content, reply)
                val input = InferenceInput(
                    prompt = ModelPromptBuilder.wrapInstructionPrompt(model.promptStyle, instruction),
                    structured = ModelPromptBuilder.structuredInstruction(instruction),
                )
                val raw = StringBuilder()
                kotlinx.coroutines.withTimeoutOrNull(TITLE_TIMEOUT_MS) {
                    llmRepo.generateResponseStream(input, TITLE_MAX_TOKENS).collect { raw.append(it) }
                }
                ChatTitles.sanitize(raw.toString())?.let { chatRepo.setGeneratedTitle(chatId, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("ChatGeneration", "Title generation failed", e)
            } finally {
                llmRepo.endUse()
            }
        }
    }

    private suspend fun suggestMemory(chatId: String, messageId: String, text: String, memoryEnabled: Boolean) {
        // An unanswered suggestion goes away once the user moves on.
        _uiState.update { it.copy(memorySuggestion = null) }
        if (!memoryEnabled || !memoryFeatureStore.suggestionsEnabled) return
        if (com.example.orbitai.feature.chat.ChatRepository.isTemporaryChat(chatId)) return
        val fact = MemorySuggester.suggest(text) ?: return
        if (MemorySuggester.isDuplicate(fact, memoryRepo.getAllMemories().map { it.content })) return
        _uiState.update { it.copy(memorySuggestion = MemorySuggestion(chatId, messageId, fact)) }
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
        const val STOP_GRACE_MS = 10_000L
        const val MAX_IMAGE_SIDE = 1024
        const val TITLE_MAX_TOKENS = 16
        const val TITLE_TIMEOUT_MS = 20_000L
    }
}
