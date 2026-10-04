package com.example.orbitai.feature.bubble

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
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.orbitai.MainActivity
import com.example.orbitai.R
import com.example.orbitai.feature.automation.AutomationRoute
import com.example.orbitai.feature.automation.AutomationRouter

/**
 * Floating bubble orchestrator: service lifecycle, the bubble view (drag, edge snapping,
 * animations) and the foreground notification. Result UI, speech and inference live in
 * [BubbleOverlayUi], [BubbleSpeechController] and [BubbleInferenceController].
 */
class OrbitBubbleService : Service() {

    private var slideTabWidthDp = 24f
    private var slideTabHeightDp = 104f

    // ── Window / bubble ───────────────────────────────────────────────────────
    private var windowManager: WindowManager? = null
    private var bubbleView: FrameLayout? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var isBubbleAttached = false
    private var isAppForeground = false

    private var pulseAnimator: android.animation.ValueAnimator? = null
    private var pendingSelectedText: String? = null

    // ── Config (read from settings on each onStartCommand) ───────────────────
    private var bubbleSizePx = 0
    private var edgeDockMarginPx = 0
    private var bubbleIdleAlpha = 0.42f
    private var bubbleStyle = "round"

    // ── Components ────────────────────────────────────────────────────────────
    private lateinit var overlayUi: BubbleOverlayUi
    private lateinit var speech: BubbleSpeechController
    private lateinit var inference: BubbleInferenceController

    private val overlayHost = object : BubbleOverlayUi.Host {
        override val bubbleParams: WindowManager.LayoutParams? get() = this@OrbitBubbleService.bubbleParams
        override val bubbleSizePx: Int get() = this@OrbitBubbleService.bubbleSizePx
        override val edgeDockMarginPx: Int get() = this@OrbitBubbleService.edgeDockMarginPx
        override fun cancelInference() = inference.cancel()
        override fun openInApp(transcript: String) = launchApp(transcript)
        override fun onRadialAction(transcript: String) = handleTranscript(transcript)
        override fun dismissBubble() = this@OrbitBubbleService.dismissBubble()
    }

    private val speechListener = object : BubbleSpeechController.Listener {
        override fun onListeningStarted() {
            enterActiveVisualMode()
            updateBubbleUi(isActive = true)
            startBubbleForeground(isListening = true)
        }

        override fun onListeningStopped(transcript: String, submitTranscript: Boolean) {
            updateBubbleUi(isActive = false)
            snapToNearestEdge(animated = true)
            enterIdleVisualMode()
            startBubbleForeground(isListening = false)

            val hasPendingText = !pendingSelectedText.isNullOrBlank()

            if (submitTranscript && transcript.isNotBlank()) {
                handleTranscript(transcript)
            } else if (hasPendingText) {
                pendingSelectedText = null
                overlayUi.hideResultOverlay()
            }
        }
    }

    private val inferenceUi = object : BubbleInferenceController.Ui {
        override val isResultVisible: Boolean get() = overlayUi.isResultVisible
        override val resultText: String get() = overlayUi.resultText
        override fun showResultOverlay(transcript: String) = overlayUi.showResultOverlay(transcript, cancelExistingJob = false)
        override fun ensureExpandedResultOverlay() = overlayUi.ensureExpandedResultOverlay()
        override fun updateResultStatus(text: String) = overlayUi.updateResultStatus(text)
        override fun updateResultText(text: String) = overlayUi.updateResultText(text)
        override fun appendResultText(delta: String) = overlayUi.appendResultText(delta)
        override fun copyTextToClipboard(text: String) = overlayUi.copyTextToClipboard(text)
        override fun setOrbitOverlayVisibility(isVisible: Boolean) {
            overlayUi.setResultCardVisible(isVisible)
            bubbleView?.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        }
        override fun setScreenCaptureForeground(isScreenCapture: Boolean) =
            startBubbleForeground(isListening = speech.isListening, isScreenCapture = isScreenCapture)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        overlayUi = BubbleOverlayUi(this, wm, overlayHost)
        speech = BubbleSpeechController(this, speechListener)
        inference = BubbleInferenceController(this, inferenceUi)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { dismissBubble(); return START_NOT_STICKY }
            ACTION_APP_FOREGROUND -> {
                isAppForeground = true
                overlayUi.hideResultOverlay()
                removeBubble()
                return START_STICKY
            }
            ACTION_APP_BACKGROUND -> {
                isAppForeground = false
            }
            ACTION_START, ACTION_TRIGGER, ACTION_USE_TEXT, null -> Unit
            else -> return START_NOT_STICKY
        }

        if (!canDrawOverlays(this) ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) { stopSelf(); return START_NOT_STICKY }

        val newSizePx = dpToPx(BubbleSettingsStore(this).bubbleSizeDp.toFloat())
        val settings = BubbleSettingsStore(this)
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
        } else if (!speech.isListening && !isAppForeground) {
            // Apply transparency slider changes immediately while bubble is idle.
            enterIdleVisualMode()
        }

        if (intent?.action == ACTION_TRIGGER) {
            if (isAppForeground) {
                startListening()
            } else {
                bubbleView?.post { toggleListening() }
            }
        } else if (intent?.action == ACTION_USE_TEXT) {
            val text = intent.getStringExtra(EXTRA_SHARED_TEXT)
            if (!text.isNullOrBlank()) {
                pendingSelectedText = text
                overlayUi.showResultOverlay("Shared text: $text")
                overlayUi.updateResultText("Speak your instruction...")
                bubbleView?.post {
                    if (!speech.isListening) startListening()
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopPulseAnimation()
        speech.stop(submitTranscript = false)
        inference.destroy()
        speech.destroy()
        overlayUi.hideResultOverlay()
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
            overlayUi.showRadialMenu()
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
                        if (!speech.isListening) enterIdleVisualMode()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    bubble.removeCallbacks(longPressRunnable)
                    if (!speech.isListening) {
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
        if (speech.toggle() == BubbleSpeechController.StartResult.NO_PERMISSION) launchApp()
    }

    private fun startListening() {
        if (speech.start() == BubbleSpeechController.StartResult.NO_PERMISSION) launchApp()
    }

    // ── Transcript routing ────────────────────────────────────────────────────

    private fun handleTranscript(transcript: String) {
        var finalTranscript = transcript
        val selectedText = pendingSelectedText

        if (!selectedText.isNullOrBlank()) {
            finalTranscript = "Instruction: $transcript\n\nSelected text: ${selectedText}"
            pendingSelectedText = null
        }

        overlayUi.lastTranscript = finalTranscript
        val route = AutomationRouter.route(com.example.orbitai.feature.automation.parser.DeviceCommandParser.normalize(finalTranscript))

        // Tool requests need app chat flow where execution and permissions are handled.
        if (route is AutomationRoute.ToolOnly || com.example.orbitai.feature.automation.parser.DeviceCommandParser.parse(finalTranscript) != null) {
            launchApp(finalTranscript)
            return
        }

        if (BubbleSettingsStore(this).bubbleResultsInOverlay) {
            overlayUi.hideResultOverlay()
            inference.run(finalTranscript)
        } else {
            launchApp(finalTranscript)
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


    private fun dpToPx(dp: Float): Int = (this as Context).dpToPx(dp)

    private fun getScreenSize(): Pair<Int, Int> = windowManager!!.screenSize()

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
        overlayUi.hideResultOverlay()
        BubbleSettingsStore(this).isFloatingBubbleEnabled = false
        stopSelf()
    }

    private fun startBubbleForeground(isListening: Boolean, isScreenCapture: Boolean = false) {
        val notification = buildNotification(isListening)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                (if (isScreenCapture) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0)
            startForeground(NOTIFICATION_ID, notification, serviceType)
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
            .setContentText(if (isListening) "Speak now. Orbit will answer with on-device AI." else "Tap to speak. Long press for screen actions that ask for screen-capture approval.")
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

    companion object {
        private const val ACTION_START = "com.example.orbitai.feature.bubble.START"
        private const val ACTION_TRIGGER = "com.example.orbitai.feature.bubble.TRIGGER"
        const val ACTION_USE_TEXT = "com.example.orbitai.feature.bubble.USE_TEXT"
        const val EXTRA_SHARED_TEXT = "com.example.orbitai.feature.bubble.EXTRA_TEXT"
        private const val ACTION_APP_FOREGROUND = "com.example.orbitai.feature.bubble.APP_FOREGROUND"
        private const val ACTION_APP_BACKGROUND = "com.example.orbitai.feature.bubble.APP_BACKGROUND"
        private const val ACTION_STOP = "com.example.orbitai.feature.bubble.STOP"
        private const val NOTIFICATION_CHANNEL_ID = "orbit_bubble"
        private const val NOTIFICATION_ID = 4201
        const val SIZE_SMALL_DP  = 52
        const val SIZE_MEDIUM_DP = 64
        const val SIZE_LARGE_DP  = 80

        fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

        /** The bubble is switched on and has the overlay and microphone permissions it needs. */
        fun isUsable(context: Context): Boolean =
            BubbleSettingsStore(context).isFloatingBubbleEnabled && canDrawOverlays(context) &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED

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

        fun deliverMediaProjectionResult(resultCode: Int, data: Intent?) {
            BubbleInferenceController.deliverMediaProjectionResult(resultCode, data)
        }

        fun overlayPermissionIntent(context: Context): Intent {
            return Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
        }
    }
}
