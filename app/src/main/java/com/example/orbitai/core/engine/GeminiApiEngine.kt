package com.example.orbitai.core.engine

import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.common.TokenStore
import com.google.genai.Client
import com.google.genai.ResponseStream
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Content
import com.google.genai.types.Part
import com.google.genai.types.SafetySetting
import com.google.genai.types.HarmCategory
import com.google.genai.types.HarmBlockThreshold
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class GeminiApiEngine(
    context: android.content.Context,
    private val settings: InferenceSettings,
) : LlmInferenceEngine {

    companion object {
        private var quotaFastFailUntilMs: Long = 0L
        private const val QUOTA_COOLDOWN_MS: Long = 10 * 60 * 1000L
    }

    private val tokenStore = TokenStore(context)

    override fun generateResponseStream(input: InferenceInput, maxDecodedTokens: Int): Flow<String> = flow {
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
            .safetySettings(listOf(
                "HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT",
            ).map { category ->
                SafetySetting.builder()
                    .category(HarmCategory(category))
                    .threshold(HarmBlockThreshold("BLOCK_MEDIUM_AND_ABOVE"))
                    .build()
            })
            .maxOutputTokens(resolvedMaxTokens)
            .apply {
                input.structured?.systemInstruction?.takeIf { it.isNotBlank() }?.let {
                    systemInstruction(Content.fromParts(Part.fromText(it)))
                }
            }
            .build()

        var emittedAnyText = false
        var cumulativeText = ""

        val client = Client.builder()
            .apiKey(tokenStore.geminiApiKey)
            .build()

        try {
            val imageParts = input.images.map { bitmap ->
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                Part.fromBytes(stream.toByteArray(), "image/jpeg")
            }
            val contents = buildGeminiContents(input.prompt, input.structured, imageParts)

            val stream: ResponseStream<GenerateContentResponse> =
                client.models.generateContentStream(tokenStore.geminiModelName, contents, config)

            stream.use {
                for (chunk in it) {
                    currentCoroutineContext().ensureActive()
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
        } catch (e: CancellationException) {
            throw e
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

/**
 * Maps a prompt to Gemini contents: real user/model turns when [structured] is present,
 * otherwise [prompt] as a single user turn. Images attach to the last user turn.
 * Empty turns are dropped, consecutive same-role turns are merged and the history
 * starts with a user turn, as the API expects.
 */
internal fun buildGeminiContents(
    prompt: String,
    structured: StructuredPrompt?,
    imageParts: List<Part>,
): List<Content> {
    val turns = structured?.turns ?: listOf(PromptTurn(isUser = true, text = prompt))
    val merged = mutableListOf<PromptTurn>()
    for (turn in turns) {
        if (turn.text.isBlank()) continue
        if (merged.isEmpty() && !turn.isUser) continue
        val last = merged.lastOrNull()
        if (last != null && last.isUser == turn.isUser) {
            merged[merged.lastIndex] = last.copy(text = last.text + "\n\n" + turn.text)
        } else {
            merged += turn
        }
    }
    val lastUserIndex = merged.indexOfLast { it.isUser }
    return merged.mapIndexed { index, turn ->
        val parts = if (index == lastUserIndex) imageParts + Part.fromText(turn.text) else listOf(Part.fromText(turn.text))
        Content.builder().role(if (turn.isUser) "user" else "model").parts(parts).build()
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
