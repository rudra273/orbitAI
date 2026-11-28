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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Unified LiteRT-LM engine that implements both [LlmInferenceEngine] (simple single-turn streaming)
 * and [LlmConversationEngine] (multi-turn conversations with tool calling).
 *
 * Used by both the main app and the floating bubble service.
 *
 * Features merged from OrbitBubbleLiteRtRuntime:
 * - Backend choice from [BackendPlan] (Auto falls back, a picked backend does not)
 * - Multi-turn conversation support
 * - Structured tool calling
 */
class LiteRtLmEngine(
    private val context: Context,
    private val modelPath: String,
    private val settings: InferenceSettings,
    private val plan: BackendPlan,
    private val supportsVision: Boolean,
) : LlmInferenceEngine, LlmConversationEngine {

    companion object {
        private const val TAG = "LiteRtLmEngine"

        /** Text-only files have no vision encoder, and LiteRT-LM refuses to load them with a vision backend set. */
        internal fun visionBackendFor(supportsVision: Boolean, backend: HardwareBackend): Backend? = when {
            !supportsVision -> null
            backend == HardwareBackend.CPU -> Backend.CPU()
            else -> Backend.GPU()
        }
    }

    private var engine: Engine? = null
    private var backendLabel: String = "CPU"

    /** The backend this engine is running on after initialization (e.g. "GPU" or "CPU"). */
    val activeBackend: String get() = backendLabel

    init {
        // NPU (only models built for this chip) → GPU → CPU in Auto; a picked backend never falls back (#40).
        val loaded = BackendCascade(context, java.io.File(modelPath).name).load(plan) { backend ->
            Log.d(TAG, "Initializing LiteRT-LM on ${backend.label} for $modelPath")
            val vision = visionBackendFor(supportsVision, backend)
            when (backend) {
                HardwareBackend.NPU -> createEngine(Backend.NPU(context.applicationInfo.nativeLibraryDir), vision)
                HardwareBackend.GPU -> createEngine(Backend.GPU(), vision)
                HardwareBackend.CPU -> createEngine(Backend.CPU(), vision)
            }
        }
        engine = loaded.value
        backendLabel = loaded.label
    }

    // ── LlmInferenceEngine (simple single-turn streaming) ─────────────────────

    override fun generateResponseStream(input: InferenceInput, maxDecodedTokens: Int): Flow<String> = flow {
        val eng = engine ?: throw IllegalStateException("No model loaded.")
        val samplerConfig = SamplerConfig(
            topK = settings.topK,
            topP = settings.topP.toDouble(),
            temperature = settings.temperature.toDouble(),
            seed = kotlin.random.Random.nextInt(),
        )
        val conversationConfig = ConversationConfig(
            samplerConfig = samplerConfig,
            maxOutputToken = maxDecodedTokens,
        )
        var conversation: Conversation? = null

        try {
            conversation = eng.createConversation(conversationConfig)
            var previousText = ""

            val contentList = mutableListOf<Content>()

            input.images.forEach { bitmap ->
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                contentList.add(Content.ImageBytes(stream.toByteArray()))
            }

            contentList.add(Content.Text(input.prompt))
            val contentsPayload = Contents.of(contentList)

            conversation.sendMessageAsync(contentsPayload).collect { message ->
                val fullText = extractText(message)
                val delta = if (fullText.startsWith(previousText)) {
                    fullText.removePrefix(previousText)
                } else {
                    fullText
                }
                previousText = fullText

                if (delta.isNotEmpty()) {
                    emit(delta)
                }
            }
        } catch (e: CancellationException) {
            // Coroutine cancellation alone doesn't stop native decoding (or prefill).
            runCatching { conversation?.cancelProcess() }
            throw e
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
        history: List<PromptTurn>,
    ): ConversationSession {
        val eng = engine ?: throw IllegalStateException("LiteRT-LM engine is not loaded.")
        val samplerConfig = SamplerConfig(
            topK = settings.topK,
            topP = settings.topP.toDouble(),
            temperature = settings.temperature.toDouble(),
            seed = kotlin.random.Random.nextInt(),
        )

        val tools = toolSchemas.map { schema ->
            when (schema) {
                is com.google.ai.edge.litertlm.ToolSet -> tool(schema)
                is com.google.ai.edge.litertlm.OpenApiTool -> tool(schema)
                else -> throw IllegalArgumentException("Unsupported tool definition: ${schema::class.java.name}")
            }
        }

        val conversationConfig = ConversationConfig(
            systemInstruction = Contents.of(systemInstruction),
            initialMessages = history.map { if (it.isUser) Message.user(it.text) else Message.model(it.text) },
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

    private fun createEngine(textBackend: Backend, visionBackend: Backend?): Engine {
        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = textBackend,
            visionBackend = visionBackend,
            cacheDir = context.cacheDir.absolutePath,
        )
        val candidate = Engine(engineConfig)
        try {
            candidate.initialize()
            return candidate
        } catch (error: Exception) {
            runCatching { candidate.close() }
            throw error
        }
    }

    private fun extractText(message: Message): String {
        return message.contents.contents
            .mapNotNull { content -> (content as? Content.Text)?.text }
            .joinToString(separator = "")
    }
}
