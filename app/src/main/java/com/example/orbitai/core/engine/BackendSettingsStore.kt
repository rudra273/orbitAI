package com.example.orbitai.core.engine

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.ModelFormat
import com.example.orbitai.core.model.ModelProvider

/** Backends a model can run on here. NPU only for models built for this phone's chip. */
fun supportedBackends(model: LlmModel, socModel: String = Build.SOC_MODEL): List<HardwareBackend> = when {
    model.provider == ModelProvider.GEMINI -> emptyList()
    model.format == ModelFormat.LITERTLM -> listOfNotNull(
        HardwareBackend.NPU.takeIf { socModel in model.npuSocModels },
        HardwareBackend.GPU,
        HardwareBackend.CPU,
    )
    model.format == ModelFormat.TASK -> listOf(HardwareBackend.GPU, HardwareBackend.CPU)
    else -> listOf(HardwareBackend.CPU)
}

/** Per-model "Run on" choice and the latest speed test, which is dropped after an app or Android update. */
class BackendSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("orbitai_backend_choice", Context.MODE_PRIVATE)

    fun preference(modelId: String): BackendPreference =
        prefs.getString("pref:$modelId", null)?.let { runCatching { BackendPreference.valueOf(it) }.getOrNull() }
            ?: BackendPreference.AUTO

    fun setPreference(modelId: String, preference: BackendPreference) {
        prefs.edit { putString("pref:$modelId", preference.name) }
    }

    fun results(modelId: String): SpeedTestResults? {
        if (prefs.getString("stamp:$modelId", null) != BackendCascade.deviceStamp()) return null
        return prefs.getString("results:$modelId", null)?.let(SpeedTestResults::decode)
    }

    fun saveResults(modelId: String, results: SpeedTestResults) {
        prefs.edit {
            putString("results:$modelId", results.encode())
            putString("stamp:$modelId", BackendCascade.deviceStamp())
        }
    }

    fun plan(model: LlmModel): BackendPlan =
        BackendPlan(supportedBackends(model), preference(model.id), results(model.id)?.winner)

    /** Changes whenever loading [modelId] would pick a different backend. */
    fun loadKey(modelId: String): String = "${preference(modelId)}:${results(modelId)?.winner}"
}
