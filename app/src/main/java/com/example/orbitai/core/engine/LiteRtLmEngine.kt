package com.example.orbitai.core.engine

import android.content.Context
import android.util.Log
import com.example.orbitai.core.common.InferenceSettings
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Unified LiteRT-LM engine that implements both [LlmInferenceEngine] (simple single-turn streaming)
 * and [LlmConversationEngine] (multi-turn conversations with tool calling).
 *
 * Used by both the main app and the floating bubble service.
 *
 * Features merged from OrbitBubbleLiteRtRuntime:
 * - GPU → CPU automatic fallback
 * - Multi-turn conversation support
 * - Structured tool calling
 */
class LiteRtLmEngine(
    private val context: Context,
    private val modelPath: String,
    private val settings: InferenceSettings,
) : LlmInferenceEngine, LlmConversationEngine {

    companion object {
        private const val TAG = "LiteRtLmEngine"
    }

    private var engine: Engine? = null
    private var backendLabel: String = "CPU"

    /** The backend this engine is running on after initialization (e.g. "GPU" or "CPU"). */
    val activeBackend: String get() = backendLabel

    init {
        engine = try {
            Log.d(TAG, "Initializing LiteRT-LM on GPU for $modelPath")
            createEngine(Backend.GPU(), Backend.GPU()).also {
                backendLabel = "GPU"
            }
        } catch (gpuError: Exception) {
            Log.w(TAG, "GPU init failed, falling back to CPU", gpuError)
            createEngine(Backend.CPU(), Backend.CPU()).also {
                backendLabel = "CPU"
            }
        }
    }

    // ── LlmInferenceEngine (simple single-turn streaming) ─────────────────────

    override fun generateResponseStream(input: InferenceInput, maxDecodedTokens: Int): Flow<String> = flow {
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

            val contentList = mutableListOf<Content>()

            input.images.forEach { bitmap ->
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                contentList.add(Content.ImageBytes(stream.toByteArray()))
            }

            contentList.add(Content.Text(input.prompt))
            val contentsPayload = Contents.of(contentList)

            conversation.sendMessageAsync(contentsPayload).collect { message ->
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

    // ── LlmConversationEngine (multi-turn with tool calling) ──────────────────

    override fun createConversation(
        systemInstruction: String,
        settings: InferenceSettings,
        toolSchemas: List<Any>,
    ): ConversationSession {
        val eng = engine ?: throw IllegalStateException("LiteRT-LM engine is not loaded.")
        val samplerConfig = SamplerConfig(
            topK = settings.topK,
            topP = settings.topP.toDouble(),
            temperature = settings.temperature.toDouble(),
            seed = 0,
        )

        val tools = toolSchemas.map { schema -> tool(schema as com.google.ai.edge.litertlm.ToolSet) }

        val conversationConfig = ConversationConfig(
            systemInstruction = Contents.of(systemInstruction),
            tools = tools,
            samplerConfig = samplerConfig,
            automaticToolCalling = false,
        )
        val conversation = eng.createConversation(conversationConfig)
        return LiteRtConversationSession(conversation)
    }

    override fun close() {
        try {
            engine?.close()
        } catch (_: Exception) {
        }
        engine = null
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun createEngine(textBackend: Backend, visionBackend: Backend): Engine {
        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = textBackend,
            visionBackend = visionBackend,
            cacheDir = context.cacheDir.absolutePath,
        )
        return Engine(engineConfig).also { it.initialize() }
    }

    private fun extractText(message: Message): String {
        return message.contents.contents
            .mapNotNull { content -> (content as? Content.Text)?.text }
            .joinToString(separator = "")
    }
}
