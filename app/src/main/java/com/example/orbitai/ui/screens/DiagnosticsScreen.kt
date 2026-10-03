package com.example.orbitai.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.BuildConfig
import com.example.orbitai.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shows stored diagnostics reports so the user can review them before sharing. */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { context.appContainer.diagnostics }
    var reports by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        store.dismissPrompt()
        reports = withContext(Dispatchers.IO) { store.readReports() }
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Diagnostics", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                Text(
                    "Reports are saved only on this device when Orbit crashes, stops responding, or is closed by " +
                        "Android for low memory. They contain the app version, device model, Android version, the " +
                        "model that was loaded and a technical stack trace. They never include your chats, memories, " +
                        "documents, tokens or API keys. Nothing is sent unless you tap Share.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            val text = reports
            if (text == null) {
                item { Text("Loading…") }
            } else if (text.isBlank()) {
                item { Text("No reports. Orbit hasn't recorded any crashes.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) {
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.SUPPORT_EMAIL))
                                }
                                putExtra(Intent.EXTRA_SUBJECT, "OrbitAI diagnostics")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(send, "Share diagnostics"))
                        }) { Text("Share") }
                        OutlinedButton(onClick = {
                            store.clear()
                            reports = ""
                        }) { Text("Clear") }
                    }
                }
                item {
                    SelectionContainer {
                        Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
