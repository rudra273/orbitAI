package com.example.orbitai.data

import android.content.Context
import com.example.orbitai.data.db.Agent
import com.example.orbitai.data.db.AgentEntity
import com.example.orbitai.data.db.AppDatabase
import com.example.orbitai.data.db.ORBIT_AGENT_ID
import com.example.orbitai.data.db.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AgentRepository(private val context: Context) {

    private val db    = AppDatabase.getInstance(context)
    private val dao   = db.agentDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val agents: StateFlow<List<Agent>> = dao.observeAgents()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        // Ensure the default Orbit agent always exists (for fresh installs prior to migration)
        scope.launch {
            val existing = dao.getAgentById(ORBIT_AGENT_ID)
            if (existing == null) {
                dao.insertAgent(
                    AgentEntity(
                        id           = ORBIT_AGENT_ID,
                        name         = "Orbit",
                        systemPrompt = "You are Orbit, a helpful on-device AI assistant. Be concise, accurate, and friendly.",
                        isDefault    = true,
                        createdAt    = System.currentTimeMillis(),
                    )
                )
            }
        }
    }

    fun orbitAgent(): Agent? = agents.value.find { it.id == ORBIT_AGENT_ID }

    suspend fun createAgent(name: String, systemPrompt: String): Agent {
        val entity = AgentEntity(
            id           = UUID.randomUUID().toString(),
            name         = name.trim(),
            systemPrompt = systemPrompt.trim(),
            isDefault    = false,
            createdAt    = System.currentTimeMillis(),
        )
        dao.insertAgent(entity)
        return entity.toDomain()
    }

    suspend fun updateAgent(id: String, name: String, systemPrompt: String) =
        withContext(Dispatchers.IO) {
            dao.updateAgent(id, name.trim(), systemPrompt.trim())
        }

    suspend fun deleteAgent(id: String) = withContext(Dispatchers.IO) {
        dao.deleteAgent(id)
    }
}
