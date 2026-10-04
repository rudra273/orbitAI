package com.example.orbitai.ui.screens

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
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
        title = { Text(stringResource(R.string.enable_message_replies)) },
        text = { Text(stringResource(R.string.android_notification_access_lets_orbit)) },
        confirmButton = {
            TextButton(onClick = {
                settings.enabled = true
                enabled = true
                disclosure = false
                NotificationReplyListener.refreshSettings()
            }) { Text(stringResource(R.string.enable)) }
        },
        dismissButton = { TextButton(onClick = { disclosure = false }) { Text(stringResource(R.string.cancel)) } },
    )

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back_to_tools)) }
            Text(stringResource(R.string.message_replies), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.review_and_reply_to_active))
            val toggleReplies: (Boolean) -> Unit = { value ->
                if (value) disclosure = true else {
                    settings.enabled = false
                    enabled = false
                    closeReply()
                    NotificationReplyListener.refreshSettings()
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(value = enabled, role = Role.Switch, onValueChange = toggleReplies),
            ) {
                Switch(checked = enabled, onCheckedChange = null)
                Text(stringResource(R.string.enable_message_replies_2), Modifier.padding(start = 12.dp))
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
            item { Text(stringResource(R.string.choose_apps), style = MaterialTheme.typography.titleMedium) }
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
            Text(stringResource(R.string.only_notifications_with_a_usable))
            status?.let { Text(it) }
        }
        if (enabled && access) {
            if (notifications.isEmpty()) item { Text(stringResource(R.string.no_reply_ready_messages_select)) }
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
                        }) { Text(stringResource(R.string.review_reply)) }
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
                    if (!current) Text(stringResource(R.string.this_notification_changed_or_disappeared))
                    OutlinedTextField(instruction, { instruction = it.take(1000) }, enabled = current && !loading,
                        label = { Text(stringResource(R.string.what_should_the_reply_say)) }, modifier = Modifier.fillMaxWidth())
                    TextButton(enabled = current && !loading && instruction.isNotBlank(), onClick = {
                        error = null
                        viewModel.draftNotificationReply(notification, instruction)
                    }) { Text(if (loading) "Drafting on device…" else "Draft with local AI") }
                    OutlinedTextField(reply, { reply = it.take(4000) }, enabled = current && !loading,
                        label = { Text(stringResource(R.string.reply_to_send)) }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.check_the_conversation_and_wording))
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
                }) { Text(stringResource(R.string.send_reply)) }
            },
            dismissButton = { TextButton(onClick = { closeReply() }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
