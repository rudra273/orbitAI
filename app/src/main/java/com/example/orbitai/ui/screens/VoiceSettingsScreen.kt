package com.example.orbitai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.orbitai.feature.voice.ReplySpeaker
import com.example.orbitai.feature.voice.VoiceSettingsStore
import kotlinx.coroutines.delay

/** Auto-read, speech speed and voice for read aloud and voice mode. */
@Composable
fun VoiceSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { VoiceSettingsStore(context) }
    val speaker = remember { ReplySpeaker(context, store) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    var autoRead by remember { mutableStateOf(store.autoRead) }
    var rate by remember { mutableFloatStateOf(store.speechRate) }
    var voiceName by remember { mutableStateOf(store.voiceName) }
    var voices by remember { mutableStateOf(emptyList<android.speech.tts.Voice>()) }
    BackHandler(onBack = onBack)

    // The speech engine starts asynchronously; poll briefly for its voice list.
    LaunchedEffect(speaker) {
        repeat(20) {
            voices = speaker.voices()
            if (voices.isNotEmpty()) return@LaunchedEffect
            delay(250)
        }
    }

    fun preview() {
        speaker.applySettings()
        speaker.speakMessage("preview", "This is how Orbit will sound when it reads replies.")
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Voice", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Tap the speaker icon under a reply to hear it, or the sound-wave button in chat for hands-free voice mode.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = autoRead, role = Role.Switch) { autoRead = it; store.autoRead = it },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-read replies", style = MaterialTheme.typography.titleSmall)
                        Text("Speak each new reply while it's written.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = autoRead, onCheckedChange = null)
                }
            }
            item {
                Text("Speech speed: ${"%.1f".format(rate)}×", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = rate,
                    onValueChange = { rate = (it * 10).toInt() / 10f },
                    onValueChangeFinished = { store.speechRate = rate; preview() },
                    valueRange = 0.5f..2f,
                    steps = 14,
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Voice", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = ::preview) { Text("Play sample") }
                }
                if (!speaker.isAvailable) {
                    Text("Text-to-speech isn't available on this phone. Install a speech engine in Android settings.", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                VoiceOption("System default", selected = voiceName == null) {
                    voiceName = null; store.voiceName = null; preview()
                }
            }
            items(voices, key = { it.name }) { voice ->
                VoiceOption(
                    label = voice.name + if (voice.isNetworkConnectionRequired) " (online)" else "",
                    selected = voice.name == voiceName,
                ) { voiceName = voice.name; store.voiceName = voice.name; preview() }
            }
        }
    }
}

@Composable
private fun VoiceOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
