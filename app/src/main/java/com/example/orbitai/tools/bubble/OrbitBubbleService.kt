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
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import com.example.orbitai.data.Message
import com.example.orbitai.data.ModelDownloader
import com.example.orbitai.data.Role
import com.example.orbitai.data.ToolSettingsStore
import com.example.orbitai.prompts.GemmaChatPromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrbitBubbleService : Service() {

    // ── Window / bubble ───────────────────────────────────────────────────────
    private var windowManager: WindowManager? = null
    private var bubbleView: FrameLayout? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var isBubbleAttached = false

    // ── Result overlay ────────────────────────────────────────────────────────
    private var resultCardView: View? = null
    private var resultTextView: TextView? = null
    private var resultScrollView: ScrollView? = null
    private var isResultVisible = false
    private var lastTranscript: String = ""

    // ── Speech ────────────────────────────────────────────────────────────────
    private var speechRecognizer: SpeechRecognizer? = null
    private var pulseAnimator: android.animation.ValueAnimator? = null
    private var partialTranscript: String = ""
    private var isListening = false

    // ── LLM (overlay mode) ────────────────────────────────────────────────────
    private var bubbleLlmRepo: LlmRepository? = null
    private var llmJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Config (read from settings on each onStartCommand) ───────────────────
    private var bubbleSizePx = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { dismissBubble(); return START_NOT_STICKY }
            ACTION_START, null -> Unit
            else -> return START_NOT_STICKY
        }

        if (!canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }

        val newSizePx = dpToPx(ToolSettingsStore(this).bubbleSizeDp.toFloat())
        if (isBubbleAttached && newSizePx != bubbleSizePx) {
            removeBubble()  // will re-attach below with new size
        }
        bubbleSizePx = newSizePx

        startBubbleForeground(isListening = false)

        if (!isBubbleAttached) {
            attachBubble()
            Toast.makeText(this, "Tap to speak. Long press to dismiss.", Toast.LENGTH_SHORT).show()
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

        val iconPad = dpToPx(12f)
        val bubble = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(bubbleSizePx, bubbleSizePx)
            background = bubbleBackground(isActive = false)
            elevation = 32f
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

        val longPressRunnable = Runnable {
            longPressTriggered = true
            dismissBubble()
        }

        bubble.setOnTouchListener { _, event ->
            val params = bubbleParams ?: return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
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
                        params.x = (initialX + deltaX).coerceIn(0, (screenW - bubbleSizePx).coerceAtLeast(0))
                        params.y = (initialY + deltaY).coerceIn(0, (screenH - bubbleSizePx).coerceAtLeast(0))
                        windowManager?.updateViewLayout(bubble, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    bubble.removeCallbacks(longPressRunnable)
                    if (!moved && !longPressTriggered) {
                        toggleListening()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    bubble.removeCallbacks(longPressRunnable)
                    true
                }
                else -> false
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(16f)
            y = dpToPx(200f)
        }

        bubbleView = bubble
        bubbleParams = layoutParams
        windowManager?.addView(bubble, layoutParams)
        isBubbleAttached = true
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
        startBubbleForeground(isListening = false)

        if (submitTranscript && transcript.isNotBlank()) {
            handleTranscript(transcript)
        }
    }

    // ── Transcript routing ────────────────────────────────────────────────────

    private fun handleTranscript(transcript: String) {
        lastTranscript = transcript
        if (ToolSettingsStore(this).bubbleResultsInOverlay) runInlineInference(transcript)
        else launchApp(transcript)
    }

    // ── Inline LLM inference ──────────────────────────────────────────────────

    private fun runInlineInference(transcript: String) {
        showResultOverlay(transcript)
        llmJob?.cancel()
        llmJob = serviceScope.launch {
            val model = AVAILABLE_MODELS.firstOrNull {
                ModelDownloader(this@OrbitBubbleService).isDownloaded(it)
            }
            if (model == null) {
                withContext(Dispatchers.Main) {
                    updateResultText("No model downloaded.\nOpen Orbit → Settings → Model to download one.")
                }
                return@launch
            }
            val settings = InferenceSettingsStore(this@OrbitBubbleService).get()
            val repo = bubbleLlmRepo
                ?: LlmRepository(this@OrbitBubbleService).also { bubbleLlmRepo = it }
            try {
                if (!repo.isModelLoaded(model.id, settings)) {
                    withContext(Dispatchers.Main) { updateResultText("Loading model…") }
                    repo.loadModel(model, settings)
                }
                val prompt = GemmaChatPromptBuilder.build(
                    messages = listOf(Message(role = Role.USER, content = transcript)),
                )
                var accumulated = ""
                repo.generateResponseStream(prompt, settings.maxDecodedTokens).collect { token ->
                    accumulated += token
                    withContext(Dispatchers.Main) { updateResultText(accumulated) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateResultText("Something went wrong: ${e.message}")
                }
            }
        }
    }

    // ── Result overlay ────────────────────────────────────────────────────────

    private fun showResultOverlay(transcript: String) {
        hideResultOverlay()
        val (screenW, screenH) = getScreenSize()
        val cardW = screenW - dpToPx(32f)
        val bubbleY = bubbleParams?.y ?: dpToPx(200f)
        val cardY = if (bubbleY < screenH / 2)
            (bubbleY + bubbleSizePx + dpToPx(10f))
        else
            (bubbleY - dpToPx(300f)).coerceAtLeast(dpToPx(24f))
        val card = buildResultCardView(transcript)
        val params = WindowManager.LayoutParams(
            cardW,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dpToPx(16f); y = cardY }
        resultCardView = card
        windowManager?.addView(card, params)
        isResultVisible = true
    }

    private fun hideResultOverlay() {
        llmJob?.cancel()
        if (isResultVisible) {
            try { windowManager?.removeView(resultCardView) } catch (_: Exception) {}
        }
        resultCardView = null
        resultTextView = null
        resultScrollView = null
        isResultVisible = false
    }

    @Suppress("SetTextI18n")
    private fun buildResultCardView(transcript: String): View {
        fun dp(v: Int): Int = dpToPx(v.toFloat())

        val frame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#1E1530"))
                setStroke(dp(1), Color.parseColor("#6D5EF5"))
            }
            elevation = 24f
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
            imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#8B5CF6"))
        })
        header.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Orbit AI"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
        })
        val closeBtn = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(30))
            text = "✕"
            setTextColor(Color.parseColor("#888888"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
        }
        closeBtn.setOnClickListener { hideResultOverlay() }
        header.addView(closeBtn)
        root.addView(header)

        // Transcript snippet
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(Color.parseColor("#2A1E45"))
        })
        root.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(dp(14), dp(6), dp(14), 0) }
            text = "\"${transcript.take(80)}${if (transcript.length > 80) "\u2026" else "\""}"
            setTextColor(Color.parseColor("#9580C2"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            maxLines = 2
        })

        // Response body
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(180))
            isVerticalScrollBarEnabled = false
        }
        val responseText = TextView(this).apply {
            setPadding(dp(14), dp(8), dp(14), dp(10))
            setTextColor(Color.parseColor("#E8E0F7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setLineSpacing(dp(2).toFloat(), 1f)
            text = "Thinking…"
        }
        scrollView.addView(responseText)
        root.addView(scrollView)
        resultTextView = responseText
        resultScrollView = scrollView

        // Footer
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(0, dp(4), 0, 0)
            }
            setBackgroundColor(Color.parseColor("#2A1E45"))
        })
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(8), dp(14), dp(12))
            gravity = Gravity.END
        }
        val openBtn = TextView(this).apply {
            text = "Open in Chat  →"
            setTextColor(Color.parseColor("#A78BF7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(12), dp(7), dp(12), dp(7))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#2D1E55"))
                setStroke(1, Color.parseColor("#6D5EF5"))
            }
        }
        openBtn.setOnClickListener { hideResultOverlay(); launchApp(lastTranscript) }
        footer.addView(openBtn)
        root.addView(footer)
        return frame
    }

    private fun updateResultText(text: String) {
        resultTextView?.text = text
        resultScrollView?.post { resultScrollView?.fullScroll(View.FOCUS_DOWN) }
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
            shape = GradientDrawable.OVAL
            colors = if (isActive) {
                intArrayOf(Color.parseColor("#FF5B5B"), Color.parseColor("#FF8A65"))
            } else {
                intArrayOf(Color.parseColor("#6D5EF5"), Color.parseColor("#4A90E2"))
            }
            setStroke(3, Color.argb(if (isActive) 160 else 110, 255, 255, 255))
        }
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
        private const val ACTION_START = "com.example.orbitai.tools.bubble.START"
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