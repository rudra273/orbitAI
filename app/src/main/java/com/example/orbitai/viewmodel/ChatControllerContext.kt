package com.example.orbitai.viewmodel

import android.app.Application
import com.example.orbitai.appContainer
import com.example.orbitai.feature.automation.parser.AutomationExecutionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** State and services shared by the controllers behind [ChatViewModel]. */
internal class ChatControllerContext(
    val app: Application,
    /** The ViewModel's scope; work launched here is cancelled when the ViewModel is cleared. */
    val scope: CoroutineScope,
) {
    val container = app.appContainer
    val uiState = MutableStateFlow(ChatUiState())
    val events = MutableSharedFlow<ChatUiEvent>()

    fun handleIntentResult(
        result: AutomationExecutionResult,
        onLaunched: (() -> Unit)?,
        onPermissionRequired: (AutomationExecutionResult.PermissionRequired) -> Unit,
    ) {
        when (result) {
            AutomationExecutionResult.Launched -> onLaunched?.invoke()
            is AutomationExecutionResult.Failed -> {
                uiState.update { it.copy(loadError = result.message, infoMessage = null) }
            }
            is AutomationExecutionResult.PermissionRequired -> onPermissionRequired(result)
        }
    }
}
