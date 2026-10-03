package com.example.orbitai.feature.bubble

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.orbitai.R

internal fun Context.dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density + 0.5f).toInt()

internal fun WindowManager.screenSize(): Pair<Int, Int> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = currentWindowMetrics.bounds
        Pair(bounds.width(), bounds.height())
    } else {
        @Suppress("DEPRECATION")
        val m = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        defaultDisplay.getMetrics(m)
        Pair(m.widthPixels, m.heightPixels)
    }
}

/** Owns the bubble's result card and radial menu windows. All calls must happen on the main thread. */
class BubbleOverlayUi(
    private val context: Context,
    private val windowManager: WindowManager,
    private val host: Host,
) {

    interface Host {
        val bubbleParams: WindowManager.LayoutParams?
        val bubbleSizePx: Int
        val edgeDockMarginPx: Int
        fun cancelInference()
        fun openInApp(transcript: String)
        fun onRadialAction(transcript: String)
        fun dismissBubble()
    }

    /** Last request shown in the card; used by "Open" and when the card is rebuilt. */
    var lastTranscript: String = ""

    private var resultCardView: View? = null
    private var resultCardParams: WindowManager.LayoutParams? = null
    private var resultStatusView: TextView? = null
    private var resultTextView: TextView? = null
    /** Unrendered markdown of the current answer; streaming appends to this, not to the rendered text. */
    private var rawResultText: String = ""
    private var resultScrollView: ScrollView? = null
    var isResultVisible = false
        private set
    private var isResultCompactMode = false

    private var radialMenuView: View? = null
    private var isRadialMenuVisible = false

    val resultText: String get() = resultTextView?.text?.toString().orEmpty()

    fun setResultCardVisible(isVisible: Boolean) {
        resultCardView?.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    }

    fun copyTextToClipboard(text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Orbit AI", text))
    }

    private fun dpToPx(dp: Float): Int = context.dpToPx(dp)

    private fun getScreenSize(): Pair<Int, Int> = windowManager.screenSize()

    fun showResultOverlay(
        transcript: String,
        cancelExistingJob: Boolean = true,
        compact: Boolean = false,
    ) {
        hideResultOverlay(cancelJob = cancelExistingJob)
        rawResultText = ""
        isResultCompactMode = compact
        val (screenW, screenH) = getScreenSize()
        val cardW = screenW - dpToPx(32f)
        val responseHeightPx = dpToPx(BubbleSettingsStore(context).bubbleResponseHeightDp.toFloat())
        val bubbleY = host.bubbleParams?.y ?: dpToPx(200f)
        val cardY = if (bubbleY < screenH / 2)
            (bubbleY + host.bubbleSizePx + dpToPx(10f))
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
        windowManager.addView(card, params)
        isResultVisible = true
    }

    fun hideResultOverlay(cancelJob: Boolean = true) {
        if (cancelJob) host.cancelInference()
        if (isResultVisible) {
            try { windowManager.removeView(resultCardView) } catch (_: Exception) {}
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
    fun hideRadialMenu() {
        if (isRadialMenuVisible) {
            try { windowManager.removeView(radialMenuView) } catch (_: Exception) {}
        }
        radialMenuView = null
        isRadialMenuVisible = false
    }

    fun showRadialMenu() {
        if (isRadialMenuVisible) return
        val bParams = host.bubbleParams ?: return
        val (screenW, screenH) = getScreenSize()
        val centerX = bParams.x + host.bubbleSizePx / 2
        val centerY = bParams.y + host.bubbleSizePx / 2

        val overlay = FrameLayout(context).apply {
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

            val btn = TextView(context).apply {
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
                        "Summarize" -> host.onRadialAction("Summarize this screen")
                        "Translate" -> host.onRadialAction("Translate this screen to English")
                        "Explain" -> host.onRadialAction("Explain this screen")
                        "Reply" -> host.onRadialAction("Write a reply I can paste based on this screen")
                        "Close" -> host.dismissBubble()
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
        windowManager.addView(overlay, layoutParams)
        isRadialMenuVisible = true
    }

    fun ensureExpandedResultOverlay() {
        if (!isResultVisible || !isResultCompactMode) return

        val currentStatus = resultStatusView?.text?.toString() ?: "Thinking..."
        val currentText = rawResultText
        val oldParams = resultCardParams
        hideResultOverlay(cancelJob = false)
        val responseHeightPx = dpToPx(BubbleSettingsStore(context).bubbleResponseHeightDp.toFloat())
        val card = buildResultCardView(lastTranscript, responseHeightPx, compact = false)
        resultCardView = card
        resultCardParams = oldParams
        if (oldParams != null) {
            windowManager.addView(card, oldParams)
        }
        isResultVisible = true
        isResultCompactMode = false
        resultStatusView?.text = currentStatus
        updateResultText(currentText)
    }

    @Suppress("SetTextI18n")
    private fun buildResultCardView(
        transcript: String,
        responseHeightPx: Int,
        compact: Boolean,
    ): View {
        fun dp(v: Int): Int = dpToPx(v.toFloat())

        val store = BubbleSettingsStore(context)
        val theme = getThemeConfig(store.bubbleResponseTheme)
        val alphaInt = (store.bubbleResponseAlphaPercent / 100f * 255).toInt()

        val frame = object : FrameLayout(context) {
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
        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        frame.addView(root, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ))

        // Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(10), dp(10), dp(8))
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(8) }
            setImageResource(R.drawable.vector_logo)
            imageTintList = android.content.res.ColorStateList.valueOf(theme.primaryText)
        })
        header.addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Orbit AI"
            setTextColor(theme.primaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
        })

        // Theme switchers
        val themeSwitcherContainer = LinearLayout(context).apply {
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
            themeSwitcherContainer.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply { marginEnd = dp(6) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(btnColor)
                    if (id == "white_glassy") setStroke(dp(1), Color.LTGRAY)
                }
                setOnClickListener {
                    store.bubbleResponseTheme = id
                    val currentStatus = resultStatusView?.text?.toString() ?: "Thinking..."
                    val currentText = rawResultText
                    val oldParams = resultCardParams
                    hideResultOverlay(cancelJob = false)
                    val card = buildResultCardView(
                        lastTranscript,
                        responseHeightPx,
                        compact = isResultCompactMode,
                    )
                    resultCardView = card
                    resultCardParams = oldParams
                    if (oldParams != null) windowManager.addView(card, oldParams)
                    isResultVisible = true
                    isResultCompactMode = compact
                    resultStatusView?.text = currentStatus
                    updateResultText(currentText)
                }
            })
        }
        if (!compact) {
            header.addView(themeSwitcherContainer)
        }

        val opacitySlider = android.widget.SeekBar(context).apply {
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

        val openBtn = TextView(context).apply {
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
        openBtn.setOnClickListener { hideResultOverlay(); host.openInApp(lastTranscript) }
        if (!compact) {
            header.addView(openBtn)
        }
        val closeBtn = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(30))
            text = "✕"
            setTextColor(theme.primaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
        }
        closeBtn.setOnClickListener { hideResultOverlay() }
        header.addView(closeBtn)
        root.addView(header)

        val statusText = TextView(context).apply {
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
        root.addView(TextView(context).apply {
            text = "Report response"
            contentDescription = "Report this AI response to the developer"
            setTextColor(theme.secondaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            minHeight = dp(48)
            setOnClickListener {
                val response = resultTextView?.text?.toString().orEmpty()
                if (response.isNotBlank()) {
                    context.startActivity(Intent(context,
                        com.example.orbitai.feature.reporting.ContentReportActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(com.example.orbitai.feature.reporting.ContentReportActivity.EXTRA_RESPONSE, response)
                    })
                    hideResultOverlay()
                }
            }
        })

        // Drag response card anywhere (bounded to visible area)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
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
                        params.x = (initialX + deltaX).coerceIn(host.edgeDockMarginPx, (screenW - cardW - host.edgeDockMarginPx).coerceAtLeast(host.edgeDockMarginPx))
                        params.y = (initialY + deltaY).coerceIn(host.edgeDockMarginPx, (screenH - cardH - host.edgeDockMarginPx).coerceAtLeast(host.edgeDockMarginPx))
                        resultCardView?.let { windowManager.updateViewLayout(it, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> moved
                else -> false
            }
        }

        // Transcript snippet
        root.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(Color.argb(70, 255, 255, 255))
            visibility = if (compact) View.GONE else View.VISIBLE
        })
        root.addView(TextView(context).apply {
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
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, responseHeightPx)
            isVerticalScrollBarEnabled = false
            visibility = if (compact) View.GONE else View.VISIBLE
        }
        val responseText = TextView(context).apply {
            setPadding(dp(14), dp(8), dp(14), dp(10))
            setTextColor(theme.primaryText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setLineSpacing(dp(2).toFloat(), 1f)
            setTextIsSelectable(true)
            text = ""
        }
        scrollView.addView(responseText)
        root.addView(scrollView)

        // Small copy icon under the answer
        root.addView(ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                gravity = Gravity.END
                setMargins(0, 0, dp(8), dp(6))
            }
            setPadding(dp(9), dp(9), dp(9), dp(9))
            setImageResource(R.drawable.ic_copy)
            imageTintList = android.content.res.ColorStateList.valueOf(theme.secondaryText)
            contentDescription = "Copy response"
            visibility = if (compact) View.GONE else View.VISIBLE
            setOnClickListener {
                val textToCopy = resultTextView?.text?.toString().orEmpty()
                if (textToCopy.isNotBlank()) {
                    copyTextToClipboard(textToCopy)
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                }
            }
        })
        resultStatusView = statusText
        resultTextView = responseText
        resultScrollView = scrollView
        return frame
    }

    fun updateResultStatus(text: String) {
        resultStatusView?.text = text
    }

    fun appendResultText(delta: String) {
        updateResultText(rawResultText + delta)
    }

    fun updateResultText(text: String) {
        rawResultText = text
        resultTextView?.text = parseSimpleMarkdown(text)
        resultScrollView?.post { resultScrollView?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun parseSimpleMarkdown(text: String): android.text.Spanned =
        android.text.Html.fromHtml(
            com.example.orbitai.feature.chat.markdown.MarkdownHtml.toHtml(text),
            android.text.Html.FROM_HTML_MODE_COMPACT,
        )

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
    }
}
