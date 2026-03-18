package com.example.orbitai.data

import android.content.Context
import com.example.orbitai.data.db.Mode
import com.example.orbitai.data.db.ModeEntity
import com.example.orbitai.data.db.AppDatabase
import com.example.orbitai.data.db.ORBIT_MODE_ID
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

    init {
        // Ensure the default Orbit mode always exists (for fresh installs prior to migration)
        scope.launch {
            val existing = dao.getModeById(ORBIT_MODE_ID)
            if (existing == null) {
                dao.insertMode(
                    ModeEntity(
                        id           = ORBIT_MODE_ID,
                        name         = "Orbit",
                        systemPrompt = "You are Orbit, a helpful on-device AI assistant. Be concise, accurate, and friendly.",
                        isDefault    = true,
                        createdAt    = System.currentTimeMillis(),
                    )
                )
            }
        }
    }

    fun orbitMode(): Mode? = modes.value.find { it.id == ORBIT_MODE_ID }

    suspend fun createMode(name: String, systemPrompt: String): Mode {
        val entity = ModeEntity(
            id           = UUID.randomUUID().toString(),
            name         = name.trim(),
            systemPrompt = systemPrompt.trim(),
            isDefault    = false,
            createdAt    = System.currentTimeMillis(),
        )
        dao.insertMode(entity)
        return entity.toDomain()
    }

    suspend fun updateMode(id: String, name: String, systemPrompt: String) =
        withContext(Dispatchers.IO) {
            dao.updateMode(id, name.trim(), systemPrompt.trim())
        }

    suspend fun deleteMode(id: String) = withContext(Dispatchers.IO) {
        dao.deleteMode(id)
    }
}
