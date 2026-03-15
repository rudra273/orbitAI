package com.example.orbitai.data

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LiteRtLmEngine(
    private val context: Context,
    private val modelPath: String,
    private val settings: InferenceSettings,
) : LlmInferenceEngine {

    private var engine: Engine? = null

    init {
        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU(),
            cacheDir = context.cacheDir.absolutePath,
        )
        engine = Engine(engineConfig).also { it.initialize() }
    }

    override fun generateResponseStream(prompt: String, maxDecodedTokens: Int): Flow<String> = flow {
        val eng = engine ?: throw IllegalStateException("No model loaded.")
        val samplerConfig = SamplerConfig(
            topK = settings.topK,
            topP = settings.topP.toDouble(),
            temperature = settings.temperature.toDouble(),
            seed = 0,
        )
        val conversationConfig = ConversationConfig(samplerConfig = samplerConfig)
        var conversation: Conversation? = null

        try {
            conversation = eng.createConversation(conversationConfig)
            var previousText = ""
            var chunkCount = 0
            var reachedLimit = false

            conversation.sendMessageAsync(prompt).collect { message ->
                if (reachedLimit) return@collect
                val fullText = extractText(message)
                val delta = if (fullText.startsWith(previousText)) {
                    fullText.removePrefix(previousText)
                } else {
                    fullText
                }
                previousText = fullText

                if (delta.isNotEmpty()) {
                    emit(delta)
                    chunkCount++
                    if (chunkCount >= maxDecodedTokens) {
                        reachedLimit = true
                        conversation.cancelProcess()
                    }
                }
            }
        } finally {
            try {
                conversation?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun extractText(message: Message): String {
        return message.contents.contents
            .mapNotNull { content -> (content as? Content.Text)?.text }
            .joinToString(separator = "")
    }

    override fun close() {
        try {
            engine?.close()
        } catch (_: Exception) {
        }
        engine = null
    }
}
