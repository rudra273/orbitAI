package com.example.orbitai.data

import kotlinx.coroutines.flow.Flow

interface LlmInferenceEngine {
    fun generateResponseStream(prompt: String, maxDecodedTokens: Int): Flow<String>
    fun close()
}
