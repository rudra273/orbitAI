package com.example.orbitai.data.rag

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import com.google.mediapipe.tasks.core.BaseOptions
import java.io.File
import kotlin.math.sqrt

/** Wraps MediaPipe TextEmbedder. Returns null safely if model not found. */
class EmbeddingModel private constructor(private val embedder: TextEmbedder) {

    /** Embed text → float vector. Returns null on error. */
    fun embed(text: String): FloatArray? = try {
        val result = embedder.embed(text)
        result.embeddingResult().embeddings()[0].floatEmbedding()
    } catch (e: Exception) {
        Log.e("EmbeddingModel", "embed() failed: ${e.message}", e)
        null
    }

    fun close() = embedder.close()

    companion object {
        const val MODEL_FILENAME = "universal_sentence_encoder.tflite"
        private const val TAG = "EmbeddingModel"

        /** Returns null if model file doesn't exist yet. */
        fun create(context: Context): EmbeddingModel? {
            val modelFile = File(context.getExternalFilesDir(null), "models/$MODEL_FILENAME")
            if (!modelFile.exists()) {
                Log.w(TAG, "Model file not found: ${modelFile.absolutePath}")
                return null
            }
            Log.d(TAG, "Loading model from ${modelFile.absolutePath} (${modelFile.length()} bytes)")
            return try {
                val options = TextEmbedderOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath(modelFile.absolutePath).build())
                    .build()
                EmbeddingModel(TextEmbedder.createFromOptions(context, options))
                    .also { Log.d(TAG, "Model loaded successfully") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model: ${e.message}", e)
                null
            }
        }
    }
}

/** Cosine similarity between two float vectors. Range: -1..1. */
fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    var dot = 0f; var normA = 0f; var normB = 0f
    for (i in a.indices) { dot += a[i] * b[i]; normA += a[i] * a[i]; normB += b[i] * b[i] }
    return if (normA == 0f || normB == 0f) 0f else dot / (sqrt(normA) * sqrt(normB))
}

/** Serialize FloatArray → ByteArray (little-endian) for Room BLOB storage. */
fun FloatArray.toByteArray(): ByteArray {
    val buf = java.nio.ByteBuffer.allocate(size * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    forEach { buf.putFloat(it) }
    return buf.array()
}

/** Deserialize ByteArray → FloatArray. */
fun ByteArray.toFloatArray(): FloatArray {
    val buf = java.nio.ByteBuffer.wrap(this).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    return FloatArray(size / 4) { buf.float }
}
