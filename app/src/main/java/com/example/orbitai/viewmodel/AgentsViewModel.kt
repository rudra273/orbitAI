package com.example.orbitai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.data.AgentRepository
import com.example.orbitai.data.db.Agent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AgentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AgentRepository(application)

    val agents: StateFlow<List<Agent>> = repository.agents

    fun createAgent(name: String, systemPrompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createAgent(name, systemPrompt)
        }
    }

    fun updateAgent(id: String, name: String, systemPrompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAgent(id, name, systemPrompt)
        }
    }

    fun deleteAgent(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAgent(id)
        }
    }
}
