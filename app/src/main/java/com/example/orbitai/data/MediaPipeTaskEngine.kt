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

    init {
        val engineOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(4096)
            .setPreferredBackend(LlmInference.Backend.CPU)
            .build()
        engine = LlmInference.createFromOptions(context, engineOptions)
    }

    override fun generateResponseStream(prompt: String, maxDecodedTokens: Int): Flow<String> = callbackFlow {
        val eng = engine ?: throw IllegalStateException("No model loaded.")
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(settings.topK)
            .setTopP(settings.topP)
            .setTemperature(settings.temperature)
            .build()
        val session = LlmInferenceSession.createFromOptions(eng, sessionOptions)

        var tokenCount = 0
        session.addQueryChunk(prompt)
        session.generateResponseAsync { partial, done ->
            tokenCount++
            trySend(partial ?: "")
            if (done || tokenCount >= maxDecodedTokens) close()
        }

        awaitClose {
            try {
                session.close()
            } catch (_: Exception) {
            }
        }
    }

    override fun close() {
        try {
            engine?.close()
        } catch (_: Exception) {
        }
        engine = null
    }

}
