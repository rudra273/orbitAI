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
import com.example.orbitai.data.AgentRepository
import com.example.orbitai.data.SpaceRepository
import com.example.orbitai.data.db.Agent
import com.example.orbitai.data.db.ORBIT_AGENT_ID
import com.example.orbitai.data.db.Space
import com.example.orbitai.data.memory.MemoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

data class ChatUiState(
    val isModelLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val loadError: String? = null,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val chatRepo = ChatRepository(application)
    private val llmRepo = LlmRepository(application)
    private val settingsStore = InferenceSettingsStore(application)
    private val spaceRepo = SpaceRepository(application)
    private val agentRepo = AgentRepository(application)
    private val memoryFeatureStore = MemoryFeatureStore(application)
    val memoryRepo = MemoryRepository(application)

    /** All available spaces — observed by the chat screen for the space selector. */
    val spaces: StateFlow<List<Space>> = spaceRepo.spaces

    /** All available agents — observed by the chat screen for the agent selector. */
    val agents: StateFlow<List<Agent>> = agentRepo.agents

    private val _activeSpaceIds = MutableStateFlow<Set<String>>(emptySet())
    val activeSpaceIds: StateFlow<Set<String>> = _activeSpaceIds.asStateFlow()

    /** ID of the currently active agent; defaults to Orbit. */
    private val _activeAgentId = MutableStateFlow(ORBIT_AGENT_ID)
    val activeAgentId: StateFlow<String> = _activeAgentId.asStateFlow()

    fun toggleSpace(id: String) {
        _activeSpaceIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun selectAgent(id: String) {
        _activeAgentId.value = id
    }

    val chats: StateFlow<List<Chat>> = chatRepo.chats

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    fun stopGeneration() {
        generationJob?.cancel()
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
        val model = AVAILABLE_MODELS.find { it.id == chat.modelId }
            ?: AVAILABLE_MODELS.first()
        val settings = settingsStore.get()

        generationJob = viewModelScope.launch(Dispatchers.IO) {
            val memoryEnabled = memoryFeatureStore.isEnabled

            // 1. Add user message
            chatRepo.addMessage(chatId, Message(role = Role.USER, content = userText))

            // 1b. Auto-detect and save memorable facts from user message
            if (memoryEnabled) {
                extractMemoryFacts(userText).forEach { fact ->
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
            val ragContext = spaceRepo.searchChunksInSpaces(
                userText,
                _activeSpaceIds.value.toList(),
                limit = 5,
            ).map { it.content }
            val memories = if (memoryEnabled) {
                memoryRepo.getAllMemories().map { it.content }
            } else {
                emptyList()
            }
            val systemPrompt = agentRepo.agents.value
                .find { it.id == _activeAgentId.value }
                ?.systemPrompt
                ?: agentRepo.agents.value.find { it.isDefault }?.systemPrompt
            val prompt = buildGemmaPrompt(history, ragContext, memories, systemPrompt)

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

    /** Format messages in Gemma instruction-tuned format, with optional system prompt, RAG context and memories. */
    private fun buildGemmaPrompt(
        messages: List<Message>,
        ragContext: List<String> = emptyList(),
        memories: List<String> = emptyList(),
        systemPrompt: String? = null,
    ): String {
        val sb = StringBuilder()

        // System prompt turn (agent persona)
        if (!systemPrompt.isNullOrBlank()) {
            sb.append("<start_of_turn>user\n")
            sb.append("System instructions: $systemPrompt\n")
            sb.append("<end_of_turn>\n")
            sb.append("<start_of_turn>model\nUnderstood.<end_of_turn>\n")
        }

        // Inject memories + RAG context as a preamble turn
        val hasContext = memories.isNotEmpty() || ragContext.isNotEmpty()
        if (hasContext) {
            sb.append("<start_of_turn>user\n")
            if (memories.isNotEmpty()) {
                sb.append("The following is personal context you know about the user. Always use this when relevant:\n\n")
                memories.forEachIndexed { i, fact ->
                    sb.append("[Memory ${i + 1}] $fact\n")
                }
                sb.append("\n")
            }
            if (ragContext.isNotEmpty()) {
                sb.append("Use the following reference documents to answer the question. ")
                sb.append("If the answer is not in the documents, say so.\n\n")
                ragContext.forEachIndexed { i, chunk ->
                    sb.append("[Document ${i + 1}]\n$chunk\n\n")
                }
            }
            sb.append("Now answer the user's questions using the above context.<end_of_turn>\n")
            sb.append("<start_of_turn>model\nUnderstood. I'll use the provided context to answer your questions.<end_of_turn>\n")
        }

        messages.forEach { msg ->
            when (msg.role) {
                Role.USER      -> sb.append("<start_of_turn>user\n${msg.content}<end_of_turn>\n")
                Role.ASSISTANT -> sb.append("<start_of_turn>model\n${msg.content}<end_of_turn>\n")
            }
        }
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        llmRepo.close()
    }
}