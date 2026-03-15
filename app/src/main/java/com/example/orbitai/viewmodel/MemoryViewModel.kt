package com.example.orbitai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.data.db.MemoryEntity
import com.example.orbitai.data.memory.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MemoryRepository(application)

    val memories: StateFlow<List<MemoryEntity>> = repository.memories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun deleteMemory(id: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteMemory(id) }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) { repository.clearAll() }
    }

    fun addMemory(content: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.addMemory(content, source = "explicit") }
    }
}
