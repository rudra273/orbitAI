package com.example.orbitai.feature.spaces.embedding

enum class EmbedKind { QUERY, PASSAGE }

/** A vector together with the id of the embedding model that produced it; vectors of different models never mix. */
class Embedded(val vector: FloatArray, val modelId: String)

interface TextEmbedder {
    val modelId: String
    fun embed(text: String, kind: EmbedKind): FloatArray?
    fun close()
}
