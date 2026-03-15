package com.example.orbitai.data

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

class LlmRepository(private val context: Context) {

    private var engine: LlmInference? = null
    private var session: LlmInferenceSession? = null
    private var currentModelId: String? = null
    private var currentSettings: InferenceSettings? = null

    fun loadModel(model: LlmModel, settings: InferenceSettings) {
        if (currentModelId == model.id && currentSettings == settings) return

        // Close previous
        session?.close()
        engine?.close()
        session = null
        engine = null

        // 1. Create engine — model path + context window
        val engineOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(File(context.getExternalFilesDir(null), "models/${model.fileName}").absolutePath)
            .setMaxTokens(4096)
            .setPreferredBackend(LlmInference.Backend.CPU)
            .build()
        engine = LlmInference.createFromOptions(context, engineOptions)

        // 2. Create session — sampling params from user settings
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(settings.topK)
            .setTopP(settings.topP)
            .setTemperature(settings.temperature)
            .build()
        session = LlmInferenceSession.createFromOptions(engine!!, sessionOptions)

        currentModelId = model.id
        currentSettings = settings
    }

    /**
     * Streams response tokens. Closes early after [maxDecodedTokens] callback
     * invocations (≈ 1 token each) to enforce an output-length limit.
     */
    fun generateResponseStream(prompt: String, maxDecodedTokens: Int): Flow<String> = callbackFlow {
        val s = session ?: throw IllegalStateException("No model loaded.")

        var tokenCount = 0
        s.addQueryChunk(prompt)
        s.generateResponseAsync { partial, done ->
            tokenCount++
            trySend(partial ?: "")
            if (done || tokenCount >= maxDecodedTokens) close()
        }

        // When the flow is cancelled (stop button) OR finishes normally, close the
        // busy session and spin up a fresh one. This is required because MediaPipe
        // throws "previous invocation still processing" if you call addQueryChunk on
        // a session whose generateResponseAsync hasn't fired done=true yet.
        awaitClose { recreateSession() }
    }

    /**
     * Closes the current session and creates a fresh one from the same engine +
     * settings. Called after every generation (normal or cancelled) so the next
     * sendMessage gets a clean session rather than the "still processing" one.
     */
    private fun recreateSession() {
        try { session?.close() } catch (_: Exception) {}
        session = null
        val eng = engine ?: return
        val settings = currentSettings ?: return
        try {
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(settings.topK)
                .setTopP(settings.topP)
                .setTemperature(settings.temperature)
                .build()
            session = LlmInferenceSession.createFromOptions(eng, sessionOptions)
        } catch (_: Exception) {
            // If recreation fails signal a full reload on the next sendMessage
            session = null
            currentModelId = null
        }
    }

    fun isModelLoaded(modelId: String, settings: InferenceSettings) =
        currentModelId == modelId && currentSettings == settings && session != null

    fun close() {
        session?.close()
        engine?.close()
        session = null
        engine = null
        currentModelId = null
    }
}