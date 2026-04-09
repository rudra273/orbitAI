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

    companion object {
        var instance: OrbitAccessibilityService? = null
            private set
    }
}
