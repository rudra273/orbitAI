package com.example.orbitai.feature.spaces.embedding

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.example.orbitai.core.model.E5_SMALL_EMBEDDING
import java.io.File
import kotlin.math.sqrt

/**
 * multilingual-e5-small (MIT, 100 languages incl. Hindi) run with ONNX Runtime. e5 expects
 * "query: " in front of questions and "passage: " in front of stored text; the vector is the
 * mean of the token outputs, scaled to length 1.
 */
class E5TextEmbedder private constructor(
    private val environment: OrtEnvironment,
    private val session: OrtSession,
    private val tokenizer: UnigramTokenizer,
) : TextEmbedder {

    override val modelId: String = E5_SMALL_EMBEDDING.id

    override fun embed(text: String, kind: EmbedKind): FloatArray? = try {
        val prefix = if (kind == EmbedKind.QUERY) "query: " else "passage: "
        val ids = tokenizer.encode(prefix + text, MAX_TOKENS).map { it.toLong() }.toLongArray()
        val mask = LongArray(ids.size) { 1L }
        val types = LongArray(ids.size)
        val inputs = HashMap<String, OnnxTensor>()
        try {
            inputs["input_ids"] = OnnxTensor.createTensor(environment, arrayOf(ids))
            inputs["attention_mask"] = OnnxTensor.createTensor(environment, arrayOf(mask))
            if ("token_type_ids" in session.inputNames) inputs["token_type_ids"] = OnnxTensor.createTensor(environment, arrayOf(types))
            session.run(inputs).use { result ->
                @Suppress("UNCHECKED_CAST")
                val hidden = (result[0].value as Array<Array<FloatArray>>)[0]
                meanPooled(hidden)
            }
        } finally {
            inputs.values.forEach { it.close() }
        }
    } catch (e: Exception) {
        Log.e(TAG, "embed() failed: ${e.message}", e)
        null
    }

    override fun close() {
        session.close()
    }

    companion object {
        private const val TAG = "E5TextEmbedder"
        private const val MAX_TOKENS = 512

        /** Mean of the token vectors, scaled to length 1. */
        internal fun meanPooled(tokens: Array<FloatArray>): FloatArray {
            val size = tokens.first().size
            val sum = FloatArray(size)
            tokens.forEach { token -> for (i in 0 until size) sum[i] += token[i] }
            var norm = 0f
            for (i in 0 until size) { sum[i] /= tokens.size; norm += sum[i] * sum[i] }
            val length = sqrt(norm).takeIf { it > 0f } ?: 1f
            for (i in 0 until size) sum[i] /= length
            return sum
        }

        /** Null when the model or tokenizer file isn't downloaded yet. */
        fun create(context: Context): E5TextEmbedder? {
            val dir = File(context.getExternalFilesDir(null), "models")
            val model = File(dir, E5_SMALL_EMBEDDING.fileName)
            val tokenizerFile = File(dir, E5_SMALL_EMBEDDING.tokenizerFileName ?: return null)
            if (!model.exists() || !tokenizerFile.exists()) return null
            return try {
                val environment = OrtEnvironment.getEnvironment()
                val options = OrtSession.SessionOptions().apply { setIntraOpNumThreads(2) }
                E5TextEmbedder(environment, environment.createSession(model.absolutePath, options), UnigramTokenizer.load(tokenizerFile))
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load model: ${e.message}", e)
                null
            }
        }
    }
}
