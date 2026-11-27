package com.example.orbitai.feature.spaces.embedding

import android.content.Context
import android.util.Log
import com.example.orbitai.core.model.EMBEDDINGGEMMA2_EMBEDDING
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.EmbeddingEngine
import com.google.ai.edge.litertlm.EmbeddingEngineConfig
import com.google.ai.edge.litertlm.EmbeddingOptions
import com.google.ai.edge.litertlm.InputData
import java.io.File

/**
 * EmbeddingGemma 2 Text 270M (Apache-2.0, 100+ languages) run with LiteRT-LM on the CPU.
 * Queries and stored text get the task prefixes from the model card; vectors come back at length 1.
 */
class EmbeddingGemmaTextEmbedder private constructor(private val engine: EmbeddingEngine) : TextEmbedder {

    override val modelId: String = EMBEDDINGGEMMA2_EMBEDDING.id

    override fun embed(text: String, kind: EmbedKind): FloatArray? = try {
        engine.computeEmbedding(listOf(InputData.Text(prompt(text, kind))), EmbeddingOptions(normalize = true)).embedding
    } catch (e: Exception) {
        Log.e(TAG, "embed() failed: ${e.message}", e)
        null
    }

    override fun close() {
        engine.close()
    }

    companion object {
        private const val TAG = "EmbeddingGemmaEmbedder"
        private const val MAX_TOKENS = 512

        /** The input format EmbeddingGemma 2 was trained with for search. */
        internal fun prompt(text: String, kind: EmbedKind): String = when (kind) {
            EmbedKind.QUERY -> "task: search result | query: $text"
            EmbedKind.PASSAGE -> "title: none | text: $text"
        }

        /** Null when the model file isn't downloaded yet or fails to load. */
        fun create(context: Context): EmbeddingGemmaTextEmbedder? {
            val model = File(context.getExternalFilesDir(null), "models/${EMBEDDINGGEMMA2_EMBEDDING.fileName}")
            if (!model.exists()) return null
            return try {
                val engine = EmbeddingEngine(
                    EmbeddingEngineConfig(
                        modelPath = model.absolutePath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.absolutePath,
                        maxInputLength = MAX_TOKENS,
                    )
                )
                engine.initialize()
                EmbeddingGemmaTextEmbedder(engine)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load model: ${e.message}", e)
                null
            }
        }
    }
}
