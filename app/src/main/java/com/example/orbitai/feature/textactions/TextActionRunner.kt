package com.example.orbitai.feature.textactions

import android.content.Context
import com.example.orbitai.appContainer
import com.example.orbitai.core.common.ModeInferenceSettingsStore
import com.example.orbitai.core.common.TokenStore
import com.example.orbitai.core.database.ORBIT_MODE_ID
import com.example.orbitai.core.engine.InferenceInput
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.ModelProvider
import com.example.orbitai.core.model.availableChatModels
import com.example.orbitai.core.prompt.ModelPromptBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Runs a [TextAction] on the on-device model shared with chat and the bubble. */
class TextActionRunner(private val context: Context) {

    private val container = context.appContainer
    private val tokenStore = TokenStore(context)

    /** The model the user last chose if it's local, else the first downloaded local model. Never cloud. */
    fun localModel(): LlmModel? {
        val local = availableChatModels(container.modelDownloader, tokenStore).filter { it.provider != ModelProvider.GEMINI }
        return local.firstOrNull { it.id == tokenStore.lastSelectedModelId } ?: local.firstOrNull()
    }

    fun run(model: LlmModel, action: TextAction, text: String, language: String): Flow<String> = flow {
        val repo = container.llmRepository
        val settings = ModeInferenceSettingsStore(context).get(ORBIT_MODE_ID)
        repo.beginUse()
        try {
            if (!repo.isModelLoaded(model.id, settings)) repo.loadModel(model, settings)
            val instruction = TextActionPrompts.instruction(action, text, language)
            val input = InferenceInput(
                prompt = ModelPromptBuilder.wrapInstructionPrompt(model.promptStyle, instruction),
                structured = ModelPromptBuilder.structuredInstruction(instruction),
            )
            repo.generateResponseStream(input, settings.maxDecodedTokens.coerceAtLeast(1024)).collect { emit(it) }
        } finally {
            repo.endUse()
        }
    }.flowOn(Dispatchers.IO)
}
