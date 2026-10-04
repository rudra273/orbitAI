package com.example.orbitai.feature.modes

import com.example.orbitai.core.common.InferenceSettings
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** A mode read from a shared file. */
data class ImportedMode(
    val name: String,
    val systemPrompt: String,
    val inference: InferenceSettings,
    val accentColor: Int?,
)

/**
 * Mode files for sharing: name, prompt, sampling settings and colour. The model and Spaces are
 * left out because they only exist on the sender's phone.
 */
object ModeJson {

    const val MAX_BYTES = 32_000
    private const val FORMAT = "orbit-mode"
    private const val VERSION = 1

    fun export(name: String, systemPrompt: String, inference: InferenceSettings, accentColor: Int?): String =
        JsonObject().apply {
            addProperty("format", FORMAT)
            addProperty("version", VERSION)
            addProperty("name", name)
            addProperty("systemPrompt", systemPrompt)
            addProperty("temperature", inference.temperature)
            addProperty("topK", inference.topK)
            addProperty("topP", inference.topP)
            addProperty("maxTokens", inference.maxDecodedTokens)
            accentColor?.let { addProperty("accentColor", "#%06X".format(it and 0xFFFFFF)) }
        }.toString()

    /** Reads a mode file, or fails with a message for the user. */
    fun import(text: String): Result<ImportedMode> = runCatching {
        require(text.toByteArray().size <= MAX_BYTES) { "This file is too large to be an Orbit mode." }
        val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
            ?: throw IllegalArgumentException("This isn't an Orbit mode file.")
        require(json.get("format")?.takeIf { it.isJsonPrimitive }?.asString == FORMAT) { "This isn't an Orbit mode file." }
        val name = json.string("name")?.trim()?.take(60)
        val prompt = json.string("systemPrompt")?.trim()
        require(!name.isNullOrEmpty()) { "The mode file has no name." }
        require(!prompt.isNullOrEmpty()) { "The mode file has no instructions." }
        val defaults = InferenceSettings()
        ImportedMode(
            name = name,
            systemPrompt = prompt.take(8_000),
            inference = InferenceSettings(
                temperature = json.number("temperature")?.toFloat()?.coerceIn(0f, 2f) ?: defaults.temperature,
                topK = json.number("topK")?.toInt()?.coerceIn(1, 100) ?: defaults.topK,
                topP = json.number("topP")?.toFloat()?.coerceIn(0f, 1f) ?: defaults.topP,
                maxDecodedTokens = json.number("maxTokens")?.toInt()?.coerceIn(64, 8192) ?: defaults.maxDecodedTokens,
            ),
            accentColor = json.string("accentColor")?.removePrefix("#")?.takeIf { it.length == 6 }?.toIntOrNull(16),
        )
    }

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun JsonObject.number(key: String): Double? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble
}
