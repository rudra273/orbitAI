package com.example.orbitai.core.model

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs

/** How well a model is expected to run on this phone. */
enum class ModelFit { GOOD, SLOW, TOO_LARGE }

/**
 * Rough fit between a model and the phone's memory. Estimates only: real usage depends on the
 * runtime, backend and context length, so "too large" models can still be downloaded after a warning.
 */
object DeviceFit {

    private const val RUNTIME_OVERHEAD_BYTES = 1_000_000_000L

    /** Preferred models for the recommendation, best first. */
    private val RECOMMENDATION_ORDER = listOf("gemma4-e4b", "gemma4-e2b", "gemma3-1b-litertlm", "onnx-gemma3-270m-it")

    /** Weights plus KV cache, activations and runtime overhead. */
    fun estimatedRamBytes(downloadBytes: Long): Long = (downloadBytes * 1.3).toLong() + RUNTIME_OVERHEAD_BYTES

    fun classify(downloadBytes: Long?, totalRamBytes: Long): ModelFit {
        if (downloadBytes == null || totalRamBytes <= 0L) return ModelFit.GOOD
        val share = estimatedRamBytes(downloadBytes).toDouble() / totalRamBytes
        return when {
            share <= 0.60 -> ModelFit.GOOD
            share <= 0.85 -> ModelFit.SLOW
            else -> ModelFit.TOO_LARGE
        }
    }

    fun fitOf(model: LlmModel, totalRamBytes: Long): ModelFit =
        classify(MODEL_DOWNLOAD_SPECS[model.id]?.totalSizeBytes, totalRamBytes)

    /** The best model that fits well on a phone with [totalRamBytes], or null if none does. */
    fun recommendedModelId(totalRamBytes: Long): String? = RECOMMENDATION_ORDER.firstOrNull { id ->
        classify(MODEL_DOWNLOAD_SPECS[id]?.totalSizeBytes, totalRamBytes) == ModelFit.GOOD
    }

    /** Leaves a 10% margin for the partial file and other app data. */
    fun hasStorageFor(downloadBytes: Long, freeBytes: Long): Boolean = freeBytes >= downloadBytes + downloadBytes / 10

    /** Short "best for" label shown on model rows. */
    fun bestFor(model: LlmModel): String {
        val size = MODEL_DOWNLOAD_SPECS[model.id]?.totalSizeBytes ?: 0L
        return when {
            model.supportsVision -> "Images + chat"
            size in 1 until 1_200_000_000L -> "Quick replies"
            else -> "Reasoning"
        }
    }
}

/** RAM and model-storage facts for this phone. */
class DeviceSpecs(private val context: Context) {
    val totalRamBytes: Long by lazy {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        info.totalMem
    }

    /** Free bytes on the volume that holds downloaded models. */
    fun freeModelStorageBytes(): Long =
        runCatching { StatFs((context.getExternalFilesDir(null) ?: context.filesDir).path).availableBytes }.getOrDefault(Long.MAX_VALUE)
}
