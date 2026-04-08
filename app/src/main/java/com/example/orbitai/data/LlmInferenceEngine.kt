package com.example.orbitai.data

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

data class InferenceInput(
    val prompt: String,
    val images: List<Bitmap> = emptyList()
)

interface LlmInferenceEngine {
    fun generateResponseStream(input: InferenceInput, maxDecodedTokens: Int): Flow<String>
    fun close()
}
