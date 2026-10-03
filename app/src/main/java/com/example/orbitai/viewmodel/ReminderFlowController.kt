package com.example.orbitai.viewmodel

import com.example.orbitai.feature.automation.executor.AutomationExecutor
import com.example.orbitai.feature.automation.parser.AutomationExecutionResult
import com.example.orbitai.feature.automation.parser.AutomationRequest
import com.example.orbitai.feature.automation.parser.ReminderDraft
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Reminder review dialog, scheduling and the saved-reminder actions. */
internal class ReminderFlowController(private val ctx: ChatControllerContext) {

    private val _uiState get() = ctx.uiState
    private val _events get() = ctx.events
    private val viewModelScope get() = ctx.scope
    private val reminderScheduler = ctx.container.reminderScheduler
    private val automationExecutor = AutomationExecutor(ctx.app)

    /** Shows the review dialog for a reminder parsed from chat. */
    fun showReview(request: ReminderReviewRequest) {
        _reminderReview.value = request
    }

    fun restoreOnStart() {
        viewModelScope.launch {
            try {
                reminderScheduler.restore()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(loadError = "Couldn't restore saved reminders. Reopen Orbit to retry.") }
            }
        }
    }

    private var pendingReminderExecution: ReminderReviewRequest? = null
    private val _reminderReview = MutableStateFlow<ReminderReviewRequest?>(null)
    val reminderReview = _reminderReview.asStateFlow()
    val reminders = reminderScheduler.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissReminderReview() { _reminderReview.value = null }

    fun cancelReminder(id: String) {
        viewModelScope.launch {
            try {
                reminderScheduler.cancel(id)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(loadError = "Couldn't remove the reminder. Please try again.") }
            }
        }
    }

    fun confirmReminder(draft: ReminderDraft, useLocalReminder: Boolean) {
        val reviewed = _reminderReview.value?.copy(draft = draft, useLocalReminder = useLocalReminder) ?: return
        _reminderReview.value = null
        viewModelScope.launch {
            try {
                if (useLocalReminder) {
                    scheduleReviewedReminder(reviewed)
                } else {
                    val result = automationExecutor.execute(AutomationRequest.CreateReminder(draft.title), draft)
                    if (result !is AutomationExecutionResult.Launched) _reminderReview.value = reviewed
                    ctx.handleIntentResult(
                        result,
                        onLaunched = { _uiState.update { it.copy(loadError = null, infoMessage = "Calendar opened. Save the event there to finish.") } },
                        onPermissionRequired = { result -> _uiState.update { it.copy(loadError = result.message) } },
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _reminderReview.value = reviewed
                _uiState.update { it.copy(loadError = "Couldn't save the reminder. Please try again.") }
            }
        }
    }

    private suspend fun scheduleReviewedReminder(reviewed: ReminderReviewRequest, requestPermission: Boolean = true) {
        when (val result = reminderScheduler.schedule(reviewed.draft)) {
            AutomationExecutionResult.Launched -> {
                _uiState.update { it.copy(loadError = null, infoMessage = "Reminder saved in Orbit for ${formatReminderTime(reviewed.draft.startTimeMillis)}${if (reminderScheduler.exactAlarmsEnabled()) "." else " (approximate; enable Alarms & reminders for precise timing)."}") }
            }
            is AutomationExecutionResult.Failed -> {
                _reminderReview.value = reviewed
                _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
            }
            is AutomationExecutionResult.PermissionRequired -> {
                _uiState.update { it.copy(loadError = result.message, infoMessage = null) }
                if (requestPermission) {
                    pendingReminderExecution = reviewed
                    _events.emit(ChatUiEvent.RequestNotificationsPermission)
                } else {
                    _reminderReview.value = reviewed
                }
            }
        }
    }

    fun editReminder(reminder: com.example.orbitai.core.database.ReminderEntity? = null) {
        val start = reminder?.triggerAt ?: System.currentTimeMillis() + 3_600_000L
        _uiState.update { it.copy(loadError = null, infoMessage = null) }
        _reminderReview.value = ReminderReviewRequest("", ReminderDraft(
            reminder?.title.orEmpty(), reminder?.description.orEmpty(), start, start + 1_800_000L,
            reminder?.id, reminder?.repeat ?: "NONE",
        ), true)
    }

    fun snoozeReminder(id: String) = updateReminder { reminderScheduler.snooze(id) }
    fun completeReminder(id: String) = updateReminder { reminderScheduler.complete(id) }
    fun refreshReminders() = updateReminder { reminderScheduler.restore() }
    private fun updateReminder(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action(); _uiState.update { it.copy(loadError = null) } }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(loadError = "Couldn't update the reminder. Please try again.") }
            }
        }
    }

    fun onNotificationsPermissionResult(granted: Boolean) {
        val pending = pendingReminderExecution
        pendingReminderExecution = null

        if (!granted) {
            _reminderReview.value = pending
            _uiState.update {
                it.copy(
                    loadError = "Enable notifications in Android Settings, or choose calendar handoff.",
                    infoMessage = null,
                )
            }
            return
        }

        if (pending != null) {
            viewModelScope.launch {
                try {
                    scheduleReviewedReminder(pending, requestPermission = false)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _reminderReview.value = pending
                    _uiState.update { it.copy(loadError = "Couldn't save the reminder. Please try again.") }
                }
            }
        }
    }

    // ── Chat management ───────────────────────────────────────────────────────

    private val reminderTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")

    private fun formatReminderTime(timeMillis: Long): String {
        return Instant.ofEpochMilli(timeMillis)
            .atZone(ZoneId.systemDefault())
            .format(reminderTimeFormatter)
    }
}
