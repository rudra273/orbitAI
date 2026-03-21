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

    companion object {
        private var quotaFastFailUntilMs: Long = 0L
        private const val QUOTA_COOLDOWN_MS: Long = 10 * 60 * 1000L
    }

    private val tokenStore = TokenStore(context)

    override fun generateResponseStream(prompt: String, maxDecodedTokens: Int): Flow<String> = flow {
        if (!tokenStore.hasGeminiConfig()) {
            throw IllegalStateException("Gemini is not configured. Add API key and model in Settings > Model.")
        }

        val now = System.currentTimeMillis()
        if (now < quotaFastFailUntilMs) {
            val seconds = ((quotaFastFailUntilMs - now) / 1000L).coerceAtLeast(1L)
            throw IllegalStateException(
                "Gemini quota is temporarily exhausted. Retry in about ${seconds}s, or switch model/API key."
            )
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
        } catch (e: Exception) {
            if (e.isQuotaError()) {
                quotaFastFailUntilMs = System.currentTimeMillis() + QUOTA_COOLDOWN_MS
            }
            throw IllegalStateException(e.toGeminiUserMessage())
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

private fun Exception.isQuotaError(): Boolean {
    val normalized = message.orEmpty().lowercase()
    return "resource_exhausted" in normalized || "quota exceeded" in normalized || "rate limit" in normalized
}

private fun Exception.toGeminiUserMessage(): String {
    val message = message.orEmpty()
    val normalized = message.lowercase()

    return when {
        "resource_exhausted" in normalized || "quota exceeded" in normalized || "rate limit" in normalized -> {
            "Gemini quota exceeded for the current API key or project. Wait for quota reset, switch to another Gemini model, or use a different API key/project."
        }

        "api key not valid" in normalized || "permission_denied" in normalized || "unauthenticated" in normalized -> {
            "Gemini rejected the current API key. Check the key in Settings > Model."
        }

        "not found" in normalized || "unsupported model" in normalized || "model" in normalized && "not" in normalized && "available" in normalized -> {
            "The saved Gemini model name is no longer available. Update it in Settings > Model."
        }

        message.isNotBlank() -> message
        else -> "Gemini request failed."
    }
}
