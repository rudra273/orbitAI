package com.example.orbitai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.data.ModeRepository
import com.example.orbitai.data.db.Mode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ModeRepository(application)

    val modes: StateFlow<List<Mode>> = repository.modes

    fun createMode(name: String, systemPrompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createMode(name, systemPrompt)
        }
    }

    fun updateMode(id: String, name: String, systemPrompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMode(id, name, systemPrompt)
        }
    }

    fun deleteMode(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMode(id)
        }
    }
}
