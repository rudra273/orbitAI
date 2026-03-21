package com.example.orbitai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.data.InferenceSettings
import com.example.orbitai.data.ModeInferenceSettingsStore
import com.example.orbitai.data.ModeRepository
import com.example.orbitai.data.db.Mode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ModeRepository(application)
    private val inferenceStore = ModeInferenceSettingsStore(application)

    val modes: StateFlow<List<Mode>> = repository.modes

    fun createMode(name: String, systemPrompt: String, inference: InferenceSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            val mode = repository.createMode(name, systemPrompt)
            inferenceStore.save(mode.id, inference)
        }
    }

    fun updateMode(id: String, name: String, systemPrompt: String, inference: InferenceSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMode(id, name, systemPrompt)
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
