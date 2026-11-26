package com.example.orbitai.ui.screens

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.orbitai.feature.backup.BackupManager
import com.example.orbitai.feature.backup.BackupPreview
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private const val MIN_PASSPHRASE = 8

/** Create an encrypted backup of chats, memories, modes, Spaces and settings, or restore one (f055). */
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { BackupManager(context) }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    var passphrase by remember { mutableStateOf("") }
    var includeKeys by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var staged by remember { mutableStateOf<BackupPreview?>(null) }
    var pendingRestore by remember { mutableStateOf(manager.hasPendingRestore()) }
    var restoreSource by remember { mutableStateOf<Uri?>(null) }

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        message = null
        scope.launch {
            val result = manager.create(uri, passphrase.toCharArray(), includeKeys)
            busy = false
            message = result.fold(
                { "Backup saved: ${it.chats} chats, ${it.memories} memories, ${it.modes} modes, ${it.spaces} Spaces." },
                { "Couldn't create the backup: ${it.message ?: it.javaClass.simpleName}" },
            )
        }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        message = null
        scope.launch {
            val result = manager.stageRestore(uri, passphrase.toCharArray())
            busy = false
            result.onSuccess { staged = it }.onFailure { message = it.message ?: "Couldn't read that backup." }
        }
    }

    Scaffold(
        containerColor = com.example.orbitai.ui.theme.OrbitTheme.colors.background,
        topBar = {
            com.example.orbitai.ui.components.OrbitSubScreenTopBar(
                title = stringResource(R.string.backup_restore),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Orbit keeps everything on this phone, so a new phone or a reinstall starts empty. A backup is one file you keep, " +
                    "locked with a passphrase only you know. Downloaded models aren't included; download them again after restoring.",
                style = com.example.orbitai.ui.theme.OrbitTheme.typography.subheadline,
                color = com.example.orbitai.ui.theme.OrbitTheme.colors.labelSecondary,
            )
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text(stringResource(R.string.passphrase)) },
                supportingText = { Text("At least $MIN_PASSPHRASE characters. If you forget it, the backup can't be opened.") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(value = includeKeys, enabled = !busy, role = Role.Switch) { includeKeys = it },
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.include_api_keys), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.hugging_face_token_and_gemini), style = MaterialTheme.typography.bodySmall)
                }
                Switch(includeKeys, null, enabled = !busy)
            }
            Button(
                enabled = !busy && passphrase.length >= MIN_PASSPHRASE,
                onClick = {
                    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(Date())
                    createLauncher.launch("orbit-$stamp.orbitbackup")
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.create_backup)) }
            OutlinedButton(
                enabled = !busy && passphrase.isNotEmpty(),
                onClick = { openLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.restore_from_backup)) }
            Text(
                "To restore, type the backup's passphrase above, then choose the file. Restoring replaces the chats, memories, modes and settings on this phone.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (busy) Text(stringResource(R.string.working), style = MaterialTheme.typography.bodyMedium)
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (pendingRestore) {
                Text(stringResource(R.string.a_restore_is_ready_and), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { restartApp(context) }) { Text(stringResource(R.string.restart_now)) }
                    TextButton(onClick = { manager.cancelPendingRestore(); pendingRestore = false }) { Text(stringResource(R.string.cancel_restore)) }
                }
            }
        }
    }

    staged?.let { preview ->
        AlertDialog(
            onDismissRequest = { manager.cancelPendingRestore(); staged = null },
            title = { Text(stringResource(R.string.restore_this_backup)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Made ${DateFormat.getDateTimeInstance().format(Date(preview.createdAt))}")
                    Text("${preview.chats} chats · ${preview.memories} memories · ${preview.modes} modes · ${preview.spaces} Spaces")
                    Text(if (preview.includesKeys) "Includes API keys." else "Does not include API keys; yours stay as they are.")
                    Text(stringResource(R.string.this_replaces_what_is_on))
                }
            },
            confirmButton = {
                TextButton(onClick = { staged = null; pendingRestore = true; restartApp(context) }) { Text(stringResource(R.string.replace_and_restart)) }
            },
            dismissButton = { TextButton(onClick = { manager.cancelPendingRestore(); staged = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun restartApp(context: android.content.Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(launch)
    Runtime.getRuntime().exit(0)
}
