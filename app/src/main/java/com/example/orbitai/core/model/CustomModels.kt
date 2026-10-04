package com.example.orbitai.core.model

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A model file the user imported (f021); stored under `models/custom/`. */
data class CustomModelSpec(
    val id: String,
    val displayName: String,
    /** File name inside `models/custom/`. */
    val file: String,
    val format: ModelFormat,
    val promptStyle: PromptStyle,
    val supportsVision: Boolean,
    val contextTokens: Int,
) {
    fun toModel() = LlmModel(
        id = id,
        displayName = displayName,
        fileName = "$CUSTOM_DIR/$file",
        description = "Imported model",
        paramCount = "Custom",
        format = format,
        promptStyle = promptStyle,
        supportsVision = supportsVision,
        contextTokens = contextTokens,
    )

    companion object {
        const val CUSTOM_DIR = "custom"
        const val ID_PREFIX = "custom-"

        /** Format from the file extension, or null if it isn't a supported model file. */
        fun formatOf(fileName: String): ModelFormat? = when (fileName.substringAfterLast('.', "").lowercase()) {
            "litertlm" -> ModelFormat.LITERTLM
            "task" -> ModelFormat.TASK
            else -> null
        }
    }
}

/** Registry of imported models, kept in preferences. Initialised once by the application. */
object CustomModels {

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()
    private val _models = MutableStateFlow<List<CustomModelSpec>>(emptyList())
    val models: StateFlow<List<CustomModelSpec>> = _models.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("orbitai_custom_models", Context.MODE_PRIVATE)
        _models.value = decode(prefs.getString(KEY, null))
    }

    fun add(spec: CustomModelSpec) = save(_models.value.filter { it.id != spec.id } + spec)

    fun remove(id: String) = save(_models.value.filter { it.id != id })

    private fun save(list: List<CustomModelSpec>) {
        _models.value = list
        if (::prefs.isInitialized) prefs.edit { putString(KEY, gson.toJson(list)) }
    }

    internal fun decode(json: String?): List<CustomModelSpec> =
        json?.let { runCatching { gson.fromJson<List<CustomModelSpec>>(it, object : TypeToken<List<CustomModelSpec>>() {}.type) }.getOrNull() }
            ?.filter { it.id.isNotBlank() && it.file.isNotBlank() }
            .orEmpty()

    private const val KEY = "models"
}

/** Catalog models plus imported ones. */
fun allLocalModels(): List<LlmModel> = AVAILABLE_MODELS + CustomModels.models.value.map { it.toModel() }

fun findModel(id: String): LlmModel? = allLocalModels().find { it.id == id }
