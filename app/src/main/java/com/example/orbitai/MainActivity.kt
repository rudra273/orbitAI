package com.example.orbitai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.orbitai.ui.navigation.OrbitNavGraph
import com.example.orbitai.ui.theme.OrbitAITheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.viewmodel.AgentsViewModel
import com.example.orbitai.viewmodel.ChatViewModel
import com.example.orbitai.viewmodel.DownloadViewModel
import com.example.orbitai.viewmodel.MemoryViewModel
import com.example.orbitai.viewmodel.SpacesViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {

    private val chatViewModel:     ChatViewModel     by viewModels()
    private val downloadViewModel: DownloadViewModel by viewModels()
    private val spacesViewModel:   SpacesViewModel   by viewModels()
    private val agentsViewModel:   AgentsViewModel   by viewModels()
    private val memoryViewModel:   MemoryViewModel   by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Let the app draw behind status bar and navigation bar
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            OrbitAITheme {

                // ── System bar colours ─────────────────────────────────────
                // Make status bar and nav bar fully transparent so our deep
                // space background bleeds to the very edges of the screen.
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color         = Color.Transparent,
                        darkIcons     = false,   // white icons on dark bg
                        isNavigationBarContrastEnforced = false,
                    )
                }

                val navController = rememberNavController()

                OrbitNavGraph(
                    navController     = navController,
                    chatViewModel     = chatViewModel,
                    downloadViewModel = downloadViewModel,
                    spacesViewModel   = spacesViewModel,
                    agentsViewModel   = agentsViewModel,
                    memoryViewModel   = memoryViewModel,
                )
            }
        }
    }
}