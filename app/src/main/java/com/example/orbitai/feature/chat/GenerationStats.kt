package com.example.orbitai.feature.chat

import java.util.Locale

/** How one reply was generated, for the optional details line under it. */
data class GenerationStats(
    val firstTokenMs: Long?,
    val streamingMs: Long,
    /** Estimated from characters (≈ 4 per token); engines don't report counts. */
    val approxTokens: Int,
    val backend: String?,
    val modelName: String,
) {
    val tokensPerSecond: Double?
        get() = if (streamingMs <= 0 || approxTokens == 0) null else approxTokens * 1000.0 / streamingMs

    fun label(locale: Locale = Locale.US): String = listOfNotNull(
        firstTokenMs?.let { String.format(locale, "%.1fs to first token", it / 1000.0) },
        tokensPerSecond?.let { String.format(locale, "≈%.1f tok/s", it) },
        backend,
        modelName,
    ).joinToString(" · ")

    companion object {
        fun estimateTokens(text: String): Int = (text.length + 3) / 4
    }
}
