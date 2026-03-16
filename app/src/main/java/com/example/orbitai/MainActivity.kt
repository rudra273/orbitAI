package com.example.orbitai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.example.orbitai.ui.navigation.OrbitNavGraph
import com.example.orbitai.ui.theme.OrbitAITheme
import com.example.orbitai.viewmodel.AgentsViewModel
import com.example.orbitai.viewmodel.ChatViewModel
import com.example.orbitai.viewmodel.DownloadViewModel
import com.example.orbitai.viewmodel.MemoryViewModel
import com.example.orbitai.viewmodel.SpacesViewModel

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel         by viewModels()
    private val downloadViewModel: DownloadViewModel by viewModels()
    private val spacesViewModel: SpacesViewModel     by viewModels()
    private val agentsViewModel: AgentsViewModel     by viewModels()
    private val memoryViewModel: MemoryViewModel     by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrbitAITheme {
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