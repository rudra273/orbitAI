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
import com.example.orbitai.data.Message
import com.example.orbitai.data.Role
import com.example.orbitai.data.rag.RagRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val isModelLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val loadError: String? = null,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val chatRepo = ChatRepository(application)
    private val llmRepo = LlmRepository(application)
    private val settingsStore = InferenceSettingsStore(application)
    private val ragRepo = RagRepository(application)

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
            // 1. Add user message
            chatRepo.addMessage(chatId, Message(role = Role.USER, content = userText))

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

            // 3. Build prompt from history + RAG context
            val history = chatRepo.getChat(chatId)?.messages ?: emptyList()
            val ragContext = ragRepo.searchChunks(userText, limit = 5)
                .map { it.content }
            val prompt = buildGemmaPrompt(history, ragContext)

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

    /** Format messages in Gemma instruction-tuned format, with optional RAG context. */
    private fun buildGemmaPrompt(
        messages: List<Message>,
        ragContext: List<String> = emptyList(),
    ): String {
        val sb = StringBuilder()

        // Inject RAG context as a system preamble in the first user turn
        if (ragContext.isNotEmpty()) {
            sb.append("<start_of_turn>user\n")
            sb.append("Use the following reference documents to answer the question. ")
            sb.append("If the answer is not in the documents, say so.\n\n")
            ragContext.forEachIndexed { i, chunk ->
                sb.append("[Document ${i + 1}]\n$chunk\n\n")
            }
            sb.append("Now answer the user's questions using the above context.<end_of_turn>\n")
            sb.append("<start_of_turn>model\nUnderstood. I'll use the provided documents to answer your questions.<end_of_turn>\n")
        }

        messages.forEach { msg ->
            when (msg.role) {
                Role.USER -> sb.append("<start_of_turn>user\n${msg.content}<end_of_turn>\n")
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