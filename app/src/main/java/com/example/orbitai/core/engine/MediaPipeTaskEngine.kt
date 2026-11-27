package com.example.orbitai.core.engine

import android.content.Context
import com.example.orbitai.core.common.InferenceSettings
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.mediapipe.framework.image.BitmapImageBuilder

class MediaPipeTaskEngine(
    private val context: Context,
    private val modelPath: String,
    private val settings: InferenceSettings,
    private val plan: BackendPlan,
) : LlmInferenceEngine {

    private var engine: LlmInference? = null

    /** The backend this engine runs on ("GPU" or "CPU"). */
    var activeBackend: String = "CPU"
        private set

    init {
        // GPU then CPU in Auto; a picked backend never falls back (#41).
        val loaded = BackendCascade(context, java.io.File(modelPath).name).load(plan) { backend ->
            val engineOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(4096)
                .setMaxNumImages(3) // Enable vision modality for multimodal support
                .setPreferredBackend(if (backend == HardwareBackend.GPU) LlmInference.Backend.GPU else LlmInference.Backend.CPU)
                .build()
            LlmInference.createFromOptions(context, engineOptions)
        }
        engine = loaded.value
        activeBackend = loaded.label
    }

    override fun generateResponseStream(input: InferenceInput, maxDecodedTokens: Int): Flow<String> = callbackFlow {
        val eng = engine ?: throw IllegalStateException("No model loaded.")
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(settings.topK)
            .setTopP(settings.topP)
            .setTemperature(settings.temperature)
            .setRandomSeed(kotlin.random.Random.nextInt())
            .build()
        val session = LlmInferenceSession.createFromOptions(eng, sessionOptions)

        var tokenCount = 0
        
        input.images.forEach { bitmap ->
            val mpImage = BitmapImageBuilder(bitmap).build()
            session.addImage(mpImage)
        }
        
        session.addQueryChunk(input.prompt)
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
