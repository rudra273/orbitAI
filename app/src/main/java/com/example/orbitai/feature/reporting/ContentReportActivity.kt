package com.example.orbitai.feature.reporting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.orbitai.core.common.ThemeSettingsStore
import com.example.orbitai.ui.screens.ContentReportDialog
import com.example.orbitai.ui.theme.OrbitAITheme

class ContentReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val response = intent.getStringExtra(EXTRA_RESPONSE).orEmpty()
        if (response.isBlank()) {
            finish()
            return
        }
        setContent {
            OrbitAITheme(isDarkTheme = ThemeSettingsStore(this).isDarkTheme) {
                ContentReportDialog(response, "bubble", onDismiss = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_RESPONSE = "reported_response"
    }
}
