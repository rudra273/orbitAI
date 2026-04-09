package com.example.orbitai.tools.bubble

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.orbitai.tools.bubble.OrbitBubbleService.Companion.ACTION_INJECT_TEXT
import com.example.orbitai.tools.bubble.OrbitBubbleService.Companion.EXTRA_INJECTED_TEXT

class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        
        if (!selectedText.isNullOrBlank()) {
            val serviceIntent = Intent(this, OrbitBubbleService::class.java).apply {
                action = ACTION_INJECT_TEXT
                putExtra(EXTRA_INJECTED_TEXT, selectedText)
            }
            startService(serviceIntent)
        }
        
        finish()
    }
}
