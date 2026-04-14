package com.example.orbitai.tools.bubble

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
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
                        try {
                            val hardwareBuffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val bitmap = android.graphics.Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                            
                            // Scale down for LLM performance
                            val scaledBitmap = if (bitmap != null) {
                                val maxWidth = 1024
                                val maxHeight = 1024
                                val ratioBitmap = bitmap.width.toFloat() / bitmap.height.toFloat()
                                val finalWidth = if (bitmap.width > maxWidth) maxWidth else bitmap.width
                                val finalHeight = (finalWidth / ratioBitmap).toInt()
                                val resized = android.graphics.Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
                                resized.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                            } else null
                            
                            hardwareBuffer.close()
                            callback(scaledBitmap)
                        } catch (e: Exception) {
                            Log.e("OrbitAccessibility", "Failed to process screenshot", e)
                            callback(null)
                        }
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

    companion object {
        var instance: OrbitAccessibilityService? = null
            private set
    }
}
