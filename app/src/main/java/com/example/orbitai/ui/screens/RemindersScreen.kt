package com.example.orbitai.ui.screens

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.orbitai.viewmodel.ChatUiEvent
import com.example.orbitai.viewmodel.ChatViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RemindersScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val reminders by viewModel.reminders.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val review by viewModel.reminderReview.collectAsState()
    var exact by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), viewModel::onNotificationsPermissionResult)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { if (it == ChatUiEvent.RequestNotificationsPermission) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    }
    fun refresh() {
        exact = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        val channel = context.getSystemService(android.app.NotificationManager::class.java)
            .getNotificationChannel(com.example.orbitai.feature.automation.reminder.ReminderNotificationHelper.CHANNEL_ID)
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled() && channel?.importance != android.app.NotificationManager.IMPORTANCE_NONE
        viewModel.refreshReminders()
    }
    DisposableEffect(owner) {
        refresh()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    review?.takeIf { it.chatId.isBlank() }?.let { ReminderReviewDialog(it, viewModel::dismissReminderReview, viewModel::confirmReminder, state.loadError) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) { Text("Back to Tools") }
            Text("Orbit reminders", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { viewModel.editReminder() }) { Text("New reminder") }
            Text(if (exact) "Precise alarms enabled" else "Timing is approximate until Alarms & reminders access is enabled.")
            TextButton(onClick = {
                try { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }
                catch (_: android.content.ActivityNotFoundException) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            }) { Text("Alarms & reminders settings") }
            Text(if (notifications) "Notifications enabled" else "Notifications are blocked. Enable them to receive reminders.")
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }) { Text("Notification settings") }
        }
        state.loadError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        state.infoMessage?.let { info -> item { Text(info) } }
        if (reminders.isEmpty()) item { Text("No saved reminders. Create one here or ask ‘Remind me at 10 pm to call Mom’.") }
        items(reminders, key = { it.id }) { reminder ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                    if (reminder.description.isNotBlank()) Text(reminder.description)
                    Text(Instant.ofEpochMilli(reminder.triggerAt).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z")))
                    Text(when { reminder.completed -> "Completed"; reminder.delivered -> "Due"; else -> "Scheduled · ${reminder.repeat.lowercase()}" })
                    if (!reminder.completed) {
                        Row {
                            TextButton(onClick = { viewModel.editReminder(reminder) }) { Text("Edit") }
                            TextButton(onClick = { viewModel.snoozeReminder(reminder.id) }) { Text("Snooze 10m") }
                        }
                        TextButton(onClick = { viewModel.completeReminder(reminder.id) }) {
                            Text(if (reminder.repeat == "NONE") "Complete" else "Stop repeating")
                        }
                    }
                    TextButton(onClick = { viewModel.cancelReminder(reminder.id) }) { Text("Delete") }
                }
            }
        }
    }
}
