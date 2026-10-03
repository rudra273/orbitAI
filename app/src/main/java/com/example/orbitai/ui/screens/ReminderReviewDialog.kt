package com.example.orbitai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.orbitai.feature.automation.parser.ReminderDraft
import com.example.orbitai.viewmodel.ReminderReviewRequest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReminderReviewDialog(
    request: ReminderReviewRequest,
    onDismiss: () -> Unit,
    onConfirm: (ReminderDraft, Boolean) -> Unit,
    saveError: String? = null,
) {
    val draft = request.draft
    val localTime = Instant.ofEpochMilli(draft.startTimeMillis).atZone(ZoneId.systemDefault())
    var title by rememberSaveable(request) { mutableStateOf(draft.title) }
    var description by rememberSaveable(request) { mutableStateOf(draft.description) }
    var date by rememberSaveable(request) { mutableStateOf(localTime.toLocalDate().toString()) }
    var time by rememberSaveable(request) { mutableStateOf(localTime.format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var local by rememberSaveable(request) { mutableStateOf(request.useLocalReminder) }
    var repeat by rememberSaveable(request) { mutableStateOf(draft.repeat) }
    var error by rememberSaveable(request) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review reminder") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Check the suggested date and time before saving.")
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Details") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true)
                OutlinedTextField(time, { time = it }, label = { Text("Time (HH:mm)") }, singleLine = true)
                Text("Time zone: ${ZoneId.systemDefault().id}")
                Text("Repeat")
                Row {
                    com.example.orbitai.feature.automation.reminder.ReminderRepeat.entries.forEach { option ->
                        TextButton(onClick = { repeat = option.name }) { Text(if (repeat == option.name) "✓ ${option.label}" else option.label) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = local, onCheckedChange = { local = it })
                    Text("Notify me in Orbit")
                }
                Text(if (local) "Saved on this device. Precise timing needs Android’s Alarms & reminders access." else "Open your calendar to review and save the event.")
                (error ?: saveError)?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val start = runCatching {
                    val requested = LocalDateTime.of(LocalDate.parse(date.trim()), LocalTime.parse(time.trim()))
                    val zoned = requested.atZone(ZoneId.systemDefault())
                    require(zoned.toLocalDateTime() == requested)
                    zoned.toInstant().toEpochMilli()
                }.getOrNull()
                if (title.isBlank() || start == null || start <= System.currentTimeMillis()) {
                    error = "Enter a title and a valid future date and time."
                } else {
                    onConfirm(draft.copy(
                        title = title.trim(), description = description.trim(), startTimeMillis = start, repeat = repeat,
                        endTimeMillis = start + (draft.endTimeMillis - draft.startTimeMillis).coerceIn(300_000L, 86_400_000L),
                    ), local)
                }
            }) { Text(if (local) "Save reminder" else "Open calendar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
