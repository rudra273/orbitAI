package com.example.orbitai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.example.orbitai.ui.navigation.OrbitNavGraph
import com.example.orbitai.ui.theme.OrbitAITheme
import com.example.orbitai.viewmodel.ChatViewModel
import com.example.orbitai.viewmodel.DownloadViewModel
import com.example.orbitai.viewmodel.MemoryViewModel
import com.example.orbitai.viewmodel.RagViewModel

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel         by viewModels()
    private val downloadViewModel: DownloadViewModel by viewModels()
    private val ragViewModel: RagViewModel           by viewModels()
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
                    ragViewModel      = ragViewModel,
                    memoryViewModel   = memoryViewModel,
                )
            }
        }
    }
}