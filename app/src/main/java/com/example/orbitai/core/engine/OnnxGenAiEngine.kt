package com.example.orbitai.core.engine

import ai.onnxruntime.genai.Config
import ai.onnxruntime.genai.Generator
import ai.onnxruntime.genai.GeneratorParams
import ai.onnxruntime.genai.Images
import ai.onnxruntime.genai.Model
import ai.onnxruntime.genai.MultiModalProcessor
import ai.onnxruntime.genai.NamedTensors
import ai.onnxruntime.genai.Sequences
import ai.onnxruntime.genai.Tokenizer
import ai.onnxruntime.genai.TokenizerStream
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.OnnxGenAiConfigFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ensureActive
import java.io.File

class OnnxGenAiEngine(
    private val context: Context,
    private val model: LlmModel,
    private val settings: InferenceSettings,
) : LlmInferenceEngine {

    companion object {
        private const val TAG = "OnnxGenAiEngine"
    }

    private val modelPath = File(context.getExternalFilesDir(null), "models/${model.fileName}")
    private var config: Config? = null
    private var runtimeModel: Model? = null
    private var tokenizer: Tokenizer? = null
    private var multimodalProcessor: MultiModalProcessor? = null

    init {
        if (!model.isChatSelectable) {
            throw IllegalStateException(
                "${model.displayName} is not enabled for chat yet. Please switch to Phi, Llama, Gemma, or LiteRT."
            )
        }
        OnnxGenAiConfigFactory.ensureConfig(model, modelPath)
        val cfg = Config(modelPath.absolutePath)
        config = cfg
        runtimeModel = Model(cfg)
        tokenizer = Tokenizer(runtimeModel!!)
        if (model.supportsVision) {
            multimodalProcessor = MultiModalProcessor(runtimeModel!!)
        }
        Log.d(TAG, "Initialized ONNX GenAI model at ${modelPath.absolutePath}")
    }

    override fun generateResponseStream(input: InferenceInput, maxDecodedTokens: Int): Flow<String> = flow {
        val activeModel = runtimeModel ?: throw IllegalStateException("No model loaded.")
        val activeTokenizer = tokenizer ?: throw IllegalStateException("Tokenizer unavailable.")

        if (input.images.isNotEmpty() && !model.supportsVision) {
            throw IllegalArgumentException("${model.displayName} does not support image input.")
        }

        if (input.images.isNotEmpty()) {
            val processor = multimodalProcessor ?: throw IllegalStateException("Vision processor unavailable.")
            val tempImage = writeBitmapToTempFile(input.images.first())
            try {
                Images(tempImage.absolutePath).use { images ->
                    processor.processImages(input.prompt, images).use { tensors ->
                        generateWithNamedTensors(
                            activeModel = activeModel,
                            processor = processor,
                            tensors = tensors,
                            maxDecodedTokens = maxDecodedTokens,
                        ) { chunk -> emit(chunk) }
                    }
                }
            } finally {
                tempImage.delete()
            }
        } else {
            activeTokenizer.encode(input.prompt).use { promptTokens ->
                generateWithTextPrompt(
                    activeModel = activeModel,
                    activeTokenizer = activeTokenizer,
                    promptTokens = promptTokens,
                    maxDecodedTokens = maxDecodedTokens,
                ) { chunk -> emit(chunk) }
            }
        }
    }

    override fun close() {
        try {
            multimodalProcessor?.close()
        } catch (_: Exception) {
        }
        multimodalProcessor = null

        try {
            tokenizer?.close()
        } catch (_: Exception) {
        }
        tokenizer = null

        try {
            runtimeModel?.close()
        } catch (_: Exception) {
        }
        runtimeModel = null

        try {
            config?.close()
        } catch (_: Exception) {
        }
        config = null
    }

    private suspend fun generateWithTextPrompt(
        activeModel: Model,
        activeTokenizer: Tokenizer,
        promptTokens: Sequences,
        maxDecodedTokens: Int,
        emitChunk: suspend (String) -> Unit,
    ) {
        val promptLength = promptTokens.getSequence(0).size
        val params = createGeneratorParams(activeModel, promptLength + maxDecodedTokens)
        try {
            activeTokenizer.createStream().use { stream ->
                Generator(activeModel, params).use { generator ->
                    generator.appendTokenSequences(promptTokens)
                    streamGenerator(generator, stream, maxDecodedTokens, emitChunk)
                }
            }
        } finally {
            params.close()
        }
    }

    private suspend fun generateWithNamedTensors(
        activeModel: Model,
        processor: MultiModalProcessor,
        tensors: NamedTensors,
        maxDecodedTokens: Int,
        emitChunk: suspend (String) -> Unit,
    ) {
        val params = createGeneratorParams(activeModel, maxDecodedTokens + 4096)
        try {
            processor.createStream().use { stream ->
                Generator(activeModel, params).use { generator ->
                    generator.setInputs(tensors)
                    streamGenerator(generator, stream, maxDecodedTokens, emitChunk)
                }
            }
        } finally {
            params.close()
        }
    }

    private suspend fun streamGenerator(
        generator: Generator,
        stream: TokenizerStream,
        maxDecodedTokens: Int,
        emitChunk: suspend (String) -> Unit,
    ) {
        var emittedTokenCount = 0
        while (emittedTokenCount < maxDecodedTokens) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            generator.generateNextToken()
            if (generator.isDone()) break

            val tokenId = generator.getLastTokenInSequence(0)
            val chunk = stream.decode(tokenId)
            if (chunk.isNotEmpty()) {
                emitChunk(chunk)
            }
            emittedTokenCount += 1
        }
    }

    private fun createGeneratorParams(activeModel: Model, maxLength: Int): GeneratorParams {
        val params = GeneratorParams(activeModel)
        params.setSearchOption("max_length", maxLength.toDouble())
        params.setSearchOption("top_k", settings.topK.toDouble())
        params.setSearchOption("top_p", settings.topP.toDouble())
        params.setSearchOption("temperature", settings.temperature.toDouble())
        params.setSearchOption(
            "do_sample",
            settings.temperature > 0f && (settings.topK > 1 || settings.topP < 1f || settings.temperature != 1f)
        )
        return params
    }

    private fun writeBitmapToTempFile(bitmap: Bitmap): File {
        val tempFile = File.createTempFile("orbit-onnx-", ".jpg", context.cacheDir)
        tempFile.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }
        return tempFile
    }
}
