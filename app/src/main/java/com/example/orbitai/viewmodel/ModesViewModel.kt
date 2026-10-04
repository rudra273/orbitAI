package com.example.orbitai.viewmodel

import com.example.orbitai.appContainer
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.common.ModeInferenceSettingsStore
import com.example.orbitai.feature.modes.ModeRepository
import com.example.orbitai.core.database.Mode
import com.example.orbitai.core.database.ModePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = application.appContainer.modeRepository
    private val inferenceStore = ModeInferenceSettingsStore(application)

    val modes: StateFlow<List<Mode>> = repository.modes

    /** Spaces and downloaded models a mode can preset. */
    val spaces = application.appContainer.spaceRepository.spaces
    fun availableModels() = com.example.orbitai.core.model.availableChatModels(
        getApplication<Application>().appContainer.modelDownloader,
        com.example.orbitai.core.common.TokenStore(getApplication()),
    )

    fun createMode(name: String, systemPrompt: String, inference: InferenceSettings, isActive: Boolean, preset: ModePreset = ModePreset()) {
        viewModelScope.launch(Dispatchers.IO) {
            val mode = repository.createMode(name, systemPrompt, isActive, preset)
            inferenceStore.save(mode.id, inference)
        }
    }

    fun updateMode(id: String, name: String, systemPrompt: String, inference: InferenceSettings, isActive: Boolean, preset: ModePreset) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMode(id, name, systemPrompt, isActive, preset)
            inferenceStore.save(id, inference)
        }
    }

    fun addTemplate(template: com.example.orbitai.feature.modes.ModeTemplate) = createMode(
        template.name,
        template.systemPrompt,
        InferenceSettings(temperature = template.temperature),
        isActive = true,
        preset = ModePreset(accentColor = template.accentColor),
    )

    /** JSON for sharing [mode]. */
    fun exportMode(mode: Mode): String =
        com.example.orbitai.feature.modes.ModeJson.export(mode.name, mode.systemPrompt, inferenceStore.get(mode.id), mode.preset.accentColor)

    /** Adds a mode from a shared file; returns its name or an error message. */
    fun importMode(text: String): Result<String> = com.example.orbitai.feature.modes.ModeJson.import(text).onSuccess { imported ->
        createMode(imported.name, imported.systemPrompt, imported.inference, isActive = true, preset = ModePreset(accentColor = imported.accentColor))
    }.map { it.name }

    fun inferenceForMode(modeId: String): InferenceSettings = inferenceStore.get(modeId)

    fun defaultInference(): InferenceSettings = InferenceSettings()

    fun deleteMode(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMode(id)
        }
    }
}
