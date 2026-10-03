package com.example.orbitai.ui.screens

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.orbitai.feature.automation.replies.NotificationReplyListener
import com.example.orbitai.feature.automation.replies.NotificationReplySettings
import com.example.orbitai.feature.automation.replies.ReplyNotification
import com.example.orbitai.viewmodel.ChatViewModel

@Composable
fun NotificationRepliesScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val settings = remember { NotificationReplySettings(context) }
    val component = remember { ComponentName(context, NotificationReplyListener::class.java) }
    fun hasAccess() = context.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(component)
    var access by remember { mutableStateOf(hasAccess()) }
    var enabled by remember { mutableStateOf(settings.enabled) }
    var packages by remember { mutableStateOf(settings.packages) }
    var disclosure by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ReplyNotification?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    val notifications by NotificationReplyListener.notifications.collectAsState()
    val draft by viewModel.notificationReplyDraft.collectAsState()

    fun closeReply() {
        viewModel.clearNotificationReplyDraft()
        selected = null
    }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                access = hasAccess()
                if (access && enabled) NotificationListenerService.requestRebind(component)
                if (!access) {
                    NotificationReplyListener.refreshSettings()
                    closeReply()
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                closeReply()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            viewModel.clearNotificationReplyDraft()
        }
    }

    if (disclosure) AlertDialog(
        onDismissRequest = { disclosure = false },
        title = { Text("Enable message replies?") },
        text = { Text("Android notification access lets Orbit receive notifications. Orbit only keeps reply-capable previews from the apps you select, in memory while those notifications are active. Drafting uses a downloaded local model; previews aren't saved to chat or sent to cloud AI. A reply is handed to the messaging app only when you tap Send reply. You can turn this off and revoke access at any time.") },
        confirmButton = {
            TextButton(onClick = {
                settings.enabled = true
                enabled = true
                disclosure = false
                NotificationReplyListener.refreshSettings()
            }) { Text("Enable") }
        },
        dismissButton = { TextButton(onClick = { disclosure = false }) { Text("Cancel") } },
    )

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) { Text("Back to Tools") }
            Text("Message replies", style = MaterialTheme.typography.headlineSmall)
            Text("Review and reply to active notifications from supported apps.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = enabled, onCheckedChange = { value ->
                    if (value) disclosure = true else {
                        settings.enabled = false
                        enabled = false
                        closeReply()
                        NotificationReplyListener.refreshSettings()
                    }
                })
                Text("Enable message replies", Modifier.padding(start = 12.dp))
            }
            Text("Notification access: ${if (access) "granted" else "not granted"}")
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (_: android.content.ActivityNotFoundException) {
                    status = "Open Android Settings > Special app access > Notification access."
                }
            }) { Text(if (access) "Manage Android access" else "Grant Android access") }
        }
        if (enabled) {
            item { Text("Choose apps", style = MaterialTheme.typography.titleMedium) }
            items(NotificationReplySettings.apps.entries.toList(), key = { it.key }) { app ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = app.key in packages, onCheckedChange = { checked ->
                        packages = if (checked) packages + app.key else packages - app.key
                        settings.packages = packages
                        closeReply()
                        NotificationReplyListener.refreshSettings()
                    })
                    Text(app.value)
                }
            }
        }
        item {
            Text("Only notifications with a usable text reply action appear. Hidden content, some group notifications, and expired messages may be unavailable.")
            status?.let { Text(it) }
        }
        if (enabled && access) {
            if (notifications.isEmpty()) item { Text("No reply-ready messages. Select an app and wait for a message notification.") }
            items(notifications, key = { it.key }) { notification ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(NotificationReplySettings.apps[notification.packageName].orEmpty(), style = MaterialTheme.typography.labelMedium)
                        Text(notification.conversation, style = MaterialTheme.typography.titleMedium)
                        Text(notification.message)
                        TextButton(onClick = {
                            viewModel.clearNotificationReplyDraft()
                            status = null
                            selected = notification
                        }) { Text("Review reply") }
                    }
                }
            }
        }
    }

    selected?.let { notification ->
        var instruction by remember(notification) { mutableStateOf("") }
        var reply by remember(notification) { mutableStateOf("") }
        var error by remember(notification) { mutableStateOf<String?>(null) }
        var sent by remember(notification) { mutableStateOf(false) }
        val current = enabled && access && notifications.any { it == notification }
        val matchingDraft = draft.key == notification.key && draft.revision == notification.revision
        val loading = matchingDraft && draft.loading
        LaunchedEffect(draft) {
            if (matchingDraft && !draft.loading && draft.text.isNotBlank()) reply = draft.text
        }
        LaunchedEffect(current) {
            if (!current) viewModel.clearNotificationReplyDraft()
        }
        AlertDialog(
            onDismissRequest = { closeReply() },
            title = { Text("Reply to ${notification.conversation}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(NotificationReplySettings.apps[notification.packageName].orEmpty())
                    Text(notification.message)
                    if (!current) Text("This notification changed or disappeared. Close this preview and review the latest message.")
                    OutlinedTextField(instruction, { instruction = it.take(1000) }, enabled = current && !loading,
                        label = { Text("What should the reply say?") }, modifier = Modifier.fillMaxWidth())
                    TextButton(enabled = current && !loading && instruction.isNotBlank(), onClick = {
                        error = null
                        viewModel.draftNotificationReply(notification, instruction)
                    }) { Text(if (loading) "Drafting on device…" else "Draft with local AI") }
                    OutlinedTextField(reply, { reply = it.take(4000) }, enabled = current && !loading,
                        label = { Text("Reply to send") }, modifier = Modifier.fillMaxWidth())
                    Text("Check the conversation and wording. Sending uses the messaging app and may require a network connection.")
                    (error ?: draft.error.takeIf { matchingDraft })?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(enabled = current && !loading && !sent && reply.isNotBlank(), onClick = {
                    sent = true
                    val failure = NotificationReplyListener.sendReply(notification, reply)
                    if (failure == null) {
                        status = "Reply handed to ${NotificationReplySettings.apps[notification.packageName]}. Delivery is handled by that app."
                        closeReply()
                    } else {
                        sent = false
                        error = failure
                    }
                }) { Text("Send reply") }
            },
            dismissButton = { TextButton(onClick = { closeReply() }) { Text("Cancel") } },
        )
    }
}
