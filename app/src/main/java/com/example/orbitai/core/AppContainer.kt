package com.example.orbitai.core

import android.content.Context
import com.example.orbitai.core.engine.LlmRepository
import com.example.orbitai.core.model.ModelDownloader
import com.example.orbitai.feature.automation.reminder.ReminderScheduler
import com.example.orbitai.feature.chat.ChatRepository
import com.example.orbitai.feature.diagnostics.DiagnosticsStore
import com.example.orbitai.feature.memory.MemoryRepository
import com.example.orbitai.feature.modes.ModeRepository
import com.example.orbitai.feature.spaces.SpaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Single instances shared by the app UI, the floating bubble, workers and receivers.
 * In particular there is one [LlmRepository], so at most one model is loaded per process.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Process-lifetime scope for work that must outlive a screen (e.g. releasing the model). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val diagnostics by lazy { DiagnosticsStore(appContext) }
    val llmRepository by lazy { LlmRepository(appContext) { state -> diagnostics.modelState = state } }
    val modelDownloader by lazy { ModelDownloader(appContext) }
    val chatRepository by lazy { ChatRepository(appContext) }
    val spaceRepository by lazy { SpaceRepository(appContext) }
    val modeRepository by lazy { ModeRepository(appContext) }
    val memoryRepository by lazy { MemoryRepository(appContext) }
    val reminderScheduler by lazy { ReminderScheduler(appContext) }
}
