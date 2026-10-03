package com.example.orbitai.feature.bubble

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import com.example.orbitai.MainActivity
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

        if (sharedText.isNullOrBlank()) {
            finish()
            return
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Use text in Orbit")
            .setItems(arrayOf("Create reminder", "Ask Orbit")) { _, which ->
                if (which == 0) {
                    openSharedTextInApp("/remind $sharedText")
                } else {
                    askOrbit(sharedText)
                }
                finish()
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun askOrbit(sharedText: String) {
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!OrbitBubbleService.canDrawOverlays(this) || !audioGranted) {
            openSharedTextInApp(sharedText)
        } else {
            try {
                val serviceIntent = Intent(this, OrbitBubbleService::class.java).apply {
                    action = ACTION_USE_TEXT
                    putExtra(EXTRA_SHARED_TEXT, sharedText)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (_: IllegalStateException) {
                openSharedTextInApp(sharedText)
            } catch (_: SecurityException) {
                openSharedTextInApp(sharedText)
            }
        }
    }
    private fun openSharedTextInApp(text: String) {
        Toast.makeText(this, "Shared text opened in Orbit. Add your instruction before sending.", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_SHARED_TEXT, text)
        })
    }
}
