package com.example.orbitai

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.orbitai.data.ThemeSettingsStore
import com.example.orbitai.data.ToolSettingsStore
import com.example.orbitai.tools.bubble.OrbitBubbleService
import com.example.orbitai.ui.navigation.OrbitNavGraph
import com.example.orbitai.ui.theme.OrbitAITheme
import com.example.orbitai.viewmodel.ModesViewModel
import com.example.orbitai.viewmodel.ChatViewModel
import com.example.orbitai.viewmodel.DownloadViewModel
import com.example.orbitai.viewmodel.MemoryViewModel
import com.example.orbitai.viewmodel.SpacesViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

data class OverlayPromptRequest(
    val chatId: String,
    val prompt: String,
    val id: Long = System.currentTimeMillis(),
)

class MainActivity : ComponentActivity() {

    private val chatViewModel:     ChatViewModel     by viewModels()
    private val downloadViewModel: DownloadViewModel by viewModels()
    private val spacesViewModel:   SpacesViewModel   by viewModels()
    private val modesViewModel:    ModesViewModel    by viewModels()
    private val memoryViewModel:   MemoryViewModel   by viewModels()
    private var pendingOverlayPrompt by mutableStateOf<OverlayPromptRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOverlayIntent(intent)

        // Let the app draw behind status bar and navigation bar
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val themeStore = remember { ThemeSettingsStore(this) }
            var isDarkTheme by remember { mutableStateOf(themeStore.isDarkTheme) }

            OrbitAITheme(isDarkTheme = isDarkTheme) {

                // ── System bar colours ─────────────────────────────────────
                // Make status bar and nav bar fully transparent so our deep
                // space background bleeds to the very edges of the screen.
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color         = Color.Transparent,
                        darkIcons     = !isDarkTheme,
                        isNavigationBarContrastEnforced = false,
                    )
                }

                val navController = rememberNavController()

                OrbitNavGraph(
                    navController     = navController,
                    chatViewModel     = chatViewModel,
                    downloadViewModel = downloadViewModel,
                    spacesViewModel   = spacesViewModel,
                    modesViewModel    = modesViewModel,
                    memoryViewModel   = memoryViewModel,
                    overlayPromptRequest = pendingOverlayPrompt,
                    onOverlayPromptConsumed = { pendingOverlayPrompt = null },
                    isDarkTheme       = isDarkTheme,
                    onThemeChanged    = { enabled ->
                        isDarkTheme = enabled
                        themeStore.isDarkTheme = enabled
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOverlayIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        syncBubbleService()
    }

    private fun handleOverlayIntent(intent: Intent?) {
        val transcript = intent?.getStringExtra(EXTRA_OVERLAY_TRANSCRIPT)?.trim().orEmpty()
        if (transcript.isBlank()) return

        pendingOverlayPrompt = OverlayPromptRequest(
            chatId = chatViewModel.createNewChat(),
            prompt = transcript,
        )
        intent?.removeExtra(EXTRA_OVERLAY_TRANSCRIPT)
    }

    private fun syncBubbleService() {
        val toolSettingsStore = ToolSettingsStore(this)
        val overlayGranted = OrbitBubbleService.canDrawOverlays(this)
        val audioGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (toolSettingsStore.isFloatingBubbleEnabled && overlayGranted && audioGranted) {
            OrbitBubbleService.start(this)
        } else if (!toolSettingsStore.isFloatingBubbleEnabled) {
            OrbitBubbleService.stop(this)
        } else if (!overlayGranted || !audioGranted) {
            toolSettingsStore.isFloatingBubbleEnabled = false
            OrbitBubbleService.stop(this)
        }
    }

    companion object {
        const val EXTRA_OVERLAY_TRANSCRIPT = "overlay_transcript"
    }
}