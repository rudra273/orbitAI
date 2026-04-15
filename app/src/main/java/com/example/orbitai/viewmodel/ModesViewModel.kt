package com.example.orbitai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.common.ModeInferenceSettingsStore
import com.example.orbitai.feature.modes.ModeRepository
import com.example.orbitai.core.database.Mode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ModeRepository(application)
    private val inferenceStore = ModeInferenceSettingsStore(application)

    val modes: StateFlow<List<Mode>> = repository.modes

    fun createMode(name: String, systemPrompt: String, inference: InferenceSettings, isActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val mode = repository.createMode(name, systemPrompt, isActive)
            inferenceStore.save(mode.id, inference)
        }
    }

    fun updateMode(id: String, name: String, systemPrompt: String, inference: InferenceSettings, isActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMode(id, name, systemPrompt, isActive)
            inferenceStore.save(id, inference)
        }
    }

    fun inferenceForMode(modeId: String): InferenceSettings = inferenceStore.get(modeId)

    fun defaultInference(): InferenceSettings = InferenceSettings()

    fun deleteMode(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMode(id)
        }
    }
}
