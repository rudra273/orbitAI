package com.example.orbitai.feature.bubble

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class OrbitAccessibilityService : AccessibilityService() {

    private var lastInterceptedText: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("OrbitAccessibility", "Service connected, listeners registered.")
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
            // Try extracting text natively from the active window since selection changed
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val extracted = findSelectedText(rootNode)
                if (!extracted.isNullOrBlank()) {
                    lastInterceptedText = extracted
                    Log.d("OrbitAccessibility", "Intercepted native selection text")
                }
            }
        }
    }

    override fun onInterrupt() {
        // Ignored
    }

    fun consumeInterceptedText(): String? {
        val text = lastInterceptedText
        lastInterceptedText = null
        return text
    }

    private fun findSelectedText(node: AccessibilityNodeInfo): String? {
        val text = node.text
        if (text != null) {
            val start = node.textSelectionStart
            val end = node.textSelectionEnd
            // Many apps report -1 if nothing is selected
            if (start != end && start >= 0 && end <= text.length) {
                // Ensure proper order
                val s = if (start < end) start else end
                val e = if (start > end) start else end
                return text.subSequence(s, e).toString()
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findSelectedText(child)
                child.recycle()
                if (found != null) return found
            }
        }
        return null
    }

    fun injectTextIntoActiveField(text: String): Boolean {
        try {
            val availableWindows = windows
            for (window in availableWindows) {
                val root = window.root ?: continue
                if (searchAndInjectText(root, text)) return true
            }
        } catch (e: Exception) {
            Log.e("OrbitAccessibility", "Injection failed", e)
        }
        return false
    }

    private fun searchAndInjectText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.isEditable && node.isFocused) {
            val arguments = android.os.Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val success = searchAndInjectText(child, text)
                child.recycle()
                if (success) return true
            }
        }
        return false
    }

    fun captureScreenAsBitmap(callback: (android.graphics.Bitmap?) -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        callback(processScreenshotResult(screenshotResult))
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.e("OrbitAccessibility", "Screenshot failed with error code: $errorCode")
                        callback(null)
                    }
                }
            )
        } else {
            callback(null)
        }
    }

    fun captureActiveWindowAsBitmap(callback: (Bitmap?) -> Unit) {
        val targetWindowId = resolvePreferredCaptureWindowId()
        if (targetWindowId == null) {
            callback(null)
            return
        }
        captureWindowAsBitmap(targetWindowId, callback)
    }

    fun captureWindowAsBitmap(windowId: Int, callback: (Bitmap?) -> Unit) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            callback(null)
            return
        }

        if (windowId < 0) {
            Log.w("OrbitAccessibility", "Invalid window id for takeScreenshotOfWindow: $windowId")
            callback(null)
            return
        }

        takeScreenshotOfWindow(
            windowId,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshotResult: ScreenshotResult) {
                    Log.d("OrbitAccessibility", "Window screenshot captured for windowId=$windowId")
                    callback(processScreenshotResult(screenshotResult))
                }

                override fun onFailure(errorCode: Int) {
                    Log.e(
                        "OrbitAccessibility",
                        "Window screenshot failed for windowId=$windowId error=$errorCode",
                    )
                    callback(null)
                }
            },
        )
    }

    fun resolvePreferredCaptureWindowId(): Int? {
        val ownPackage = packageName
        val rootWindowId = rootInActiveWindow?.windowId
        val rootPackage = rootInActiveWindow?.packageName?.toString()
        if (rootWindowId != null && rootWindowId >= 0 && rootPackage != ownPackage) {
            return rootWindowId
        }

        return windows
            .asSequence()
            .filter { window -> window.id >= 0 }
            .filter { window ->
                val pkg = window.root?.packageName?.toString()
                pkg != null && pkg != ownPackage
            }
            .sortedWith(
                compareByDescending<android.view.accessibility.AccessibilityWindowInfo> { it.isActive }
                    .thenByDescending { it.isFocused },
            )
            .map { it.id }
            .firstOrNull()
    }

    private fun processScreenshotResult(screenshotResult: ScreenshotResult): Bitmap? {
        return try {
            val hardwareBuffer = screenshotResult.hardwareBuffer
            val colorSpace = screenshotResult.colorSpace
            val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)

            val scaledBitmap = if (bitmap != null) {
                val maxWidth = 1024
                val ratioBitmap = bitmap.width.toFloat() / bitmap.height.toFloat()
                val finalWidth = if (bitmap.width > maxWidth) maxWidth else bitmap.width
                val finalHeight = (finalWidth / ratioBitmap).toInt()
                val resized = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
                resized.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                null
            }

            hardwareBuffer.close()
            scaledBitmap
        } catch (e: Exception) {
            Log.e("OrbitAccessibility", "Failed to process screenshot", e)
            null
        }
    }

    companion object {
        var instance: OrbitAccessibilityService? = null
            private set
    }
}
