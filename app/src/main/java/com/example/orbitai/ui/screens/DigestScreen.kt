package com.example.orbitai.ui.screens

import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.orbitai.R
import com.example.orbitai.feature.automation.digest.DigestBuffer
import com.example.orbitai.feature.automation.digest.DigestPrompt
import com.example.orbitai.feature.automation.digest.DigestSettings
import com.example.orbitai.feature.automation.replies.NotificationReplyListener
import com.example.orbitai.feature.textactions.TextActionPrompts
import com.example.orbitai.feature.textactions.TextActionRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Summarises selected apps' recent notifications with the on-device model. Off by default, nothing is stored on disk. */
@Composable
fun DigestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { DigestSettings(context) }
    val runner = remember { TextActionRunner(context.applicationContext) }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    var enabled by remember { mutableStateOf(settings.enabled) }
    var packages by remember { mutableStateOf(settings.packages) }
    var tick by remember { mutableIntStateOf(0) }
    var showDisclosure by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }

    val component = remember { ComponentName(context, NotificationReplyListener::class.java) }
    val hasAccess = remember(tick) { context.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(component) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) tick++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val collected = remember(tick, enabled, packages) { DigestBuffer.shared.size }
    val skipped = remember(tick, enabled, packages) { DigestBuffer.shared.skippedSensitive }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text(stringResource(R.string.digest_enable_title)) },
            text = { Text(stringResource(R.string.digest_disclosure)) },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    settings.enabled = true
                    enabled = true
                    NotificationReplyListener.refreshSettings()
                }) { Text(stringResource(R.string.enable)) }
            },
            dismissButton = { TextButton(onClick = { showDisclosure = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    Scaffold { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(stringResource(R.string.digest_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.digest_intro), style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.digest_switch), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { wanted ->
                        if (wanted) showDisclosure = true else {
                            settings.enabled = false
                            enabled = false
                            DigestBuffer.shared.clear()
                            NotificationReplyListener.refreshSettings()
                            output = ""
                        }
                    })
                }
            }
            if (enabled && !hasAccess) {
                item {
                    Text(stringResource(R.string.digest_needs_access), style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text(stringResource(R.string.digest_open_access)) }
                }
            }
            if (enabled) {
                item { Text(stringResource(R.string.choose_apps), style = MaterialTheme.typography.titleSmall) }
                items(DigestSettings.apps.entries.toList()) { app ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = app.key in packages, onCheckedChange = { checked ->
                            packages = if (checked) packages + app.key else packages - app.key
                            settings.packages = packages
                            NotificationReplyListener.refreshSettings()
                            tick++
                        })
                        Text(app.value)
                    }
                }
                item {
                    Text(
                        stringResource(R.string.digest_collected, collected) +
                            if (skipped > 0) " " + stringResource(R.string.digest_skipped, skipped) else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(stringResource(R.string.digest_memory_note), style = MaterialTheme.typography.bodySmall)
                }
                item {
                    Button(
                        enabled = !running && hasAccess,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val items = DigestBuffer.shared.snapshot()
                            val model = runner.localModel()
                            when {
                                items.isEmpty() -> message = context.getString(R.string.digest_nothing)
                                model == null -> message = context.getString(R.string.digest_needs_model)
                                else -> scope.launch {
                                    running = true
                                    message = null
                                    output = ""
                                    try {
                                        runner.runInstruction(model, DigestPrompt.build(items)).collect { output += it }
                                        output = TextActionPrompts.cleanResult(output)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        message = context.getString(R.string.digest_failed, e.message ?: e.javaClass.simpleName)
                                    }
                                    running = false
                                }
                            }
                        },
                    ) { Text(stringResource(if (running) R.string.digest_working else R.string.digest_make)) }
                }
                message?.let { item { Text(it, style = MaterialTheme.typography.bodyMedium) } }
                if (output.isNotBlank()) {
                    item {
                        Text(output, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Orbit", output))
                            }) { Text(stringResource(R.string.copy)) }
                            TextButton(onClick = { output = "" }) { Text(stringResource(R.string.clear)) }
                        }
                    }
                }
                item {
                    TextButton(onClick = { DigestBuffer.shared.clear(); output = ""; tick++ }) {
                        Text(stringResource(R.string.digest_clear_collected))
                    }
                }
            }
        }
    }
}
