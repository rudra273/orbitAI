package com.example.orbitai.data

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MediaPipeTaskEngine(
    private val context: Context,
    private val modelPath: String,
    private val settings: InferenceSettings,
) : LlmInferenceEngine {

    private var engine: LlmInference? = null
    private var session: LlmInferenceSession? = null

    init {
        val engineOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(4096)
            .setPreferredBackend(LlmInference.Backend.CPU)
            .build()
        engine = LlmInference.createFromOptions(context, engineOptions)

        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(settings.topK)
            .setTopP(settings.topP)
            .setTemperature(settings.temperature)
            .build()
        session = LlmInferenceSession.createFromOptions(engine!!, sessionOptions)
    }

    override fun generateResponseStream(prompt: String, maxDecodedTokens: Int): Flow<String> = callbackFlow {
        val s = session ?: throw IllegalStateException("No model loaded.")

        var tokenCount = 0
        s.addQueryChunk(prompt)
        s.generateResponseAsync { partial, done ->
            tokenCount++
            trySend(partial ?: "")
            if (done || tokenCount >= maxDecodedTokens) close()
        }

        awaitClose { recreateSession() }
    }

    private fun recreateSession() {
        try {
            session?.close()
        } catch (_: Exception) {
        }
        session = null

        val eng = engine ?: return
        try {
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(settings.topK)
                .setTopP(settings.topP)
                .setTemperature(settings.temperature)
                .build()
            session = LlmInferenceSession.createFromOptions(eng, sessionOptions)
        } catch (_: Exception) {
            session = null
        }
    }

    override fun close() {
        try {
            session?.close()
        } catch (_: Exception) {
        }
        try {
            engine?.close()
        } catch (_: Exception) {
        }
        session = null
        engine = null
    }
}
