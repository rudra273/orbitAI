package com.example.orbitai.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.orbitai.viewmodel.VoiceCallPrompt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class VoiceState { Idle, Listening, Responding }

@Composable
internal fun rememberVoiceInput(
    onTextChange: (String) -> Unit,
    prompt: VoiceCallPrompt?,
    onCall: (String) -> Boolean,
    onCancelCall: () -> Unit,
    onPromptSpoken: (Long) -> Unit,
    onVoiceUnavailable: () -> Unit,
): Pair<VoiceState, () -> Unit> {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val textCallback by rememberUpdatedState(onTextChange)
    val callCallback by rememberUpdatedState(onCall)
    val cancelCallback by rememberUpdatedState(onCancelCall)
    val spokenCallback by rememberUpdatedState(onPromptSpoken)
    val unavailableCallback by rememberUpdatedState(onVoiceUnavailable)
    val currentPrompt by rememberUpdatedState(prompt)
    var state by remember { mutableStateOf(VoiceState.Idle) }
    var disclosure by remember { mutableStateOf(false) }
    var consent by remember { mutableStateOf(false) }
    val active = remember { booleanArrayOf(false) }
    val waiting = remember { booleanArrayOf(false) }
    val confirmedText = remember { StringBuilder() }
    val recognizer = remember(context) {
        when {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context) -> SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            SpeechRecognizer.isRecognitionAvailable(context) -> SpeechRecognizer.createSpeechRecognizer(context)
            else -> null
        }
    }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsStatus by remember { mutableStateOf<Int?>(null) }
    var speakingId by remember { mutableStateOf<Long?>(null) }

    fun stop(cancelCall: Boolean = true) {
        active[0] = false
        waiting[0] = false
        speakingId = null
        recognizer?.cancel()
        tts?.stop()
        state = VoiceState.Idle
        confirmedText.clear()
        if (cancelCall) cancelCallback()
    }

    fun voiceFailed(message: String) {
        stop(cancelCall = false)
        unavailableCallback()
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun startListening() {
        if (!active[0] || waiting[0]) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            state = VoiceState.Listening
            recognizer?.startListening(intent)
        } catch (_: SecurityException) {
            voiceFailed("Microphone permission is required for voice input.")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            confirmedText.clear()
            active[0] = true
            startListening()
        }
    }

    DisposableEffect(recognizer, owner) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) {
                if (!active[0] || waiting[0]) return
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                textCallback(confirmedText.toString() + partial)
            }
            override fun onResults(results: Bundle?) {
                if (!active[0] || waiting[0]) return
                val result = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }
                if (result != null) {
                    val text = (confirmedText.toString() + result).trim()
                    // Only finalized speech can initiate or confirm a call.
                    if (callCallback(text)) {
                        waiting[0] = true
                        state = VoiceState.Responding
                        confirmedText.clear()
                        textCallback("")
                        return
                    }
                    confirmedText.append(result).append(' ')
                    textCallback(confirmedText.toString().trimEnd())
                }
                startListening()
            }
            override fun onError(error: Int) {
                if (!active[0] || waiting[0]) return
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    scope.launch { delay(300); startListening() }
                } else {
                    voiceFailed("Voice input stopped. Check speech services and microphone permission, then try again.")
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            stop()
            owner.lifecycle.removeObserver(observer)
            recognizer?.destroy()
            tts?.shutdown()
        }
    }

    LaunchedEffect(prompt?.id, ttsStatus) {
        val question = prompt ?: return@LaunchedEffect
        if (!active[0] || speakingId == question.id) return@LaunchedEffect
        waiting[0] = true
        state = VoiceState.Responding
        if (tts == null) {
            tts = TextToSpeech(context) { status -> scope.launch { ttsStatus = status } }
            return@LaunchedEffect
        }
        val status = ttsStatus ?: return@LaunchedEffect
        if (status != TextToSpeech.SUCCESS) {
            voiceFailed("Speech playback is unavailable. You can review the contact on screen.")
            return@LaunchedEffect
        }
        val speaker = tts ?: return@LaunchedEffect
        speakingId = question.id
        speaker.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                scope.launch {
                    if (!active[0] || currentPrompt?.id != question.id || utteranceId != question.id.toString()) return@launch
                    spokenCallback(question.id)
                    waiting[0] = false
                    if (question.listenAfter) {
                        // Leave a gap before reopening the mic so speaker audio is not recognized as an answer.
                        delay(250)
                        if (currentPrompt?.id == question.id) startListening()
                    } else stop()
                }
            }
            override fun onError(utteranceId: String?) {
                scope.launch {
                    if (active[0] && currentPrompt?.id == question.id && utteranceId == question.id.toString()) {
                        voiceFailed("Couldn't speak the confirmation. You can review the contact on screen.")
                    }
                }
            }
        })
        if (speaker.speak(question.text, TextToSpeech.QUEUE_FLUSH, null, question.id.toString()) == TextToSpeech.ERROR) {
            voiceFailed("Couldn't speak the confirmation. You can review the contact on screen.")
        }
    }

    fun begin() {
        if (recognizer == null) {
            Toast.makeText(context, "Speech recognition is unavailable on this device.", Toast.LENGTH_SHORT).show()
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            confirmedText.clear()
            waiting[0] = false
            active[0] = true
            startListening()
        } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    if (disclosure) {
        AlertDialog(
            onDismissRequest = { disclosure = false },
            title = { Text("Use voice input?") },
            text = { Text("Orbit uses your microphone to transcribe speech. It prefers on-device recognition; your device's speech provider may process audio online otherwise. Orbit does not save audio recordings. Call requests are handled when you finish speaking: Orbit says the matching contact and listens for your confirmation before calling. Other speech stays in the message box for review.") },
            confirmButton = { TextButton(onClick = { consent = true; disclosure = false; begin() }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { disclosure = false }) { Text("Cancel") } },
        )
    }

    return state to {
        if (active[0]) stop()
        else if (!consent) disclosure = true
        else begin()
    }
}
