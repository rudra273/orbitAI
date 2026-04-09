package com.example.orbitai.tools.bubble

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OrbitAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We only act on demand, so passive events are ignored.
    }

    override fun onInterrupt() {
        // Ignored
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("OrbitAccessibility", "Service connected")
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
    }

    fun getSelectedTextOrCopy(onCompleted: (String?) -> Unit) {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            onCompleted(null)
            return
        }

        // Strategy 1: Extract text directly from the Accessibility Node Tree bounds
        val extracted = findSelectedText(rootNode)
        if (extracted != null) {
            Log.d("OrbitAccessibility", "Extracted text directly from node.")
            onCompleted(extracted)
            return
        }

        // Strategy 2: Search all active windows for the Android floating "Copy" button and click it natively! (Bypasses WebView issues).
        if (findAndClickCopyButton()) {
            Log.d("OrbitAccessibility", "Fired ACTION_CLICK on the popup Copy button.")
            CoroutineScope(Dispatchers.Main).launch {
                delay(200) // wait for clipboard
                onCompleted(readClipboard())
            }
            return
        }

        // Strategy 3: If we couldn't extract it directly, tell the node to fire a COPY intent.
        val copyInvoked = findAndCopyNode(rootNode)
        if (copyInvoked) {
            Log.d("OrbitAccessibility", "Fired ACTION_COPY on focused node.")
            CoroutineScope(Dispatchers.Main).launch {
                delay(200) // wait for Android to place text in clipboard
                onCompleted(readClipboard()) 
            }
        } else {
            Log.d("OrbitAccessibility", "Failed to extract text or invoke copy.")
            onCompleted(null)
        }
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

    private fun findAndCopyNode(node: AccessibilityNodeInfo): Boolean {
        if (node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY)) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_COPY)) {
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val success = findAndCopyNode(child)
                child.recycle()
                if (success) return true
            }
        }
        return false
    }

    private fun findAndClickCopyButton(): Boolean {
        try {
            val systemCopyString = getString(android.R.string.copy).lowercase()
            val availableWindows = windows
            for (window in availableWindows) {
                val root = window.root ?: continue
                if (searchAndClickCopy(root, systemCopyString)) return true
            }
        } catch (e: Exception) {}
        return false
    }

    private fun searchAndClickCopy(node: AccessibilityNodeInfo, systemCopyString: String): Boolean {
        val text = node.text?.toString()?.lowercase()
        val desc = node.contentDescription?.toString()?.lowercase()
        
        if (text == systemCopyString || desc == systemCopyString || text == "copy" || desc == "copy") {
            if (node.isClickable && node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val success = searchAndClickCopy(child, systemCopyString)
                child.recycle()
                if (success) return true
            }
        }
        return false
    }

    private fun readClipboard(): String? {
        try {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val item = clipboard.primaryClip?.getItemAt(0)
                return item?.text?.toString()
            }
        } catch (e: Exception) {
            Log.e("OrbitAccessibility", "Clipboard read failed", e)
        }
        return null
    }

    companion object {
        var instance: OrbitAccessibilityService? = null
            private set
    }
}
