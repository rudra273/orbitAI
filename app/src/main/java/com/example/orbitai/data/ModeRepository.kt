package com.example.orbitai.data

import android.content.Context
import com.example.orbitai.data.db.Mode
import com.example.orbitai.data.db.ModeEntity
import com.example.orbitai.data.db.AppDatabase
import com.example.orbitai.data.db.CONCISE_MODE_ID
import com.example.orbitai.data.db.ORBIT_MODE_ID
import com.example.orbitai.data.db.STEP_BY_STEP_MODE_ID
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

class ModeRepository(private val context: Context) {

    private val db    = AppDatabase.getInstance(context)
    private val dao   = db.modeDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val modes: StateFlow<List<Mode>> = dao.observeModes()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val activeModes: StateFlow<List<Mode>> = modes
        .map { list -> list.filter { it.isActive } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        // Ensure built-in default modes always exist (fresh installs and pre-migration users)
        scope.launch {
            val defaults = listOf(
                ModeEntity(
                    id = ORBIT_MODE_ID,
                    name = "Orbit",
                    systemPrompt = "You are Orbit, a helpful on-device AI assistant. Be concise, accurate, and friendly.",
                    isDefault = true,
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                ),
                ModeEntity(
                    id = CONCISE_MODE_ID,
                    name = "Concise",
                    systemPrompt = "You are a concise assistant. Give short, direct answers. Use only essential details and avoid extra explanation unless asked.",
                    isDefault = true,
                    isActive = true,
                    createdAt = System.currentTimeMillis() + 1,
                ),
                ModeEntity(
                    id = STEP_BY_STEP_MODE_ID,
                    name = "Step-by-step",
                    systemPrompt = "You are a step-by-step assistant. Break solutions into clear numbered steps, explain each step briefly, and keep progression logical.",
                    isDefault = true,
                    isActive = true,
                    createdAt = System.currentTimeMillis() + 2,
                ),
            )
            defaults.forEach { mode ->
                if (dao.getModeById(mode.id) == null) {
                    dao.insertMode(mode)
                }
            }
        }
    }

    fun orbitMode(): Mode? = modes.value.find { it.id == ORBIT_MODE_ID }

    suspend fun createMode(name: String, systemPrompt: String, isActive: Boolean): Mode {
        val entity = ModeEntity(
            id           = UUID.randomUUID().toString(),
            name         = name.trim(),
            systemPrompt = systemPrompt.trim(),
            isDefault    = false,
            isActive     = isActive,
            createdAt    = System.currentTimeMillis(),
        )
        dao.insertMode(entity)
        return entity.toDomain()
    }

    suspend fun updateMode(id: String, name: String, systemPrompt: String, isActive: Boolean) =
        withContext(Dispatchers.IO) {
            dao.updateMode(id, name.trim(), systemPrompt.trim(), isActive)
        }

    suspend fun deleteMode(id: String) = withContext(Dispatchers.IO) {
        dao.deleteMode(id)
    }
}
