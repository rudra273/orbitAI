package com.example.orbitai.feature.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Speaks assistant replies sentence by sentence. Sentences can be queued while a reply is
 * still streaming; [onDrained] runs when everything queued has been spoken.
 */
class ReplySpeaker(context: Context, private val settings: VoiceSettingsStore) {

    private val _speakingId = MutableStateFlow<String?>(null)
    /** Id of the message being read, or null when silent. */
    val speakingId: StateFlow<String?> = _speakingId.asStateFlow()

    /** Called on a background thread when the queue empties after speaking. */
    @Volatile var onDrained: (() -> Unit)? = null

    private var ready = false
    private var failed = false
    private val waiting = mutableListOf<Pair<String, String>>()
    private var queued = 0
    private var counter = 0L

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        synchronized(this) {
            ready = status == TextToSpeech.SUCCESS
            failed = !ready
            if (ready) {
                applySettings()
                waiting.forEach { (id, text) -> speakNow(id, text) }
            }
            waiting.clear()
        }
    }.apply {
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finished()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finished()
            override fun onStop(utteranceId: String?, interrupted: Boolean) = Unit
        })
    }

    val isAvailable: Boolean get() = !failed

    /** Reads a whole message, replacing anything being spoken. */
    fun speakMessage(id: String, markdown: String) {
        stop()
        enqueue(id, SpeechText.sentences(SpeechText.clean(markdown)))
    }

    /** Adds sentences of message [id] to the end of the queue. */
    @Synchronized
    fun enqueue(id: String, sentences: List<String>) {
        if (sentences.isEmpty()) return
        _speakingId.value = id
        sentences.forEach { sentence ->
            if (ready) speakNow(id, sentence) else waiting += id to sentence
        }
    }

    @Synchronized
    fun stop() {
        waiting.clear()
        queued = 0
        _speakingId.value = null
        if (ready) tts.stop()
    }

    @Synchronized
    fun applySettings() {
        if (!ready) return
        tts.setSpeechRate(settings.speechRate)
        val voice = settings.voiceName?.let { name -> tts.voices?.firstOrNull { it.name == name } }
        if (voice != null) tts.voice = voice else tts.language = Locale.getDefault()
    }

    /** Voices for the device language, offline ones first. */
    @Synchronized
    fun voices(): List<android.speech.tts.Voice> {
        if (!ready) return emptyList()
        val language = Locale.getDefault().language
        return tts.voices.orEmpty()
            .filter { it.locale.language == language && !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.name }))
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }

    private fun speakNow(id: String, text: String) {
        queued++
        if (tts.speak(text, TextToSpeech.QUEUE_ADD, null, "$id#${counter++}") == TextToSpeech.ERROR) queued--
    }

    private fun finished() {
        val drained = synchronized(this) {
            queued = (queued - 1).coerceAtLeast(0)
            if (queued == 0 && waiting.isEmpty() && _speakingId.value != null) {
                _speakingId.value = null
                true
            } else false
        }
        if (drained) onDrained?.invoke()
    }
}
