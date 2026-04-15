package com.example.orbitai.feature.bubble

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.model.LlmModel
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolCall
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.google.ai.edge.litertlm.tool

class OrbitBubbleLiteRtRuntime(
    private val context: Context,
) : AutoCloseable {

    data class LoadedRuntime(
        val backendLabel: String,
    )

    data class TurnResult(
        val text: String,
        val toolCalls: List<ToolCall>,
    )

    private val logTag = "OrbitLiteRtRuntime"

    private var engine: Engine? = null
    private var loadedModelId: String? = null
    private var loadedSettings: InferenceSettings? = null
    private var loadedBackendLabel: String? = null

    fun ensureLoaded(model: LlmModel, settings: InferenceSettings): LoadedRuntime {
        if (loadedModelId == model.id && loadedSettings == settings && engine != null) {
            return LoadedRuntime(backendLabel = loadedBackendLabel ?: "CPU")
        }

        close()

        val preferredBackend = Backend.GPU()
        val preferredVisionBackend = Backend.GPU()

        val loadedEngine = try {
            Log.d(logTag, "Initializing LiteRT-LM engine on GPU for model=${model.id}")
            createEngine(
                modelPath = modelPathFor(model),
                textBackend = preferredBackend,
                visionBackend = preferredVisionBackend,
            ).also {
                loadedBackendLabel = "GPU"
            }
        } catch (gpuError: Exception) {
            Log.w(logTag, "GPU engine init failed for model=${model.id}; falling back to CPU", gpuError)
            createEngine(
                modelPath = modelPathFor(model),
                textBackend = Backend.CPU(),
                visionBackend = Backend.CPU(),
            ).also {
                loadedBackendLabel = "CPU"
            }
        }

        engine = loadedEngine
        loadedModelId = model.id
        loadedSettings = settings

        return LoadedRuntime(backendLabel = loadedBackendLabel ?: "CPU")
    }

    fun createConversation(
        systemInstruction: String,
        settings: InferenceSettings,
        enableTools: Boolean,
    ): Conversation {
        val activeEngine = engine ?: throw IllegalStateException("LiteRT-LM engine is not loaded.")
        val samplerConfig = SamplerConfig(
            topK = settings.topK,
            topP = settings.topP.toDouble(),
            temperature = settings.temperature.toDouble(),
            seed = 0,
        )
        val conversationConfig = ConversationConfig(
            systemInstruction = Contents.of(systemInstruction),
            tools = if (enableTools) listOf(tool(OrbitBubbleToolSchema())) else emptyList(),
            samplerConfig = samplerConfig,
            automaticToolCalling = false,
        )
        return activeEngine.createConversation(conversationConfig)
    }

    suspend fun streamTurn(
        conversation: Conversation,
        contents: Contents,
        maxDecodedTokens: Int,
        onDelta: suspend (String) -> Unit,
    ): TurnResult {
        val resolvedMaxTokens = maxDecodedTokens.coerceAtLeast(1)
        var previousText = ""
        var chunkCount = 0
        var reachedLimit = false
        var lastMessage: Message? = null

        conversation.sendMessageAsync(contents).collect { message ->
            if (reachedLimit) {
                return@collect
            }

            lastMessage = message
            val fullText = extractText(message)
            val delta = if (fullText.startsWith(previousText)) {
                fullText.removePrefix(previousText)
            } else {
                fullText
            }
            previousText = fullText

            if (delta.isNotEmpty()) {
                onDelta(delta)
                chunkCount++
                if (chunkCount >= resolvedMaxTokens) {
                    reachedLimit = true
                    conversation.cancelProcess()
                }
            }
        }

        return TurnResult(
            text = extractText(lastMessage),
            toolCalls = lastMessage?.toolCalls.orEmpty(),
        )
    }

    fun textContents(text: String): Contents = Contents.of(text)

    fun imagePromptContents(bitmap: Bitmap, promptText: String): Contents {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return Contents.of(
            Content.ImageBytes(stream.toByteArray()),
            Content.Text(promptText),
        )
    }

    override fun close() {
        try {
            engine?.close()
        } catch (_: Exception) {
        }
        engine = null
        loadedModelId = null
        loadedSettings = null
        loadedBackendLabel = null
    }

    private fun createEngine(
        modelPath: String,
        textBackend: Backend,
        visionBackend: Backend,
    ): Engine {
        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = textBackend,
            visionBackend = visionBackend,
            cacheDir = context.cacheDir.absolutePath,
        )
        return Engine(engineConfig).also { it.initialize() }
    }

    private fun modelPathFor(model: LlmModel): String {
        return java.io.File(context.getExternalFilesDir(null), "models/${model.fileName}").absolutePath
    }

    private fun extractText(message: Message?): String {
        return message?.contents?.contents
            ?.mapNotNull { content -> (content as? Content.Text)?.text }
            ?.joinToString(separator = "")
            .orEmpty()
    }

    private class OrbitBubbleToolSchema : ToolSet {

        @Tool(
            description = "Capture the current Android screen when visual context is required to answer a request about what is visible on-screen.",
        )
        fun take_screenshot(): Map<String, String> {
            return mapOf("status" to "handled_manually")
        }

        @Tool(
            description = "Insert generated text into the user's currently focused editable text field.",
        )
        fun inject_into_textfield(
            @ToolParam(description = "The exact text to insert into the active text field.")
            text: String,
        ): Map<String, String> {
            return mapOf(
                "status" to "handled_manually",
                "text" to text,
            )
        }
    }
}
