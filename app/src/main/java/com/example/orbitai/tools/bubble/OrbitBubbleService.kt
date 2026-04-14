package com.example.orbitai.tools.bubble

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.orbitai.MainActivity
import com.example.orbitai.R
import com.example.orbitai.data.AVAILABLE_MODELS
import com.example.orbitai.data.InferenceSettingsStore
import com.example.orbitai.data.LlmRepository
import com.example.orbitai.data.ModelFormat
import com.example.orbitai.data.ModelDownloader
import com.example.orbitai.data.ModelProvider
import com.example.orbitai.data.ToolSettingsStore
import com.example.orbitai.tools.router.ToolRoute
import com.example.orbitai.tools.router.ToolRouter
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

class OrbitBubbleService : Service() {

    private val logTag = "OrbitBubble"

    private var slideTabWidthDp = 24f
    private var slideTabHeightDp = 104f

    // ── Window / bubble ───────────────────────────────────────────────────────
    private var windowManager: WindowManager? = null
    private var bubbleView: FrameLayout? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var isBubbleAttached = false
    private var isAppForeground = false

    // ── Result overlay ────────────────────────────────────────────────────────
    private var resultCardView: View? = null
    private var resultCardParams: WindowManager.LayoutParams? = null
    private var resultStatusView: TextView? = null
    private var resultTextView: TextView? = null
    private var resultScrollView: ScrollView? = null
    private var isResultVisible = false
    private var isResultCompactMode = false
    private var lastTranscript: String = ""

    // ── Radial Menu ──────────────────────────────────────────────────────────
    private var radialMenuView: View? = null
    private var isRadialMenuVisible = false

    // ── Speech ────────────────────────────────────────────────────────────────
    private var speechRecognizer: SpeechRecognizer? = null
    private var pulseAnimator: android.animation.ValueAnimator? = null
    private var partialTranscript: String = ""
    private var isListening = false
    private var pendingSelectedText: String? = null

    // ── LLM (overlay mode) ────────────────────────────────────────────────────
    private var bubbleLlmRepo: LlmRepository? = null
    private var bubbleLiteRtRuntime: OrbitBubbleLiteRtRuntime? = null
    private var llmJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Config (read from settings on each onStartCommand) ───────────────────
    private var bubbleSizePx = 0
    private var edgeDockMarginPx = 0
    private var bubbleIdleAlpha = 0.42f
    private var bubbleStyle = "round"

    private enum class ScreenCaptureSource {
        WINDOW,
        DISPLAY_FALLBACK,
        UNAVAILABLE,
    }

    private data class ScreenCaptureResult(
        val bitmap: Bitmap?,
        val source: ScreenCaptureSource,
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { dismissBubble(); return START_NOT_STICKY }
            ACTION_APP_FOREGROUND -> {
                isAppForeground = true
                hideResultOverlay()
                removeBubble()
                return START_STICKY
            }
            ACTION_APP_BACKGROUND -> {
                isAppForeground = false
            }
            ACTION_START, ACTION_TRIGGER, ACTION_INJECT_TEXT, null -> Unit
            else -> return START_NOT_STICKY
        }

        if (!canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }

        val newSizePx = dpToPx(ToolSettingsStore(this).bubbleSizeDp.toFloat())
        val settings = ToolSettingsStore(this)
        edgeDockMarginPx = dpToPx(6f)
        bubbleStyle = settings.bubbleStyle
        bubbleIdleAlpha = settings.bubbleIdleAlphaPercent / 100f
        if (isBubbleAttached && newSizePx != bubbleSizePx) {
            removeBubble()  // will re-attach below with new size
        }
        bubbleSizePx = newSizePx
        
        // Calculate slide tab dimensions based on bubble size (proportional scaling)
        val bubbleSizeDp = settings.bubbleSizeDp.toFloat()
        slideTabWidthDp = (bubbleSizeDp * 0.375f).coerceIn(16f, 32f)
        slideTabHeightDp = (bubbleSizeDp * 1.625f).coerceIn(76f, 130f)

        startBubbleForeground(isListening = false)

        if (!isAppForeground && !isBubbleAttached) {
            attachBubble()
        } else if (!isListening && !isAppForeground) {
            // Apply transparency slider changes immediately while bubble is idle.
            enterIdleVisualMode()
        }

        if (intent?.action == ACTION_TRIGGER) {
            if (isAppForeground) {
                startListening()
            } else {
                bubbleView?.post { toggleListening() }
            }
        } else if (intent?.action == ACTION_INJECT_TEXT) {
            val text = intent.getStringExtra(EXTRA_INJECTED_TEXT)
            if (!text.isNullOrBlank()) {
                pendingSelectedText = text
                showResultOverlay("Selected: $text")
                updateResultText("Speak your instruction...")
                bubbleView?.post {
                    if (!isListening) startListening()
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopPulseAnimation()
        stopListening(submitTranscript = false)
        llmJob?.cancel()
        serviceScope.cancel()
        bubbleLlmRepo?.close()
        bubbleLlmRepo = null
        bubbleLiteRtRuntime?.close()
        bubbleLiteRtRuntime = null
        speechRecognizer?.destroy()
        speechRecognizer = null
        hideResultOverlay()
        removeBubble()
        super.onDestroy()
    }

    private fun attachBubble() {
        if (isBubbleAttached) return

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false
        var longPressTriggered = false

        val bubble = FrameLayout(this).apply {
            val width = if (bubbleStyle == "slide") dpToPx(slideTabWidthDp) else bubbleSizePx
            val height = if (bubbleStyle == "slide") dpToPx(slideTabHeightDp) else bubbleSizePx
            layoutParams = FrameLayout.LayoutParams(width, height)
            background = bubbleBackground(isActive = false)
            elevation = 32f
            if (bubbleStyle == "slide") {
                addView(View(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        dpToPx(3f),
                        dpToPx(44f),
                        Gravity.CENTER,
                    )
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dpToPx(2f).toFloat()
                        setColor(Color.argb(170, 255, 255, 255))
                    }
                })
            } else {
                val iconPad = dpToPx(12f)
                addView(ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER,
                    )
                    setPadding(iconPad, iconPad, iconPad, iconPad)
                    setImageResource(R.drawable.vector_logo)
                    imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                })
            }
        }

        val longPressRunnable = Runnable {
            longPressTriggered = true
            if (OrbitAccessibilityService.instance != null) {
                showRadialMenu()
            } else {
                dismissBubble()
                android.widget.Toast.makeText(this@OrbitBubbleService, "Enable Orbit Accessibility in Settings for quick actions", android.widget.Toast.LENGTH_LONG).show()
            }
        }

        bubble.setOnTouchListener { _, event ->
            val params = bubbleParams ?: return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    enterActiveVisualMode()
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    longPressTriggered = false
                    bubble.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (!moved && (kotlin.math.abs(deltaX) > touchSlop || kotlin.math.abs(deltaY) > touchSlop)) {
                        moved = true
                        bubble.removeCallbacks(longPressRunnable)
                    }
                    if (moved) {
                        val (screenW, screenH) = getScreenSize()
                        val bubbleW = bubble.width.takeIf { it > 0 } ?: params.width
                        val bubbleH = bubble.height.takeIf { it > 0 } ?: params.height
                        if (bubbleStyle == "slide") {
                            // Keep slide style docked with half of the widget outside screen.
                            params.x = (screenW - (bubbleW / 2)).coerceAtLeast(edgeDockMarginPx)
                            params.y = (initialY + deltaY).coerceIn(edgeDockMarginPx, (screenH - bubbleH - edgeDockMarginPx).coerceAtLeast(edgeDockMarginPx))
                        } else {
                            params.x = (initialX + deltaX).coerceIn(edgeDockMarginPx, (screenW - bubbleW - edgeDockMarginPx).coerceAtLeast(edgeDockMarginPx))
                            params.y = (initialY + deltaY).coerceIn(edgeDockMarginPx, (screenH - bubbleH - edgeDockMarginPx).coerceAtLeast(edgeDockMarginPx))
                        }
                        windowManager?.updateViewLayout(bubble, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    bubble.removeCallbacks(longPressRunnable)
                    if (!moved && !longPressTriggered) {
                        toggleListening()
                    } else {
                        snapToNearestEdge(animated = true)
                        if (!isListening) enterIdleVisualMode()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    bubble.removeCallbacks(longPressRunnable)
                    if (!isListening) {
                        snapToNearestEdge(animated = true)
                        enterIdleVisualMode()
                    }
                    true
                }
                else -> false
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            if (bubbleStyle == "slide") dpToPx(slideTabWidthDp) else bubbleSizePx,
            if (bubbleStyle == "slide") dpToPx(slideTabHeightDp) else bubbleSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (bubbleStyle == "slide") getScreenSize().first - (width / 2) else dpToPx(16f)
            y = if (bubbleStyle == "slide") dpToPx(90f) else dpToPx(200f)
        }

        bubbleView = bubble
        bubbleParams = layoutParams
        windowManager?.addView(bubble, layoutParams)
        isBubbleAttached = true
        bubble.alpha = 0f
        bubble.scaleX = 0.82f
        bubble.scaleY = 0.82f
        bubble.animate()
            .alpha(bubbleIdleAlpha)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(220L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        snapToNearestEdge(animated = false)
    }

    private fun removeBubble() {
        val bubble = bubbleView ?: return
        if (isBubbleAttached) {
            windowManager?.removeView(bubble)
        }
        bubbleView = null
        bubbleParams = null
        isBubbleAttached = false
    }

    private fun toggleListening() {
        if (isListening) {
            stopListening(submitTranscript = true)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is unavailable on this device.", Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Enable microphone permission in Orbit settings first.", Toast.LENGTH_SHORT).show()
            launchApp()
            return
        }

        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(this).also { created ->
            created.setRecognitionListener(BubbleRecognitionListener())
            speechRecognizer = created
        }

        partialTranscript = ""
        isListening = true
        enterActiveVisualMode()
        updateBubbleUi(isActive = true)
        startBubbleForeground(isListening = true)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }

        recognizer.startListening(intent)
    }

    private fun stopListening(submitTranscript: Boolean) {
        if (!isListening && partialTranscript.isBlank()) return

        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()

        val transcript = partialTranscript.trim()
        partialTranscript = ""
        isListening = false
        updateBubbleUi(isActive = false)
        snapToNearestEdge(animated = true)
        enterIdleVisualMode()
        startBubbleForeground(isListening = false)

        val hasPendingText = !pendingSelectedText.isNullOrBlank()

        if (submitTranscript && transcript.isNotBlank()) {
            handleTranscript(transcript)
        } else if (hasPendingText) {
            pendingSelectedText = null
            hideResultOverlay()
        }
    }

    // ── Transcript routing ────────────────────────────────────────────────────

    private fun handleTranscript(transcript: String) {
        var finalTranscript = transcript
        var selectedText = pendingSelectedText

        if (selectedText.isNullOrBlank()) {
            selectedText = OrbitAccessibilityService.instance?.consumeInterceptedText()
        }

        if (!selectedText.isNullOrBlank()) {
            finalTranscript = "Instruction: $transcript\n\nSelected text: ${selectedText}"
            pendingSelectedText = null
        }

        lastTranscript = finalTranscript
        val route = ToolRouter.route(finalTranscript)

        // Tool requests need app chat flow where execution and permissions are handled.
        if (route is ToolRoute.ToolOnly) {
            launchApp(finalTranscript)
            return
        }

        if (ToolSettingsStore(this).bubbleResultsInOverlay) {
            runAgenticInference(finalTranscript)
        } else {
            launchApp(finalTranscript)
        }
    }

    // ── Agentic Inference Loop ────────────────────────────────────────────────
    
    private fun runAgenticInference(initialTranscript: String) {
        hideResultOverlay()
        Log.d(logTag, "runAgenticInference transcript=${initialTranscript.take(120)}")
        val isScreenIntent = isLikelyScreenUnderstandingRequest(initialTranscript)
        val targetWindowId = if (isScreenIntent) {
            OrbitAccessibilityService.instance?.resolvePreferredCaptureWindowId()
        } else {
            null
        }
        if (isScreenIntent) {
            Log.d(logTag, "Screen-understanding route selected targetWindowId=$targetWindowId")
        }

        llmJob?.cancel()
        llmJob = serviceScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            val downloader = ModelDownloader(this@OrbitBubbleService)
            val selectedModelId = ToolSettingsStore(this@OrbitBubbleService).bubbleModelId
            val model = AVAILABLE_MODELS.firstOrNull { it.id == selectedModelId && downloader.isDownloaded(it) }
                ?: AVAILABLE_MODELS.firstOrNull { downloader.isDownloaded(it) }

            if (model == null) {
                withContext(Dispatchers.Main) {
                    showResultOverlay(initialTranscript, cancelExistingJob = false)
                    updateResultStatus("Model unavailable")
                    updateResultText("No model downloaded.\nOpen Orbit → Settings → Model to download one.")
                }
                return@launch
            }

            val settings = InferenceSettingsStore(this@OrbitBubbleService).get()

            try {
                if (model.format != ModelFormat.LITERTLM || model.provider != ModelProvider.LOCAL) {
                    withContext(Dispatchers.Main) {
                        showResultOverlay(initialTranscript, cancelExistingJob = false)
                    }
                    runLegacyOverlayStreaming(
                        initialTranscript = initialTranscript,
                        modelId = model.id,
                        settings = settings,
                        startedAt = startedAt,
                        model = model,
                    )
                    return@launch
                }

                val runtime = bubbleLiteRtRuntime
                    ?: OrbitBubbleLiteRtRuntime(this@OrbitBubbleService).also { bubbleLiteRtRuntime = it }
                val loadedRuntime = runtime.ensureLoaded(model, settings)
                Log.d(
                    logTag,
                    "Native LiteRT bubble ready model=${model.id} backend=${loadedRuntime.backendLabel}",
                )

                if (isScreenIntent) {
                    runDedicatedScreenUnderstanding(
                        initialTranscript = initialTranscript,
                        targetWindowId = targetWindowId,
                        runtime = runtime,
                        settings = settings,
                        startedAt = startedAt,
                    )
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    showResultOverlay(initialTranscript, cancelExistingJob = false)
                    updateResultStatus("Thinking...")
                    updateResultText("")
                }

                val systemInstruction = """
                    You are Orbit AI running fully on-device in an Android floating bubble.
                    Answer the user directly unless a tool is truly required.
                    Use inject_into_textfield only when the user wants you to write, paste, reply, or fill text into the currently focused text field.
                    Use take_screenshot only when the user is asking about what is currently visible on-screen and visual context is required.
                    Never expose raw tool names or tool arguments to the user.
                    Keep answers concise by default.
                    Call at most one tool in a turn.
                """.trimIndent()

                runtime.createConversation(
                    systemInstruction = systemInstruction,
                    settings = settings,
                    enableTools = true,
                ).use { conversation ->
                    val firstTurn = runtime.streamTurn(
                        conversation = conversation,
                        contents = runtime.textContents(initialTranscript),
                        maxDecodedTokens = settings.maxDecodedTokens,
                    ) { delta ->
                        withContext(Dispatchers.Main) {
                            appendResultText(delta)
                        }
                    }

                    val firstToolCall = firstTurn.toolCalls.firstOrNull()
                    if (firstToolCall == null) {
                        withContext(Dispatchers.Main) {
                            updateResultStatus("Done in ${formatElapsed(startedAt)}")
                            if (firstTurn.text.isBlank()) {
                                updateResultText("I couldn't generate a response.")
                            }
                        }
                        return@launch
                    }

                    Log.d(logTag, "Structured tool selected: ${firstToolCall.name}")
                    withContext(Dispatchers.Main) { updateResultText("") }

                    when (firstToolCall.name) {
                        "inject_into_textfield" -> {
                            handleInjectToolCall(firstToolCall, startedAt)
                        }

                        "take_screenshot" -> {
                            withContext(Dispatchers.Main) { updateResultStatus("Capturing screen...") }
                            val capture = captureBestScreenBitmap(targetWindowId = null)
                            val bitmap = capture.bitmap
                            if (bitmap == null) {
                                withContext(Dispatchers.Main) {
                                    updateResultStatus("Screenshot unavailable")
                                    updateResultText("I couldn't capture the current screen. Make sure Orbit Accessibility is enabled and try again.")
                                }
                                return@launch
                            }

                            try {
                                withContext(Dispatchers.Main) {
                                    updateResultStatus(
                                        if (capture.source == ScreenCaptureSource.WINDOW) {
                                            "Analyzing screenshot..."
                                        } else {
                                            "Analyzing screenshot (fallback)..."
                                        }
                                    )
                                    updateResultText("")
                                }
                                val followUpTurn = runtime.streamTurn(
                                    conversation = conversation,
                                    contents = runtime.imagePromptContents(
                                        bitmap,
                                        buildScreenUnderstandingPrompt(initialTranscript),
                                    ),
                                    maxDecodedTokens = settings.maxDecodedTokens,
                                ) { delta ->
                                    withContext(Dispatchers.Main) {
                                        appendResultText(delta)
                                    }
                                }

                                val followUpToolCall = followUpTurn.toolCalls.firstOrNull()
                                if (followUpToolCall?.name == "inject_into_textfield") {
                                    handleInjectToolCall(followUpToolCall, startedAt)
                                } else {
                                    withContext(Dispatchers.Main) {
                                        updateResultStatus("Done in ${formatElapsed(startedAt)}")
                                        if (followUpTurn.text.isBlank()) {
                                            updateResultText("I couldn't read a useful answer from the current screen.")
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
                                updateResultStatus("Unsupported tool")
                                updateResultText("Orbit requested an unsupported tool: ${firstToolCall.name}")
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
                    if (!isResultVisible) {
                        showResultOverlay(initialTranscript, cancelExistingJob = false)
                    }
                    updateResultStatus("Failed")
                    updateResultText("Something went wrong: ${e.message}")
                }
            }
        }
    }

    private suspend fun runLegacyOverlayStreaming(
        initialTranscript: String,
        modelId: String,
        settings: com.example.orbitai.data.InferenceSettings,
        startedAt: Long,
        model: com.example.orbitai.data.LlmModel,
    ) {
        val repo = bubbleLlmRepo ?: LlmRepository(this@OrbitBubbleService).also { bubbleLlmRepo = it }
        if (!repo.isModelLoaded(modelId, settings)) {
            withContext(Dispatchers.Main) { updateResultStatus("Loading model...") }
            repo.loadModel(model, settings)
        }

        withContext(Dispatchers.Main) {
            ensureExpandedResultOverlay()
            updateResultStatus("Thinking...")
            updateResultText("")
        }

        repo.generateResponseStream(
            input = com.example.orbitai.data.InferenceInput(prompt = initialTranscript),
            maxDecodedTokens = settings.maxDecodedTokens,
        ).collect { token ->
            withContext(Dispatchers.Main) {
                appendResultText(token)
            }
        }

        withContext(Dispatchers.Main) {
            updateResultStatus("Done in ${formatElapsed(startedAt)}")
            if ((resultTextView?.text?.toString() ?: "").isBlank()) {
                updateResultText("I couldn't generate a response.")
            }
        }
    }

    private suspend fun runDedicatedScreenUnderstanding(
        initialTranscript: String,
        targetWindowId: Int?,
        runtime: OrbitBubbleLiteRtRuntime,
        settings: com.example.orbitai.data.InferenceSettings,
        startedAt: Long,
    ) {
        val capture = captureBestScreenBitmap(targetWindowId)
        val bitmap = capture.bitmap

        withContext(Dispatchers.Main) {
            showResultOverlay(initialTranscript, cancelExistingJob = false)
            updateResultStatus(
                when (capture.source) {
                    ScreenCaptureSource.WINDOW -> "Analyzing screenshot..."
                    ScreenCaptureSource.DISPLAY_FALLBACK -> "Analyzing screenshot (fallback)..."
                    ScreenCaptureSource.UNAVAILABLE -> "Screenshot unavailable"
                }
            )
            updateResultText("")
        }

        if (bitmap == null) {
            withContext(Dispatchers.Main) {
                updateResultText("I couldn't capture the current screen. Make sure Orbit Accessibility is enabled and try again.")
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
            runtime.createConversation(
                systemInstruction = systemInstruction,
                settings = settings,
                enableTools = false,
            ).use { conversation ->
                val analysisTurn = runtime.streamTurn(
                    conversation = conversation,
                    contents = runtime.imagePromptContents(
                        bitmap = bitmap,
                        promptText = buildScreenUnderstandingPrompt(initialTranscript),
                    ),
                    maxDecodedTokens = settings.maxDecodedTokens,
                ) { delta ->
                    withContext(Dispatchers.Main) {
                        appendResultText(delta)
                    }
                }

                withContext(Dispatchers.Main) {
                    updateResultStatus("Done in ${formatElapsed(startedAt)}")
                    if (analysisTurn.text.isBlank()) {
                        updateResultText("I couldn't read a useful answer from the current screen.")
                    }
                }
            }
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private suspend fun handleInjectToolCall(
        toolCall: com.google.ai.edge.litertlm.ToolCall,
        startedAt: Long,
    ) {
        val textToInject = toolCall.arguments["text"]?.toString()?.takeIf { it.isNotBlank() }
        if (textToInject.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                updateResultStatus("Missing tool input")
                updateResultText("Orbit requested text insertion but did not return any text to insert.")
            }
            return
        }

        withContext(Dispatchers.Main) { updateResultStatus("Writing into field...") }
        val injected = OrbitAccessibilityService.instance?.injectTextIntoActiveField(textToInject) == true
        withContext(Dispatchers.Main) {
            if (injected) {
                updateResultStatus("Inserted in ${formatElapsed(startedAt)}")
                updateResultText(textToInject)
                Toast.makeText(this@OrbitBubbleService, "Text inserted", Toast.LENGTH_SHORT).show()
            } else {
                updateResultStatus("No text field found")
                updateResultText(
                    "I generated the text, but I couldn't find an active text field to insert it into.\n\n$textToInject",
                )
                Toast.makeText(this@OrbitBubbleService, "No active text box found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun captureBestScreenBitmap(targetWindowId: Int?): ScreenCaptureResult {
        val service = OrbitAccessibilityService.instance
        if (service == null) {
            return ScreenCaptureResult(bitmap = null, source = ScreenCaptureSource.UNAVAILABLE)
        }

        val windowBitmap = suspendCaptureBitmap { callback ->
            when {
                targetWindowId != null -> service.captureWindowAsBitmap(targetWindowId, callback)
                else -> service.captureActiveWindowAsBitmap(callback)
            }
        }
        if (windowBitmap != null) {
            Log.d(logTag, "Using window-only screenshot path for screen understanding targetWindowId=$targetWindowId")
            return ScreenCaptureResult(bitmap = windowBitmap, source = ScreenCaptureSource.WINDOW)
        }

        Log.w(logTag, "Window screenshot unavailable; falling back to full-display capture targetWindowId=$targetWindowId")
        val displayBitmap = captureDisplayBitmapWithOrbitUiHidden()
        return ScreenCaptureResult(
            bitmap = displayBitmap,
            source = if (displayBitmap != null) {
                ScreenCaptureSource.DISPLAY_FALLBACK
            } else {
                ScreenCaptureSource.UNAVAILABLE
            },
        )
    }

    private suspend fun captureDisplayBitmapWithOrbitUiHidden(): Bitmap? {
        withContext(Dispatchers.Main) {
            setOrbitOverlayVisibility(isVisible = false)
        }
        delay(90)
        return try {
            suspendCaptureBitmap { callback ->
                OrbitAccessibilityService.instance?.captureScreenAsBitmap(callback) ?: callback(null)
            }
        } finally {
            withContext(Dispatchers.Main) {
                setOrbitOverlayVisibility(isVisible = true)
            }
        }
    }

    private suspend fun suspendCaptureBitmap(
        request: ((Bitmap?) -> Unit) -> Unit,
    ): Bitmap? {
        return suspendCancellableCoroutine { continuation ->
            request { bitmap ->
                continuation.resumeWith(Result.success(bitmap))
            }
        }
    }

    private fun setOrbitOverlayVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        resultCardView?.visibility = visibility
        bubbleView?.visibility = visibility
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

    // ── Result overlay ────────────────────────────────────────────────────────

    private fun showResultOverlay(
        transcript: String,
        cancelExistingJob: Boolean = true,
        compact: Boolean = false,
    ) {
        hideResultOverlay(cancelJob = cancelExistingJob)
        isResultCompactMode = compact
        val (screenW, screenH) = getScreenSize()
        val cardW = screenW - dpToPx(32f)
        val responseHeightPx = dpToPx(ToolSettingsStore(this).bubbleResponseHeightDp.toFloat())
        val bubbleY = bubbleParams?.y ?: dpToPx(200f)
        val cardY = if (bubbleY < screenH / 2)
            (bubbleY + bubbleSizePx + dpToPx(10f))
        else
            (bubbleY - (responseHeightPx + dpToPx(94f))).coerceAtLeast(dpToPx(24f))
        val card = buildResultCardView(transcript, responseHeightPx, compact)
        val params = WindowManager.LayoutParams(
            cardW,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dpToPx(16f); y = cardY }
        resultCardView = card
        resultCardParams = params
        windowManager?.addView(card, params)
        isResultVisible = true
    }

    private fun hideResultOverlay(cancelJob: Boolean = true) {
        if (cancelJob) llmJob?.cancel()
        if (isResultVisible) {
            try { windowManager?.removeView(resultCardView) } catch (_: Exception) {}
        }
        resultCardView = null
        resultCardParams = null
        resultStatusView = null
        resultTextView = null
        resultScrollView = null
        isResultVisible = false
        isResultCompactMode = false
    }

    // ── Radial Menu ───────────────────────────────────────────────────────────
    private fun hideRadialMenu() {
        if (isRadialMenuVisible) {
            try { windowManager?.removeView(radialMenuView) } catch (_: Exception) {}
        }
        radialMenuView = null
        isRadialMenuVisible = false
    }

    private fun showRadialMenu() {
        if (isRadialMenuVisible) return
        val bParams = bubbleParams ?: return
        val (screenW, screenH) = getScreenSize()
        val centerX = bParams.x + bubbleSizePx / 2
        val centerY = bParams.y + bubbleSizePx / 2

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(120, 0, 0, 0))
            isClickable = true
            isFocusable = true
            setOnClickListener { hideRadialMenu() }
        }

        val radius = dpToPx(72f).toFloat() // distance from bubble center
        val isOnLeft = centerX < screenW / 2
        val angles = if (isOnLeft) {
            listOf(-70.0, -35.0, 0.0, 35.0, 70.0)
        } else {
            listOf(250.0, 215.0, 180.0, 145.0, 110.0)
        }

        val actions = listOf(
            Triple("Summarize", "S", Color.parseColor("#8C52FF")), // Violet
            Triple("Translate", "T", Color.parseColor("#00B4D8")), // Cyan
            Triple("Explain",   "E", Color.parseColor("#10B981")), // Emerald
            Triple("Reply",     "R", Color.parseColor("#F59E0B")), // Orange
            Triple("Close",     "✕", Color.parseColor("#EF4444"))  // Red
        )
        val btnSize = dpToPx(38f)

        for (i in actions.indices) {
            val (actionName, initial, colorInt) = actions[i]
            val angleRad = Math.toRadians(angles[i])
            val dx = (Math.cos(angleRad) * radius).toInt()
            val dy = (Math.sin(angleRad) * radius).toInt()

            val btnX = centerX + dx - btnSize / 2
            val btnY = centerY + dy - btnSize / 2

            val btn = TextView(this).apply {
                layoutParams = FrameLayout.LayoutParams(btnSize, btnSize).apply {
                    leftMargin = btnX
                    topMargin = btnY
                }
                text = initial
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.argb(190, Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt)))
                    setStroke(dpToPx(1f), Color.argb(120, 255, 255, 255))
                }
                elevation = 16f
                setOnClickListener {
                    hideRadialMenu()
                    when (actionName) {
                        "Summarize" -> handleTranscript("Summarize this context")
                        "Translate" -> handleTranscript("Translate this to English")
                        "Explain" -> handleTranscript("Explain this context")
                        "Reply" -> handleTranscript("Write a reply to this based on its context")
                        "Close" -> dismissBubble()
                    }
                }
            }
            overlay.addView(btn)
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        radialMenuView = overlay
        windowManager?.addView(overlay, layoutParams)
        isRadialMenuVisible = true
    }

    private fun ensureExpandedResultOverlay() {
        if (!isResultVisible || !isResultCompactMode) return

        val currentStatus = resultStatusView?.text?.toString() ?: "Thinking..."
        val currentText = resultTextView?.text?.toString() ?: ""
        val oldParams = resultCardParams
        hideResultOverlay(cancelJob = false)
        val responseHeightPx = dpToPx(ToolSettingsStore(this).bubbleResponseHeightDp.toFloat())
        val card = buildResultCardView(lastTranscript, responseHeightPx, compact = false)
        resultCardView = card
        resultCardParams = oldParams
        if (oldParams != null) {
            windowManager?.addView(card, oldParams)
        }
        isResultVisible = true
        isResultCompactMode = false
        resultStatusView?.text = currentStatus
        resultTextView?.text = parseSimpleMarkdown(currentText)
    }

    @Suppress("SetTextI18n")
    private fun buildResultCardView(
        transcript: String,
        responseHeightPx: Int,
        compact: Boolean,
    ): View {
        fun dp(v: Int): Int = dpToPx(v.toFloat())

        val store = ToolSettingsStore(this)
        val theme = getThemeConfig(store.bubbleResponseTheme)
        val alphaInt = (store.bubbleResponseAlphaPercent / 100f * 255).toInt()

        val frame = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: android.view.KeyEvent?): Boolean {
                if (event?.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                    hideResultOverlay()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }.apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                setColor(Color.argb(alphaInt, theme.bgRGB.first, theme.bgRGB.second, theme.bgRGB.third))
                setStroke(dp(1), Color.argb(132, theme.borderRGB.first, theme.borderRGB.second, theme.borderRGB.third))
            }
            elevation = 24f
            isFocusableInTouchMode = true
        }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        frame.addView(root, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ))

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(10), dp(10), dp(8))
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(8) }
            setImageResource(R.drawable.vector_logo)
            imageTintList = android.content.res.ColorStateList.valueOf(theme.primaryText)
        })
        header.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Orbit AI"
            setTextColor(theme.primaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
        })

        // Theme switchers
        val themeSwitcherContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(8)
            }
        }
        val quickThemes = listOf(
            "dark_glassy" to Color.parseColor("#333333"),
            "white_glassy" to Color.parseColor("#F5F5F5"),
            "violet" to Color.parseColor("#6842A6")
        )
        quickThemes.forEach { (id, btnColor) ->
            themeSwitcherContainer.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply { marginEnd = dp(6) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(btnColor)
                    if (id == "white_glassy") setStroke(dp(1), Color.LTGRAY)
                }
                setOnClickListener {
                    store.bubbleResponseTheme = id
                    val currentStatus = resultStatusView?.text?.toString() ?: "Thinking..."
                    val currentText = resultTextView?.text?.toString() ?: ""
                    val oldParams = resultCardParams
                    hideResultOverlay(cancelJob = false)
                    val card = buildResultCardView(
                        lastTranscript,
                        responseHeightPx,
                        compact = isResultCompactMode,
                    )
                    resultCardView = card
                    resultCardParams = oldParams
                    if (oldParams != null) windowManager?.addView(card, oldParams)
                    isResultVisible = true
                    isResultCompactMode = compact
                    resultStatusView?.text = currentStatus
                    resultTextView?.text = parseSimpleMarkdown(currentText)
                }
            })
        }
        if (!compact) {
            header.addView(themeSwitcherContainer)
        }

        val opacitySlider = android.widget.SeekBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(50), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(8)
            }
            max = 90
            progress = store.bubbleResponseAlphaPercent - 10
            progressDrawable?.setTint(theme.primaryText)
            thumb?.setTint(theme.primaryText)
            
            setOnSeekBarChangeListener(object: android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val newAlpha = progress + 10
                        store.bubbleResponseAlphaPercent = newAlpha
                        frame.background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dp(20).toFloat()
                            val alphaInt = (newAlpha / 100f * 255).toInt()
                            setColor(Color.argb(alphaInt, theme.bgRGB.first, theme.bgRGB.second, theme.bgRGB.third))
                            setStroke(dp(1), Color.argb(132, theme.borderRGB.first, theme.borderRGB.second, theme.borderRGB.third))
                        }
                    }
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })
        }
        if (!compact) {
            header.addView(opacitySlider)
        }

        val openBtn = TextView(this).apply {
            text = "Open"
            setTextColor(theme.primaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(9).toFloat()
                setColor(Color.argb(52, 188, 150, 255))
                setStroke(1, Color.argb(120, 224, 206, 255))
            }
        }
        openBtn.setOnClickListener { hideResultOverlay(); launchApp(lastTranscript) }
        if (!compact) {
            header.addView(openBtn)
        }
        val closeBtn = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(30))
            text = "✕"
            setTextColor(theme.primaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
        }
        closeBtn.setOnClickListener { hideResultOverlay() }
        header.addView(closeBtn)
        root.addView(header)

        val statusText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(dp(14), 0, dp(14), dp(6)) }
            setTextColor(theme.secondaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            text = "Thinking..."
        }
        root.addView(statusText)

        // Drag response card anywhere (bounded to visible area)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var moved = false
        header.setOnTouchListener { _, event ->
            val params = resultCardParams ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (!moved && (kotlin.math.abs(deltaX) > touchSlop || kotlin.math.abs(deltaY) > touchSlop)) {
                        moved = true
                    }
                    if (moved) {
                        val (screenW, screenH) = getScreenSize()
                        val cardW = params.width
                        val cardH = (resultCardView?.height ?: (responseHeightPx + dp(100))).coerceAtLeast(dp(140))
                        params.x = (initialX + deltaX).coerceIn(edgeDockMarginPx, (screenW - cardW - edgeDockMarginPx).coerceAtLeast(edgeDockMarginPx))
                        params.y = (initialY + deltaY).coerceIn(edgeDockMarginPx, (screenH - cardH - edgeDockMarginPx).coerceAtLeast(edgeDockMarginPx))
                        resultCardView?.let { windowManager?.updateViewLayout(it, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> moved
                else -> false
            }
        }

        // Transcript snippet
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(Color.argb(70, 255, 255, 255))
            visibility = if (compact) View.GONE else View.VISIBLE
        })
        root.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(dp(14), dp(6), dp(14), 0) }
            text = "\"${transcript.take(80)}${if (transcript.length > 80) "\u2026" else "\""}"
            setTextColor(theme.secondaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            maxLines = 2
            visibility = if (compact) View.GONE else View.VISIBLE
        })

        // Response body
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, responseHeightPx)
            isVerticalScrollBarEnabled = false
            visibility = if (compact) View.GONE else View.VISIBLE
        }
        val responseText = TextView(this).apply {
            setPadding(dp(14), dp(8), dp(14), dp(10))
            setTextColor(theme.primaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setLineSpacing(dp(2).toFloat(), 1f)
            setTextIsSelectable(true)
            text = ""
        }
        scrollView.addView(responseText)
        root.addView(scrollView)
        resultStatusView = statusText
        resultTextView = responseText
        resultScrollView = scrollView
        return frame
    }

    private fun updateResultStatus(text: String) {
        resultStatusView?.text = text
    }

    private fun appendResultText(delta: String) {
        val currentText = resultTextView?.text?.toString().orEmpty()
        updateResultText(currentText + delta)
    }

    private fun updateResultText(text: String) {
        resultTextView?.text = parseSimpleMarkdown(text)
        resultScrollView?.post { resultScrollView?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun parseSimpleMarkdown(text: String): android.text.Spanned {
        // Convert markdown to Android Spanned for basic formatting
        // Support: **bold**, *italic*, `code`, # heading
        val pattern = Regex("""(\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`|^#+\s.+$)""", RegexOption.MULTILINE)
        
        val html = text
            .replace(Regex("""^(#+)\s+(.+)$""", RegexOption.MULTILINE)) { match ->
                val level = match.groupValues[1].length
                val text = match.groupValues[2]
                val size = when (level) {
                    1 -> "1.5em"
                    2 -> "1.3em"
                    else -> "1.1em"
                }
                "<b style=\"font-size:$size\">$text</b><br>"
            }
            .replace(Regex("""\*\*([^*]+)\*\*"""), "<b>$1</b>")
            .replace(Regex("""\*([^*]+)\*"""), "<i>$1</i>")
            .replace(Regex("""`([^`]+)`"""), "<tt>$1</tt>")
            .replace("\n", "<br>")
        
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(html)
        }
    }

    private fun updateBubbleUi(isActive: Boolean) {
        val bubble = bubbleView ?: return
        bubble.background = bubbleBackground(isActive)
        if (isActive) {
            startPulseAnimation(bubble)
        } else {
            stopPulseAnimation()
            bubble.scaleX = 1f
            bubble.scaleY = 1f
        }
    }

    private fun startPulseAnimation(target: View) {
        stopPulseAnimation()
        pulseAnimator = android.animation.ValueAnimator.ofFloat(1f, 1.12f).apply {
            duration = 620L
            repeatCount = android.animation.ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                target.scaleX = scale
                target.scaleY = scale
            }
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    private fun bubbleBackground(isActive: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = if (bubbleStyle == "slide") GradientDrawable.RECTANGLE else GradientDrawable.OVAL
            colors = if (isActive) {
                intArrayOf(Color.parseColor("#FF5B5B"), Color.parseColor("#FF8A65"))
            } else {
                intArrayOf(Color.parseColor("#7A6BFF"), Color.parseColor("#5A9CFF"))
            }
            if (bubbleStyle == "slide") {
                cornerRadii = floatArrayOf(
                    dpToPx(14f).toFloat(), dpToPx(14f).toFloat(),
                    0f, 0f,
                    0f, 0f,
                    dpToPx(14f).toFloat(), dpToPx(14f).toFloat(),
                )
            }
            setStroke(3, Color.argb(if (isActive) 160 else 110, 255, 255, 255))
        }
    }

    private fun snapToNearestEdge(animated: Boolean) {
        val params = bubbleParams ?: return
        val (screenW, screenH) = getScreenSize()
        val bubbleW = (bubbleView?.width ?: params.width).coerceAtLeast(1)
        val bubbleH = (bubbleView?.height ?: params.height).coerceAtLeast(1)
        val maxX = (screenW - bubbleW - edgeDockMarginPx).coerceAtLeast(edgeDockMarginPx)
        val maxY = (screenH - bubbleH - edgeDockMarginPx).coerceAtLeast(edgeDockMarginPx)
        val leftX = edgeDockMarginPx
        val rightX = maxX
        val centerX = screenW / 2
        val currentX = params.x
        val targetX = if (bubbleStyle == "slide") {
            (screenW - (bubbleW / 2)).coerceAtLeast(0)
        } else if (currentX + bubbleW / 2 < centerX) {
            leftX
        } else {
            rightX
        }
        val clampedY = params.y.coerceIn(edgeDockMarginPx, maxY)

        if (!animated) {
            params.x = targetX
            params.y = clampedY
            bubbleView?.let { windowManager?.updateViewLayout(it, params) }
            return
        }

        val startX = currentX
        val animator = android.animation.ValueAnimator.ofInt(startX, targetX)
        animator.duration = 220L
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            params.x = value
            params.y = clampedY
            bubbleView?.let { view -> windowManager?.updateViewLayout(view, params) }
        }
        animator.start()
    }

    private fun enterIdleVisualMode() {
        bubbleView?.alpha = bubbleIdleAlpha
    }

    private fun enterActiveVisualMode() {
        bubbleView?.alpha = 1f
    }

    private fun dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density + 0.5f).toInt()

    private fun getScreenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager!!.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            val m = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager!!.defaultDisplay.getMetrics(m)
            Pair(m.widthPixels, m.heightPixels)
        }
    }

    private fun launchApp(transcript: String? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (!transcript.isNullOrBlank()) {
                putExtra(MainActivity.EXTRA_OVERLAY_TRANSCRIPT, transcript)
            }
        }
        startActivity(intent)
    }

    private fun dismissBubble() {
        hideResultOverlay()
        ToolSettingsStore(this).isFloatingBubbleEnabled = false
        stopSelf()
    }

    private fun startBubbleForeground(isListening: Boolean) {
        val notification = buildNotification(isListening)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(isListening: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OrbitBubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isListening) "Orbit bubble is listening" else "Orbit bubble is ready")
            .setContentText(if (isListening) "Speak now. Orbit will send your transcript to chat." else "Tap the bubble to speak, or long press it to dismiss.")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Orbit Bubble",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps the Orbit floating bubble visible over other apps"
            },
        )
    }

    private inner class BubbleRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onResults(results: Bundle?) {
            partialTranscript = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            stopListening(submitTranscript = true)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialTranscript = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
        }

        override fun onError(error: Int) {
            val shouldSubmit = partialTranscript.isNotBlank() || error !in setOf(
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            )
            if (!shouldSubmit) {
                Toast.makeText(this@OrbitBubbleService, "No speech detected.", Toast.LENGTH_SHORT).show()
            }
            stopListening(submitTranscript = partialTranscript.isNotBlank())
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    companion object {
        data class BubbleThemeConfig(
            val bgRGB: Triple<Int, Int, Int>,
            val borderRGB: Triple<Int, Int, Int>,
            val primaryText: Int,
            val secondaryText: Int
        )

        fun getThemeConfig(themeId: String): BubbleThemeConfig {
            return when (themeId) {
                "dark_glassy" -> BubbleThemeConfig(Triple(25, 25, 25), Triple(100, 100, 100), Color.WHITE, Color.parseColor("#CCCCCC"))
                "white_glassy" -> BubbleThemeConfig(Triple(245, 245, 245), Triple(200, 200, 200), Color.BLACK, Color.parseColor("#555555"))
                "emerald" -> BubbleThemeConfig(Triple(16, 89, 65), Triple(105, 204, 171), Color.WHITE, Color.parseColor("#D1EAE1"))
                "ocean" -> BubbleThemeConfig(Triple(25, 75, 120), Triple(122, 186, 245), Color.WHITE, Color.parseColor("#D6EAFF"))
                "sunset" -> BubbleThemeConfig(Triple(143, 44, 19), Triple(245, 137, 110), Color.WHITE, Color.parseColor("#FFDBCF"))
                "midnight" -> BubbleThemeConfig(Triple(9, 13, 26), Triple(64, 80, 122), Color.WHITE, Color.parseColor("#B0C1EE"))
                "violet" -> BubbleThemeConfig(Triple(104, 66, 166), Triple(193, 168, 255), Color.WHITE, Color.parseColor("#E9DBFF"))
                else -> BubbleThemeConfig(Triple(104, 66, 166), Triple(193, 168, 255), Color.WHITE, Color.parseColor("#E9DBFF"))
            }
        }

        private const val ACTION_START = "com.example.orbitai.tools.bubble.START"
        private const val ACTION_TRIGGER = "com.example.orbitai.tools.bubble.TRIGGER"
        const val ACTION_INJECT_TEXT = "com.example.orbitai.tools.bubble.INJECT_TEXT"
        const val EXTRA_INJECTED_TEXT = "com.example.orbitai.tools.bubble.EXTRA_TEXT"
        private const val ACTION_APP_FOREGROUND = "com.example.orbitai.tools.bubble.APP_FOREGROUND"
        private const val ACTION_APP_BACKGROUND = "com.example.orbitai.tools.bubble.APP_BACKGROUND"
        private const val ACTION_STOP = "com.example.orbitai.tools.bubble.STOP"
        private const val NOTIFICATION_CHANNEL_ID = "orbit_bubble"
        private const val NOTIFICATION_ID = 4201
        const val SIZE_SMALL_DP  = 52
        const val SIZE_MEDIUM_DP = 64
        const val SIZE_LARGE_DP  = 80

        fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

        fun start(context: Context) {
            val intent = Intent(context, OrbitBubbleService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun trigger(context: Context) {
            val intent = Intent(context, OrbitBubbleService::class.java).setAction(ACTION_TRIGGER)
            ContextCompat.startForegroundService(context, intent)
        }

        fun setAppForeground(context: Context, isForeground: Boolean) {
            val action = if (isForeground) ACTION_APP_FOREGROUND else ACTION_APP_BACKGROUND
            val intent = Intent(context, OrbitBubbleService::class.java).setAction(action)
            context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OrbitBubbleService::class.java))
        }

        fun overlayPermissionIntent(context: Context): Intent {
            return Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
        }
    }
}
