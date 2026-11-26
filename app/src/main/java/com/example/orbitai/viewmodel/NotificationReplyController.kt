package com.example.orbitai.viewmodel

import com.example.orbitai.core.common.TokenStore
import com.example.orbitai.core.engine.InferenceInput
import com.example.orbitai.core.model.availableChatModels
import com.example.orbitai.core.prompt.ModelPromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local-model drafts for the Message replies screen. Shares the chat's generation slot. */
internal class NotificationReplyController(
    private val ctx: ChatControllerContext,
    private val generation: ChatGenerationController,
) {

    private val llmRepo = ctx.container.llmRepository
    private val tokenStore = TokenStore(ctx.app)

    private val _notificationReplyDraft = MutableStateFlow(NotificationReplyDraftState())
    val notificationReplyDraft = _notificationReplyDraft.asStateFlow()
    private var notificationGenerationToken: Long? = null

    fun clearNotificationReplyDraft() {
        if (notificationGenerationToken?.let(generation::isGenerationTokenActive) == true) generation.stopGeneration()
        notificationGenerationToken = null
        _notificationReplyDraft.value = NotificationReplyDraftState()
    }

    fun draftNotificationReply(preview: com.example.orbitai.feature.automation.replies.ReplyNotification, instruction: String) {
        if (instruction.isBlank()) return
        val model = com.example.orbitai.feature.automation.replies.selectLocalReplyModel(
            availableChatModels(ctx.container.modelDownloader, tokenStore), tokenStore.lastSelectedModelId,
        )
        if (model == null) {
            _notificationReplyDraft.value = NotificationReplyDraftState(preview.key, preview.revision,
                error = "Download a local model in Settings > Model, or write your reply below.")
            return
        }
        _notificationReplyDraft.value = NotificationReplyDraftState(preview.key, preview.revision, loading = true)
        notificationGenerationToken = generation.launchExclusive { token ->
            try {
                val settings = com.example.orbitai.core.common.InferenceSettings(maxDecodedTokens = 256)
                llmRepo.loadModel(model, settings)
                val prompt = ModelPromptBuilder.wrapInstructionPrompt(model.promptStyle,
                    com.example.orbitai.feature.automation.replies.ReplyDraftPrompt.build(preview.message, instruction))
                var text = ""
                llmRepo.generateResponseStream(InferenceInput(prompt), settings.maxDecodedTokens).collect { part ->
                    if (!generation.isGenerationTokenActive(token)) throw CancellationException("Stale reply draft")
                    text += part
                }
                if (generation.isGenerationTokenActive(token)) {
                    _notificationReplyDraft.value = NotificationReplyDraftState(preview.key, preview.revision,
                        text = text.trim().take(4000), error = if (text.isBlank()) "No draft was generated. Try again or write a reply." else null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (generation.isGenerationTokenActive(token)) {
                    _notificationReplyDraft.value = NotificationReplyDraftState(preview.key, preview.revision,
                        error = "Couldn't generate a local draft. Try again or write your reply.")
                }
            }
        }
    }
}
