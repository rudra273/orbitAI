package com.example.orbitai.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.orbitai.feature.chat.Message
import com.example.orbitai.feature.chat.Role
import com.example.orbitai.feature.voice.ReplySpeaker
import com.example.orbitai.feature.voice.SpeechText
import com.example.orbitai.feature.voice.VoiceSettingsStore
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.TextMuted
import com.example.orbitai.ui.theme.TextPrimary
import com.example.orbitai.ui.theme.VioletBright
import com.example.orbitai.ui.theme.VioletGlow
import com.example.orbitai.viewmodel.VoiceCallPrompt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class VoicePhase { Listening, Thinking, Speaking, Paused }

/**
 * Hands-free conversation: listen → send → speak the reply sentence by sentence while it
 * streams → listen again. Tap the orb to interrupt. The screen stays on while it's open, and it
 * closes when the app goes to the background.
 */
@Composable
internal fun VoiceModeOverlay(
    messages: List<Message>,
    isGenerating: Boolean,
    speaker: ReplySpeaker,
    voiceSettings: VoiceSettingsStore,
    voiceCallPrompt: VoiceCallPrompt?,
    onSend: (String) -> Unit,
    onVoiceCall: (String) -> Boolean,
    onVoiceCallPromptSpoken: (Long) -> Unit,
    onStopGeneration: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val close by rememberUpdatedState(onClose)
    val send by rememberUpdatedState(onSend)
    val voiceCall by rememberUpdatedState(onVoiceCall)
    val generating by rememberUpdatedState(isGenerating)
    val callPrompt by rememberUpdatedState(voiceCallPrompt)
    val currentMessages by rememberUpdatedState(messages)

    var phase by remember { mutableStateOf(VoicePhase.Paused) }
    var caption by remember { mutableStateOf("") }
    var level by remember { mutableFloatStateOf(0f) }
    var showIntro by remember { mutableStateOf(!voiceSettings.voiceModeIntroShown) }
    // The reply being spoken: the first assistant message after the one that existed when we sent.
    var baselineReplyId by remember { mutableStateOf<String?>(null) }
    var awaitingReply by remember { mutableStateOf(false) }
    val stream = remember { arrayOf(SpeechText.Stream()) }
    var spokenPromptId by remember { mutableStateOf<Long?>(null) }

    val recognizer = remember(context) {
        when {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context) -> SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            SpeechRecognizer.isRecognitionAvailable(context) -> SpeechRecognizer.createSpeechRecognizer(context)
            else -> null
        }
    }

    fun listen() {
        if (recognizer == null) {
            caption = "Speech recognition is unavailable on this device."
            phase = VoicePhase.Paused
            return
        }
        speaker.stop()
        caption = ""
        phase = VoicePhase.Listening
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        try {
            recognizer.startListening(intent)
        } catch (_: SecurityException) {
            caption = "Microphone permission is required."
            phase = VoicePhase.Paused
        }
    }

    /** Stops speaking or generating and listens again. */
    fun interrupt() {
        recognizer?.cancel()
        speaker.stop()
        if (generating) onStopGeneration()
        awaitingReply = false
        listen()
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listen() else close()
    }

    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(recognizer, owner) {
        view.keepScreenOn = true
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { level = 0f }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { caption = it }
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                if (phase != VoicePhase.Listening) return
                if (text.isBlank()) {
                    scope.launch { delay(300); listen() }
                    return
                }
                caption = text
                phase = VoicePhase.Thinking
                // Call requests keep using the call confirmation flow.
                if (voiceCall(text)) return
                baselineReplyId = currentMessages.lastOrNull { it.role == Role.ASSISTANT }?.id ?: ""
                awaitingReply = true
                stream[0] = SpeechText.Stream()
                send(text)
            }
            override fun onError(error: Int) {
                if (phase != VoicePhase.Listening) return
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    scope.launch { delay(300); if (phase == VoicePhase.Listening) listen() }
                } else {
                    caption = "Voice input stopped. Tap to try again."
                    phase = VoicePhase.Paused
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        speaker.onDrained = {
            scope.launch {
                val prompt = callPrompt
                if (prompt != null && spokenPromptId == prompt.id) {
                    onVoiceCallPromptSpoken(prompt.id)
                    delay(250) // Keep speaker audio out of the next recognition.
                    if (prompt.listenAfter) listen() else phase = VoicePhase.Paused
                } else if (phase == VoicePhase.Speaking && !generating && !awaitingReply) {
                    delay(250)
                    listen()
                }
            }
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) close() }
        owner.lifecycle.addObserver(observer)
        if (!showIntro) start()
        onDispose {
            view.keepScreenOn = false
            owner.lifecycle.removeObserver(observer)
            speaker.onDrained = null
            speaker.stop()
            recognizer?.destroy()
        }
    }

    // Speak the first reply after the one that existed when we sent, as it streams.
    val reply = messages.lastOrNull { it.role == Role.ASSISTANT }?.takeIf { awaitingReply && baselineReplyId != null && it.id != baselineReplyId }
    LaunchedEffect(reply?.content, reply?.isStreaming, isGenerating) {
        val current = reply ?: run {
            // The send failed or produced no reply (e.g. a device action): listen again.
            if (awaitingReply && baselineReplyId != null && !isGenerating && phase == VoicePhase.Thinking) {
                delay(600)
                if (!generating && awaitingReply && phase == VoicePhase.Thinking) {
                    awaitingReply = false
                    listen()
                }
            }
            return@LaunchedEffect
        }
        val done = !current.isStreaming && !isGenerating
        val sentences = stream[0].next(current.content, final = done)
        if (sentences.isNotEmpty()) {
            phase = VoicePhase.Speaking
            speaker.enqueue(current.id, sentences)
        }
        if (done) {
            awaitingReply = false
            // Nothing left to say (empty reply, or everything already spoken and finished).
            if (speaker.speakingId.value == null) listen()
        }
    }

    // Speak call confirmations ("Call Mom, mobile?") and listen for the answer.
    LaunchedEffect(voiceCallPrompt?.id) {
        val prompt = voiceCallPrompt ?: return@LaunchedEffect
        if (spokenPromptId == prompt.id) return@LaunchedEffect
        spokenPromptId = prompt.id
        recognizer?.cancel()
        caption = prompt.text
        phase = VoicePhase.Speaking
        speaker.stop()
        speaker.enqueue("call-${prompt.id}", listOf(prompt.text))
    }

    BackHandler(onBack = close)

    if (showIntro) {
        AlertDialog(
            onDismissRequest = { showIntro = false; close() },
            title = { Text("Voice mode") },
            text = {
                Text(
                    "Talk to Orbit hands-free. Orbit listens, answers out loud, then listens again. Tap the circle to interrupt. " +
                        "Speech is recognised on this device when possible; otherwise your phone's speech service may process it online. " +
                        "Orbit doesn't save recordings. The screen stays on while voice mode is open.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showIntro = false
                    voiceSettings.voiceModeIntroShown = true
                    start()
                }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { showIntro = false; close() }) { Text("Cancel") } },
        )
    }

    val orbScale by animateFloatAsState(
        targetValue = when (phase) {
            VoicePhase.Listening -> 1f + level * 0.35f
            VoicePhase.Speaking -> 1.15f
            VoicePhase.Thinking -> 0.9f
            VoicePhase.Paused -> 0.8f
        },
        label = "orb",
    )
    val status = when (phase) {
        VoicePhase.Listening -> "Listening…"
        VoicePhase.Thinking -> "Thinking…"
        VoicePhase.Speaking -> "Speaking · tap to interrupt"
        VoicePhase.Paused -> "Paused · tap to talk"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep)
            .statusBarsPadding(),
    ) {
        IconButton(onClick = close, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "End voice mode", tint = TextPrimary)
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(orbScale)
                    .clip(CircleShape)
                    .background(if (phase == VoicePhase.Listening) VioletBright else VioletGlow)
                    .clickable { if (phase == VoicePhase.Paused) start() else interrupt() }
                    .semantics { contentDescription = if (phase == VoicePhase.Paused) "Start talking" else "Interrupt and talk" },
            )
            Spacer(Modifier.height(36.dp))
            Text(status, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
