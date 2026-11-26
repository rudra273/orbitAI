package com.example.orbitai.feature.bubble

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.content.ContextCompat

/** Owns the bubble's [SpeechRecognizer] and reports start/stop with the final transcript. */
class BubbleSpeechController(
    private val context: Context,
    private val listener: Listener,
) {

    interface Listener {
        fun onListeningStarted()
        fun onListeningStopped(transcript: String, submitTranscript: Boolean)
    }

    enum class StartResult { STARTED, UNAVAILABLE, NO_PERMISSION }

    private var speechRecognizer: SpeechRecognizer? = null
    private var partialTranscript: String = ""
    var isListening = false
        private set

    fun toggle(): StartResult? {
        return if (isListening) {
            stop(submitTranscript = true)
            null
        } else {
            start()
        }
    }

    fun start(): StartResult {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Speech recognition is unavailable on this device.", Toast.LENGTH_SHORT).show()
            return StartResult.UNAVAILABLE
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Enable microphone permission in Orbit settings first.", Toast.LENGTH_SHORT).show()
            return StartResult.NO_PERMISSION
        }

        val recognizer = speechRecognizer ?: (if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }).also { created ->
            created.setRecognitionListener(BubbleRecognitionListener())
            speechRecognizer = created
        }

        partialTranscript = ""
        isListening = true
        listener.onListeningStarted()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }

        recognizer.startListening(intent)
        return StartResult.STARTED
    }

    fun stop(submitTranscript: Boolean) {
        if (!isListening && partialTranscript.isBlank()) return

        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()

        val transcript = partialTranscript.trim()
        partialTranscript = ""
        isListening = false
        listener.onListeningStopped(transcript, submitTranscript)
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private inner class BubbleRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onResults(results: Bundle?) {
            partialTranscript = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            stop(submitTranscript = true)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialTranscript = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
        }

        override fun onError(error: Int) {
            val shouldSubmit = partialTranscript.isNotBlank() || error !in setOf(
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            )
            if (!shouldSubmit) {
                Toast.makeText(context, "No speech detected.", Toast.LENGTH_SHORT).show()
            }
            stop(submitTranscript = partialTranscript.isNotBlank())
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }
}
