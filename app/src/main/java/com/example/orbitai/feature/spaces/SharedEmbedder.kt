package com.example.orbitai.feature.spaces

import android.content.Context
import com.example.orbitai.core.model.E5_SMALL_EMBEDDING
import com.example.orbitai.core.model.EMBEDDINGGEMMA2_EMBEDDING
import com.example.orbitai.core.model.GECKO_EMBEDDING
import com.example.orbitai.feature.spaces.embedding.E5TextEmbedder
import com.example.orbitai.feature.spaces.embedding.EmbedKind
import com.example.orbitai.feature.spaces.embedding.Embedded
import com.example.orbitai.feature.spaces.embedding.EmbedderSettings
import com.example.orbitai.feature.spaces.embedding.EmbeddingGemmaTextEmbedder
import com.example.orbitai.feature.spaces.embedding.TextEmbedder

/**
 * The chosen embedding model for search and indexing, loaded on first use instead of per query or
 * per document and swapped when the user picks another one. Calls are serialized because the
 * engines aren't thread-safe.
 */
class SharedEmbedder(private val context: Context) {

    private val settings = EmbedderSettings(context)
    private var engine: TextEmbedder? = null

    /** The model new vectors are made with; stored vectors from any other model are not compared. */
    val activeModelId: String get() = settings.activeModelId

    @Synchronized
    fun embed(text: String, kind: EmbedKind): Embedded? {
        val wanted = settings.activeModelId
        if (engine?.modelId != wanted) {
            engine?.close()
            engine = create(wanted)
        }
        val loaded = engine ?: return null
        return loaded.embed(text, kind)?.let { Embedded(it, loaded.modelId) }
    }

    @Synchronized
    fun release() {
        engine?.close()
        engine = null
    }

    private fun create(modelId: String): TextEmbedder? = when (modelId) {
        E5_SMALL_EMBEDDING.id -> E5TextEmbedder.create(context)
        EMBEDDINGGEMMA2_EMBEDDING.id -> EmbeddingGemmaTextEmbedder.create(context)
        else -> EmbeddingModel.create(context)?.let { UseTextEmbedder(it) }
    }

    private class UseTextEmbedder(private val model: EmbeddingModel) : TextEmbedder {
        override val modelId: String = GECKO_EMBEDDING.id
        override fun embed(text: String, kind: EmbedKind): FloatArray? = model.embed(text)
        override fun close() = model.close()
    }
}
