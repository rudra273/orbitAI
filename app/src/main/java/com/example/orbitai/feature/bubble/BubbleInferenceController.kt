package com.example.orbitai.feature.bubble

import com.example.orbitai.appContainer
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.example.orbitai.core.common.InferenceSettingsStore
import com.example.orbitai.core.engine.ConversationSession
import com.example.orbitai.core.engine.LlmConversationEngine
import com.example.orbitai.core.engine.LlmRepository
import com.example.orbitai.core.engine.ToolCallResult
import com.example.orbitai.core.model.AVAILABLE_MODELS
import com.example.orbitai.core.model.ModelDownloader
import com.example.orbitai.core.prompt.AiSafetyPolicy
import com.example.orbitai.core.prompt.ModelPromptBuilder
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Runs bubble requests: loads the bubble's model, streams answers, handles the bubble tools
 * (copy, user-approved screenshot) and reports progress to [Ui] on the main thread.
 */
class BubbleInferenceController(
    private val context: Context,
    private val ui: Ui,
) {

    interface Ui {
        val isResultVisible: Boolean
        val resultText: String
        fun showResultOverlay(transcript: String)
        fun ensureExpandedResultOverlay()
        fun updateResultStatus(text: String)
        fun updateResultText(text: String)
        fun appendResultText(delta: String)
        fun copyTextToClipboard(text: String)
        fun setOrbitOverlayVisibility(isVisible: Boolean)
        fun setScreenCaptureForeground(isScreenCapture: Boolean)
    }

    private val logTag = "OrbitBubble"

    private val llmRepository: LlmRepository = context.appContainer.llmRepository
    private var llmJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private enum class ScreenCaptureSource {
        MEDIA_PROJECTION,
        UNAVAILABLE,
    }

    private data class ScreenCaptureResult(
        val bitmap: Bitmap?,
        val source: ScreenCaptureSource,
    )

    /** Session of the run in progress, so a stop can halt native decoding immediately. */
    @Volatile private var activeSession: ConversationSession? = null
    private var watchdogJob: Job? = null

    fun cancel() {
        activeSession?.cancel()
        llmJob?.cancel()
        watchdogJob?.cancel()
    }

    fun destroy() {
        activeSession?.cancel()
        watchdogJob?.cancel()
        llmJob?.cancel()
        serviceScope.cancel()
        // Shared model: free it only if the app isn't using it.
        context.appContainer.appScope.launch { llmRepository.releaseIfIdle() }
    }

    fun run(initialTranscript: String) {
        Log.d(logTag, "Starting bubble inference")
        val isScreenIntent = isLikelyScreenUnderstandingRequest(initialTranscript)
        if (isScreenIntent) {
            Log.d(logTag, "Screen-understanding route selected; requesting user-approved capture when needed")
        }

        // Stop the previous run and let it fully release the engine before starting this one.
        val previousJob = llmJob
        activeSession?.cancel()
        previousJob?.cancel()
        watchdogJob?.cancel()

        val job = serviceScope.launch {
            previousJob?.join()
            val startedAt = SystemClock.elapsedRealtime()
            val downloader = context.appContainer.modelDownloader
            val selectedModelId = BubbleSettingsStore(context).bubbleModelId
            val model = AVAILABLE_MODELS.firstOrNull {
                it.isChatSelectable && it.id == selectedModelId && downloader.isDownloaded(it)
            } ?: AVAILABLE_MODELS.firstOrNull {
                it.isChatSelectable && downloader.isDownloaded(it)
            }

            if (model == null) {
                withContext(Dispatchers.Main) {
                    ui.showResultOverlay(initialTranscript)
                    ui.updateResultStatus("Model unavailable")
                    ui.updateResultText("No model downloaded.\nOpen Orbit → Settings → Model to download one.")
                }
                return@launch
            }

            val settings = InferenceSettingsStore(context).get()

            llmRepository.beginUse()
            try {
                // Ensure the shared LlmRepository has the model loaded.
                val repo = llmRepository
                if (!repo.isModelLoaded(model.id, settings)) {
                    withContext(Dispatchers.Main) {
                        ui.showResultOverlay(initialTranscript)
                        ui.updateResultStatus("Loading model...")
                    }
                    repo.loadModel(model, settings)
                }

                val convEngine = repo.conversationEngine()

                // Non-LiteRT models (Task/Gemini) don't support conversations — use simple streaming.
                if (convEngine == null) {
                    withContext(Dispatchers.Main) {
                        if (!ui.isResultVisible) {
                            ui.showResultOverlay(initialTranscript)
                        }
                    }
                    runLegacyOverlayStreaming(
                        model = model,
                        initialTranscript = initialTranscript,
                        repo = repo,
                        settings = settings,
                        startedAt = startedAt,
                    )
                    return@launch
                }

                Log.d(logTag, "Conversation engine ready for model=${model.id}")

                if (isScreenIntent) {
                    runDedicatedScreenUnderstanding(
                        initialTranscript = initialTranscript,
                        convEngine = convEngine,
                        settings = settings,
                        startedAt = startedAt,
                    )
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    ui.showResultOverlay(initialTranscript)
                    ui.updateResultStatus("Thinking...")
                    ui.updateResultText("")
                }

                val systemInstruction = """
                    You are Orbit AI running fully on-device in an Android floating bubble.
                    Answer the user directly unless a tool is truly required.
                    Use copy_to_clipboard only when the user wants you to draft, write, paste, reply, or fill text in another app.
                    Use take_screenshot only when the user is asking about what is currently visible on-screen and visual context is required. The user will approve screen capture first.
                    Never expose raw tool names or tool arguments to the user.
                    Keep answers concise by default.
                    Call at most one tool in a turn.
                """.trimIndent()

                convEngine.createConversation(
                    systemInstruction = AiSafetyPolicy.withInstructions(systemInstruction),
                    settings = settings,
                    toolSchemas = listOf(OrbitBubbleToolSchema()),
                ).use { session ->
                    activeSession = session
                    val firstTurn = session.streamTurn(
                        contents = textContents(initialTranscript),
                        maxDecodedTokens = settings.maxDecodedTokens,
                    ) { delta ->
                        withContext(Dispatchers.Main) {
                            ui.appendResultText(delta)
                        }
                    }

                    val firstToolCall = firstTurn.toolCalls.firstOrNull()
                    if (firstToolCall == null) {
                        withContext(Dispatchers.Main) {
                            ui.updateResultStatus("Done in ${formatElapsed(startedAt)}")
                            if (firstTurn.text.isBlank()) {
                                ui.updateResultText("I couldn't generate a response.")
                            }
                        }
                        return@launch
                    }

                    Log.d(logTag, "Structured tool selected: ${firstToolCall.name}")
                    withContext(Dispatchers.Main) { ui.updateResultText("") }

                    when (firstToolCall.name) {
                        "copy_to_clipboard" -> {
                            handleCopyToolCall(firstToolCall, startedAt)
                        }

                        "take_screenshot" -> {
                            withContext(Dispatchers.Main) { ui.updateResultStatus("Capturing screen...") }
                            val capture = captureBestScreenBitmap()
                            val bitmap = capture.bitmap
                            if (bitmap == null) {
                                withContext(Dispatchers.Main) {
                                    ui.updateResultStatus("Screenshot unavailable")
                                    ui.updateResultText("I couldn't capture the current screen. Approve screen capture when Android asks, then try again.")
                                }
                                return@launch
                            }

                            try {
                                withContext(Dispatchers.Main) {
                                    ui.updateResultStatus(
                                        if (capture.source == ScreenCaptureSource.MEDIA_PROJECTION) {
                                            "Analyzing screenshot..."
                                        } else {
                                            "Screenshot unavailable"
                                        }
                                    )
                                    ui.updateResultText("")
                                }
                                val followUpTurn = session.streamTurn(
                                    contents = imagePromptContents(
                                        bitmap,
                                        buildScreenUnderstandingPrompt(initialTranscript),
                                    ),
                                    maxDecodedTokens = settings.maxDecodedTokens,
                                ) { delta ->
                                    withContext(Dispatchers.Main) {
                                        ui.appendResultText(delta)
                                    }
                                }

                                val followUpToolCall = followUpTurn.toolCalls.firstOrNull()
                                if (followUpToolCall?.name == "copy_to_clipboard") {
                                    handleCopyToolCall(followUpToolCall, startedAt)
                                } else {
                                    withContext(Dispatchers.Main) {
                                        ui.updateResultStatus("Done in ${formatElapsed(startedAt)}")
                                        if (followUpTurn.text.isBlank()) {
                                            ui.updateResultText("I couldn't read a useful answer from the current screen.")
                                        }
                                    }
                                }
                            } finally {
                                if (!bitmap.isRecycled) {
                                    bitmap.recycle()
                                }
                            }
                        }

                        else -> {
                            Log.w(logTag, "Unsupported tool call returned: ${firstToolCall.name}")
                            withContext(Dispatchers.Main) {
                                ui.updateResultStatus("Unsupported tool")
                                ui.updateResultText("Orbit requested an unsupported tool: ${firstToolCall.name}")
                            }
                        }
                    }
                }

            } catch (e: CancellationException) {
                Log.d(logTag, "Agentic inference cancelled")
                throw e
            } catch (e: Exception) {
                Log.e(logTag, "Agentic inference failed", e)
                withContext(Dispatchers.Main) {
                    if (!ui.isResultVisible) {
                        ui.showResultOverlay(initialTranscript)
                    }
                    ui.updateResultStatus("Failed")
                    ui.updateResultText("Something went wrong: ${e.message}")
                }
            } finally {
                activeSession = null
                llmRepository.endUse()
                Log.d(logTag, "Bubble run finished in ${formatElapsed(startedAt)}")
            }
        }
        llmJob = job
        watchdogJob = serviceScope.launch {
            delay(MAX_RUN_MS)
            if (job.isActive) {
                Log.w(logTag, "Bubble run exceeded ${MAX_RUN_MS / 1000}s; stopping")
                activeSession?.cancel()
                job.cancel()
                withContext(Dispatchers.Main) {
                    if (!ui.isResultVisible) ui.showResultOverlay(initialTranscript)
                    ui.updateResultStatus("Stopped after ${MAX_RUN_MS / 1000}s")
                    if (ui.resultText.isBlank()) {
                        ui.updateResultText("This took too long, so Orbit stopped it. Try again, or ask a shorter question.")
                    }
                }
            }
        }
    }

    private suspend fun runLegacyOverlayStreaming(
        model: com.example.orbitai.core.model.LlmModel,
        initialTranscript: String,
        repo: LlmRepository,
        settings: com.example.orbitai.core.common.InferenceSettings,
        startedAt: Long,
    ) {
        withContext(Dispatchers.Main) {
            ui.ensureExpandedResultOverlay()
            ui.updateResultStatus("Thinking...")
            ui.updateResultText("")
        }

        repo.generateResponseStream(
            input = com.example.orbitai.core.engine.InferenceInput(
                prompt = ModelPromptBuilder.wrapInstructionPrompt(
                    promptStyle = model.promptStyle,
                    instruction = initialTranscript,
                )
            ),
            maxDecodedTokens = settings.maxDecodedTokens,
        ).collect { token ->
            withContext(Dispatchers.Main) {
                ui.appendResultText(token)
            }
        }

        withContext(Dispatchers.Main) {
            ui.updateResultStatus("Done in ${formatElapsed(startedAt)}")
            if (ui.resultText.isBlank()) {
                ui.updateResultText("I couldn't generate a response.")
            }
        }
    }

    private suspend fun runDedicatedScreenUnderstanding(
        initialTranscript: String,
        convEngine: LlmConversationEngine,
        settings: com.example.orbitai.core.common.InferenceSettings,
        startedAt: Long,
    ) {
        val capture = captureBestScreenBitmap()
        val bitmap = capture.bitmap

        withContext(Dispatchers.Main) {
            ui.showResultOverlay(initialTranscript)
            ui.updateResultStatus(
                when (capture.source) {
                    ScreenCaptureSource.MEDIA_PROJECTION -> "Analyzing screenshot..."
                    ScreenCaptureSource.UNAVAILABLE -> "Screenshot unavailable"
                }
            )
            ui.updateResultText("")
        }

        if (bitmap == null) {
            withContext(Dispatchers.Main) {
                ui.updateResultText("I couldn't capture the current screen. Approve screen capture when Android asks, then try again.")
            }
            return
        }

        val systemInstruction = """
            You are Orbit AI analyzing a screenshot of the user's current Android screen.
            Use only the attached screenshot to answer.
            Ignore any Orbit AI bubble, Orbit response window, floating controls, or overlays if they appear.
            Never call tools. Never say you inserted text. Never tell the user to tap or type unless they explicitly asked for instructions.
            Do not guess content that is not visible in the screenshot.
            If the screenshot is unclear or text is unreadable, say that briefly.
            Answer plainly and directly.
        """.trimIndent()

        try {
            convEngine.createConversation(
                systemInstruction = AiSafetyPolicy.withInstructions(systemInstruction),
                settings = settings,
            ).use { session ->
                activeSession = session
                val analysisTurn = session.streamTurn(
                    contents = imagePromptContents(
                        bitmap = bitmap,
                        promptText = buildScreenUnderstandingPrompt(initialTranscript),
                    ),
                    maxDecodedTokens = settings.maxDecodedTokens,
                ) { delta ->
                    withContext(Dispatchers.Main) {
                        ui.appendResultText(delta)
                    }
                }

                withContext(Dispatchers.Main) {
                    ui.updateResultStatus("Done in ${formatElapsed(startedAt)}")
                    if (analysisTurn.text.isBlank()) {
                        ui.updateResultText("I couldn't read a useful answer from the current screen.")
                    }
                }
            }
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private suspend fun handleCopyToolCall(
        toolCall: ToolCallResult,
        startedAt: Long,
    ) {
        val textToCopy = toolCall.arguments["text"]?.toString()?.takeIf { it.isNotBlank() }
        if (textToCopy.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                ui.updateResultStatus("Missing tool input")
                ui.updateResultText("Orbit tried to prepare text, but did not return anything to copy.")
            }
            return
        }

        withContext(Dispatchers.Main) {
            ui.copyTextToClipboard(textToCopy)
            ui.updateResultStatus("Copied in ${formatElapsed(startedAt)}")
            ui.updateResultText(textToCopy)
            Toast.makeText(context, "Copied. Paste it where you want.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Content helpers (moved from OrbitBubbleLiteRtRuntime) ─────────────────

    private fun textContents(text: String): Contents = Contents.of(text)

    private fun imagePromptContents(bitmap: Bitmap, promptText: String): Contents {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return Contents.of(
            Content.ImageBytes(stream.toByteArray()),
            Content.Text(promptText),
        )
    }

    private suspend fun captureBestScreenBitmap(): ScreenCaptureResult {
        val displayBitmap = captureDisplayBitmapWithOrbitUiHidden()
        return ScreenCaptureResult(
            bitmap = displayBitmap,
            source = if (displayBitmap != null) {
                ScreenCaptureSource.MEDIA_PROJECTION
            } else {
                ScreenCaptureSource.UNAVAILABLE
            },
        )
    }

    private suspend fun captureDisplayBitmapWithOrbitUiHidden(): Bitmap? {
        withContext(Dispatchers.Main) {
            ui.setOrbitOverlayVisibility(isVisible = false)
        }
        delay(90)
        return try {
            captureScreenWithUserConsent()
        } finally {
            withContext(Dispatchers.Main) {
                ui.setOrbitOverlayVisibility(isVisible = true)
            }
        }
    }

    private suspend fun captureScreenWithUserConsent(): Bitmap? {
        val permission = requestMediaProjectionPermission() ?: return null
        return try {
            ui.setScreenCaptureForeground(isScreenCapture = true)
            captureBitmapWithMediaProjection(permission.first, permission.second)
        } finally {
            ui.setScreenCaptureForeground(isScreenCapture = false)
        }
    }

    private suspend fun requestMediaProjectionPermission(): Pair<Int, Intent>? {
        return suspendCancellableCoroutine { continuation ->
            val registered = registerMediaProjectionContinuation(continuation)
            if (!registered) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            val intent = Intent(context, MediaProjectionPermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                clearMediaProjectionContinuation(continuation)
                if (continuation.isActive) continuation.resume(null)
                Log.e(logTag, "Failed to request screen capture permission", e)
            }
        }
    }

    private suspend fun captureBitmapWithMediaProjection(resultCode: Int, resultData: Intent): Bitmap? {
        return withContext(Dispatchers.IO) {
            val manager = context.getSystemService(MediaProjectionManager::class.java)
            val projection = manager.getMediaProjection(resultCode, resultData) ?: return@withContext null
            val handlerThread = HandlerThread("OrbitScreenCapture").apply { start() }
            val handler = Handler(handlerThread.looper)
            var virtualDisplay: VirtualDisplay? = null
            var imageReader: ImageReader? = null
            val callback = object : MediaProjection.Callback() {
                override fun onStop() = Unit
            }

            try {
                projection.registerCallback(callback, handler)
                val (screenW, screenH) = context.getSystemService(WindowManager::class.java).screenSize()
                val densityDpi = context.resources.configuration.densityDpi
                imageReader = ImageReader.newInstance(screenW, screenH, PixelFormat.RGBA_8888, 2)
                virtualDisplay = projection.createVirtualDisplay(
                    "OrbitScreenCapture",
                    screenW,
                    screenH,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    null,
                    handler,
                )
                delay(420)

                var image: Image? = null
                repeat(6) {
                    if (image == null) {
                        image = imageReader.acquireLatestImage()
                        if (image == null) delay(120)
                    }
                }
                image?.use { convertImageToBitmap(it, screenW, screenH) }
            } catch (e: Exception) {
                Log.e(logTag, "MediaProjection screenshot failed", e)
                null
            } finally {
                virtualDisplay?.release()
                imageReader?.close()
                projection.unregisterCallback(callback)
                projection.stop()
                handlerThread.quitSafely()
            }
        }
    }

    private fun convertImageToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        return try {
            val plane = image.planes.firstOrNull() ?: return null
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width
            val paddedWidth = width + rowPadding / pixelStride
            val paddedBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
            paddedBitmap.copyPixelsFromBuffer(buffer)
            val cropped = Bitmap.createBitmap(paddedBitmap, 0, 0, width, height)
            if (paddedBitmap != cropped) paddedBitmap.recycle()

            val maxWidth = 1024
            if (cropped.width <= maxWidth) {
                cropped
            } else {
                val ratio = cropped.width.toFloat() / cropped.height.toFloat()
                val resized = Bitmap.createScaledBitmap(cropped, maxWidth, (maxWidth / ratio).toInt(), true)
                cropped.recycle()
                resized.copy(Bitmap.Config.ARGB_8888, false).also { resized.recycle() }
            }
        } catch (e: Exception) {
            Log.e(logTag, "Failed to convert MediaProjection image", e)
            null
        }
    }

    private fun isLikelyScreenUnderstandingRequest(transcript: String): Boolean {
        val normalized = transcript.lowercase()
        val screenHints = listOf(
            "screen",
            "screenshot",
            "what is on my screen",
            "what's on my screen",
            "current screen",
            "this screen",
            "read my screen",
            "read this screen",
            "translate this screen",
            "what am i looking at",
            "what is here",
        )
        return screenHints.any { hint -> normalized.contains(hint) }
    }

    private fun buildScreenUnderstandingPrompt(transcript: String): String {
        val normalized = transcript.lowercase()
        val taskInstruction = when {
            normalized.contains("translate") -> {
                "Translate all clearly visible text on the screen into the user's language. Preserve important structure when possible."
            }
            normalized.contains("explain") -> {
                "Explain what is visible on the screen, what app or UI it appears to be, and what the important elements mean."
            }
            normalized.contains("summarize") -> {
                "Summarize the important visible content on the screen."
            }
            else -> {
                "Describe what is visible on the screen and answer the user's request."
            }
        }

        return """
            Attached is a screenshot of the user's current screen.
            User request: $transcript

            Rules:
            - Use only the screenshot.
            - Ignore any Orbit AI bubble, Orbit AI response card, or floating overlay if visible.
            - Do not mention tools or hidden context.
            - Do not say you inserted text.
            - If something is not readable, say so briefly.

            Task:
            $taskInstruction
        """.trimIndent()
    }

    private fun formatElapsed(startedAt: Long): String {
        val elapsedMs = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
        return if (elapsedMs >= 10_000L) {
            "${elapsedMs / 1000}s"
        } else {
            String.format(java.util.Locale.US, "%.1fs", elapsedMs / 1000f)
        }
    }

    companion object {
        private const val MAX_RUN_MS = 120_000L
        private var mediaProjectionContinuation: CancellableContinuation<Pair<Int, Intent>?>? = null

        fun deliverMediaProjectionResult(resultCode: Int, data: Intent?) {
            val continuation = mediaProjectionContinuation ?: return
            mediaProjectionContinuation = null
            val result = if (resultCode == Activity.RESULT_OK && data != null) {
                resultCode to data
            } else {
                null
            }
            continuation.resume(result)
        }

        private fun registerMediaProjectionContinuation(
            continuation: CancellableContinuation<Pair<Int, Intent>?>,
        ): Boolean {
            if (mediaProjectionContinuation != null) return false
            mediaProjectionContinuation = continuation
            continuation.invokeOnCancellation {
                clearMediaProjectionContinuation(continuation)
            }
            return true
        }

        private fun clearMediaProjectionContinuation(
            continuation: CancellableContinuation<Pair<Int, Intent>?>,
        ) {
            if (mediaProjectionContinuation == continuation) {
                mediaProjectionContinuation = null
            }
        }
    }
}
