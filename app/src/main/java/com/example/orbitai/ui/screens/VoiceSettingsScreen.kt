package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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

/** Speech speed and voice for reading replies aloud. */
@Composable
fun VoiceSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { VoiceSettingsStore(context) }
    val speaker = remember { ReplySpeaker(context, store) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
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
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(stringResource(R.string.voice), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Tap the speaker icon under a reply to hear it. Talking to Orbit by voice works in the Orbit Bubble.",
                    style = MaterialTheme.typography.bodyMedium,
                )
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
                    Text(stringResource(R.string.voice), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = ::preview) { Text(stringResource(R.string.play_sample)) }
                }
                if (!speaker.isAvailable) {
                    Text(stringResource(R.string.text_to_speech_isn_t), style = MaterialTheme.typography.bodySmall)
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
