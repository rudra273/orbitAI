package com.example.orbitai.core.engine

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

data class InferenceInput(
    val prompt: String,
    val images: List<Bitmap> = emptyList(),
    /** Message-list form of [prompt] for providers that take structured turns (Gemini). */
    val structured: StructuredPrompt? = null,
)

data class StructuredPrompt(
    val systemInstruction: String,
    val turns: List<PromptTurn>,
)

data class PromptTurn(
    val isUser: Boolean,
    val text: String,
)

interface LlmInferenceEngine {
    fun generateResponseStream(input: InferenceInput, maxDecodedTokens: Int): Flow<String>
    fun close()
}
