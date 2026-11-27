package com.example.orbitai

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
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
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.orbitai.core.common.ThemeSettingsStore
import com.example.orbitai.feature.bubble.BubbleSettingsStore
import com.example.orbitai.feature.bubble.OrbitBubbleService
import com.example.orbitai.ui.navigation.OrbitNavGraph
import com.example.orbitai.ui.theme.OrbitAITheme
import com.example.orbitai.viewmodel.ModesViewModel
import com.example.orbitai.viewmodel.ChatViewModel
import com.example.orbitai.viewmodel.DownloadViewModel
import com.example.orbitai.viewmodel.MemoryViewModel
import com.example.orbitai.viewmodel.SpacesViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Where a launcher shortcut, the Quick Settings tile or a notification asked the app to open. */
sealed interface LaunchRequest {
    data object NewChat : LaunchRequest
    data object TemporaryChat : LaunchRequest
    data object Voice : LaunchRequest
    data object Reminders : LaunchRequest
    data object Settings : LaunchRequest
    data class OpenChat(val chatId: String) : LaunchRequest
}

data class OverlayPromptRequest(
    val chatId: String,
    val prompt: String,
    val id: Long = System.currentTimeMillis(),
    val autoSend: Boolean = true,
    /** Files shared to Orbit, attached to the new chat's input. */
    val attachments: List<String> = emptyList(),
)

class MainActivity : ComponentActivity() {

    private val chatViewModel:     ChatViewModel     by viewModels()
    private val downloadViewModel: DownloadViewModel by viewModels()
    private val spacesViewModel:   SpacesViewModel   by viewModels()
    private val modesViewModel:    ModesViewModel    by viewModels()
    private val memoryViewModel:   MemoryViewModel   by viewModels()
    private var pendingOverlayPrompt by mutableStateOf<OverlayPromptRequest?>(null)
    private var pendingLaunch by mutableStateOf<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOverlayIntent(intent)
        if (savedInstanceState == null) handleLaunchIntent(intent)

        val themeStore = ThemeSettingsStore(this)
        val darkWindowBg = android.graphics.Color.parseColor("#141416")
        val blackWindowBg = android.graphics.Color.BLACK
        val lightWindowBg = android.graphics.Color.parseColor("#F2F2F7")
        val systemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        fun windowBg(appearance: com.example.orbitai.core.common.Appearance, dark: Boolean) = when {
            !dark -> lightWindowBg
            appearance.mode == com.example.orbitai.core.common.ThemeMode.BLACK -> blackWindowBg
            else -> darkWindowBg
        }

        // Let the app draw behind status bar and navigation bar
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Sync the native window before Compose draws to avoid a light flash
        // when the app's stored theme differs from the system theme bucket.
        themeStore.appearance.let { window.setBackgroundDrawable(ColorDrawable(windowBg(it, it.isDark(systemDark)))) }

        setContent {
            var appearance by remember { mutableStateOf(themeStore.appearance) }
            val isDarkTheme = appearance.isDark(androidx.compose.foundation.isSystemInDarkTheme())

            OrbitAITheme(appearance = appearance) {

                // ── Window background — keep native layer in sync with Compose theme ─
                // Without this, toggling theme shows a white flash from the AppCompat
                // window background for one frame before Compose repaints.
                SideEffect {
                    window.decorView.setBackgroundColor(windowBg(appearance, isDarkTheme))
                }

                // ── System bar colours ─────────────────────────────────────
                // Make status bar and nav bar fully transparent so our deep
                // space background bleeds to the very edges of the screen.
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !isDarkTheme
                        isAppearanceLightNavigationBars = !isDarkTheme
                    }
                    window.isNavigationBarContrastEnforced = false
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
                    launchRequest     = pendingLaunch,
                    onLaunchConsumed  = { pendingLaunch = null },
                    appearance        = appearance,
                    onAppearanceChanged = { updated ->
                        appearance = updated
                        themeStore.appearance = updated
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOverlayIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        syncBubbleService()
        if (BubbleSettingsStore(this).isFloatingBubbleEnabled) {
            OrbitBubbleService.setAppForeground(this, true)
        }
    }

    override fun onStop() {
        super.onStop()
        val bubbleSettingsStore = BubbleSettingsStore(this)
        val overlayGranted = OrbitBubbleService.canDrawOverlays(this)
        val audioGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (bubbleSettingsStore.isFloatingBubbleEnabled && overlayGranted && audioGranted) {
            OrbitBubbleService.setAppForeground(this, false)
        }
    }

    /** Turns a shortcut / tile intent into a [LaunchRequest] for the nav graph. */
    private fun handleLaunchIntent(intent: Intent?) {
        // Voice runs in the bubble only: start it and step aside, or open its settings below.
        if (intent?.action == ACTION_VOICE && OrbitBubbleService.isUsable(this)) {
            androidx.core.content.pm.ShortcutManagerCompat.reportShortcutUsed(this, "voice")
            OrbitBubbleService.trigger(this)
            intent.action = Intent.ACTION_MAIN
            moveTaskToBack(true)
            return
        }
        val request = when (intent?.action) {
            ACTION_NEW_CHAT -> LaunchRequest.NewChat
            ACTION_TEMPORARY_CHAT -> LaunchRequest.TemporaryChat
            ACTION_VOICE -> LaunchRequest.Voice
            ACTION_REMINDERS -> LaunchRequest.Reminders
            ACTION_OPEN_CHAT -> intent.getStringExtra(EXTRA_CHAT_ID)?.let { LaunchRequest.OpenChat(it) }
            android.service.quicksettings.TileService.ACTION_QS_TILE_PREFERENCES -> LaunchRequest.Settings
            else -> null
        } ?: return
        pendingLaunch = request
        val shortcutId = when (request) {
            LaunchRequest.NewChat -> "new_chat"
            LaunchRequest.Voice -> "voice"
            LaunchRequest.TemporaryChat -> "temporary_chat"
            LaunchRequest.Reminders -> "reminders"
            is LaunchRequest.OpenChat -> "chat:${request.chatId}"
            LaunchRequest.Settings -> null
        }
        shortcutId?.let { androidx.core.content.pm.ShortcutManagerCompat.reportShortcutUsed(this, it) }
        intent?.action = Intent.ACTION_MAIN // Don't repeat on recreation.
    }

    private fun handleOverlayIntent(intent: Intent?) {
        val sharedText = intent?.getStringExtra(EXTRA_SHARED_TEXT)?.trim().orEmpty()
        val transcript = intent?.getStringExtra(EXTRA_OVERLAY_TRANSCRIPT)?.trim().orEmpty()
        val sharedFiles = intent?.getStringArrayListExtra(EXTRA_SHARED_FILES).orEmpty()
        val prompt = sharedText.ifBlank { transcript }
        if (prompt.isBlank() && sharedFiles.isEmpty()) return

        val autoSend = sharedText.isBlank() && sharedFiles.isEmpty()
        lifecycleScope.launch {
            pendingOverlayPrompt = OverlayPromptRequest(
                chatId = chatViewModel.createNewChat(),
                prompt = prompt,
                autoSend = autoSend,
                attachments = sharedFiles,
            )
        }
        intent?.removeExtra(EXTRA_OVERLAY_TRANSCRIPT)
        intent?.removeExtra(EXTRA_SHARED_TEXT)
        intent?.removeExtra(EXTRA_SHARED_FILES)
    }

    private fun syncBubbleService() {
        val bubbleSettingsStore = BubbleSettingsStore(this)
        val overlayGranted = OrbitBubbleService.canDrawOverlays(this)
        val audioGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (bubbleSettingsStore.isFloatingBubbleEnabled && overlayGranted && audioGranted) {
            OrbitBubbleService.start(this)
        } else if (!bubbleSettingsStore.isFloatingBubbleEnabled) {
            OrbitBubbleService.stop(this)
        } else if (!overlayGranted || !audioGranted) {
            bubbleSettingsStore.isFloatingBubbleEnabled = false
            OrbitBubbleService.stop(this)
        }
    }

    companion object {
        const val EXTRA_SHARED_TEXT = "shared_text_draft"
        const val EXTRA_OVERLAY_TRANSCRIPT = "overlay_transcript"
        const val EXTRA_SHARED_FILES = "shared_files"
        const val ACTION_NEW_CHAT = "com.example.orbitai.NEW_CHAT"
        const val ACTION_TEMPORARY_CHAT = "com.example.orbitai.TEMPORARY_CHAT"
        const val ACTION_VOICE = "com.example.orbitai.VOICE"
        const val ACTION_REMINDERS = "com.example.orbitai.REMINDERS"
        const val ACTION_OPEN_CHAT = "com.example.orbitai.OPEN_CHAT"
        const val EXTRA_CHAT_ID = "chat_id"
    }
}
