package com.example.orbitai.feature.bubble

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.example.orbitai.feature.bubble.OrbitBubbleService.Companion.ACTION_USE_TEXT
import com.example.orbitai.feature.bubble.OrbitBubbleService.Companion.EXTRA_SHARED_TEXT

class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            }
            Intent.ACTION_SEND -> {
                intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            }
            else -> null
        }

        if (!sharedText.isNullOrBlank()) {
            val serviceIntent = Intent(this, OrbitBubbleService::class.java).apply {
                action = ACTION_USE_TEXT
                putExtra(EXTRA_SHARED_TEXT, sharedText)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }

        finish()
    }
}
