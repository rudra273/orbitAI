package com.example.orbitai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.orbitai.feature.chat.ChatSettingsStore

/** Settings > Chat: smart titles, generation details and the translate language. */
@Composable
fun ChatSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { ChatSettingsStore(context) }
    var smartTitles by remember { mutableStateOf(store.smartTitles) }
    var details by remember { mutableStateOf(store.showGenerationDetails) }
    var language by remember { mutableStateOf(store.translateLanguage) }
    var languageMenu by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Chat", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                SettingSwitch(
                    title = "Smart titles",
                    description = "After the first reply, the on-device model names the chat in a few words. Cloud models are never used for this.",
                    checked = smartTitles,
                ) { smartTitles = it; store.smartTitles = it }
            }
            item {
                SettingSwitch(
                    title = "Show generation details",
                    description = "Under each new reply: time to first token, speed, hardware (CPU/GPU) and model.",
                    checked = details,
                ) { details = it; store.showGenerationDetails = it }
            }
            item {
                Column {
                    Text("Translate to", style = MaterialTheme.typography.titleSmall)
                    Text("Used by Translate when you select text in another app and pick Orbit AI.", style = MaterialTheme.typography.bodySmall)
                    androidx.compose.foundation.layout.Box {
                        TextButton(onClick = { languageMenu = true }) { Text(language) }
                        androidx.compose.material3.DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                            ChatSettingsStore.TRANSLATE_LANGUAGES.forEach { option ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { language = option; store.translateLanguage = option; languageMenu = false },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
