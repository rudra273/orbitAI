package com.example.orbitai.data

import com.google.genai.Client
import com.google.genai.ResponseStream
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GeminiApiEngine(
    context: android.content.Context,
    private val settings: InferenceSettings,
) : LlmInferenceEngine {

    private val tokenStore = TokenStore(context)

    override fun generateResponseStream(prompt: String, maxDecodedTokens: Int): Flow<String> = flow {
        if (!tokenStore.hasGeminiConfig()) {
            throw IllegalStateException("Gemini is not configured. Add API key and model in Developer settings.")
        }

        val resolvedMaxTokens = maxDecodedTokens.coerceAtLeast(1)
        val config = GenerateContentConfig.builder()
            .temperature(settings.temperature)
            .topK(settings.topK.toFloat())
            .topP(settings.topP)
            .maxOutputTokens(resolvedMaxTokens)
            .build()

        var emittedAnyText = false
        var cumulativeText = ""

        val client = Client.builder()
            .apiKey(tokenStore.geminiApiKey)
            .build()

        try {
            val stream: ResponseStream<GenerateContentResponse> =
                client.models.generateContentStream(tokenStore.geminiModelName, prompt, config)

            stream.use {
                for (chunk in it) {
                    val chunkText = chunk.text().orEmpty()
                    if (chunkText.isEmpty()) continue

                    val delta = when {
                        cumulativeText.isEmpty() -> chunkText
                        chunkText.startsWith(cumulativeText) -> chunkText.removePrefix(cumulativeText)
                        else -> chunkText
                    }

                    if (delta.isNotEmpty()) {
                        emittedAnyText = true
                        emit(delta)
                        cumulativeText += delta
                    }
                }
            }
        } finally {
            client.close()
        }

        if (!emittedAnyText) {
            throw IllegalStateException("Gemini returned an empty response.")
        }
    }

    override fun close() {
        // SDK model object is per request; nothing to close.
    }
}
