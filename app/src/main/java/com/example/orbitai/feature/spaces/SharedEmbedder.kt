package com.example.orbitai.feature.spaces

import android.content.Context

/**
 * One embedding model for search and indexing, loaded on first use instead of per query or
 * per document. Calls are serialized because the MediaPipe embedder isn't thread-safe.
 */
class SharedEmbedder(private val context: Context) {

    private var model: EmbeddingModel? = null

    @Synchronized
    fun embed(text: String): FloatArray? {
        val loaded = model ?: EmbeddingModel.create(context)?.also { model = it } ?: return null
        return loaded.embed(text)
    }

    @Synchronized
    fun release() {
        model?.close()
        model = null
    }
}
