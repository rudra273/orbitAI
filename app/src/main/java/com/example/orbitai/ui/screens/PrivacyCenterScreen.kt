package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.orbitai.core.model.formatModelSize
import com.example.orbitai.feature.privacy.NetworkLog
import com.example.orbitai.feature.privacy.PrivacyCenter
import com.example.orbitai.feature.privacy.StorageReport
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private class ClearAction(val title: String, val text: String, val run: suspend () -> Unit)

private class PermissionRow(val name: String, val why: String, val granted: Boolean)

/** What Orbit stores, what it sent over the network, which permissions it uses, and one-tap clearing (f057). */
@Composable
fun PrivacyCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val center = remember { PrivacyCenter(context) }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    var report by remember { mutableStateOf<StorageReport?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var network by remember { mutableStateOf(NetworkLog.shared?.events().orEmpty()) }
    var confirm by remember { mutableStateOf<ClearAction?>(null) }
    var confirmWipe by remember { mutableStateOf(false) }
    var wipeModels by remember { mutableStateOf(false) }

    LaunchedEffect(refresh) {
        report = center.report()
        network = NetworkLog.shared?.events().orEmpty()
    }

    val permissions = remember(refresh) {
        fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        listOf(
            PermissionRow("Microphone", "Talk to the Orbit Bubble. Audio is turned into text on the phone.", granted(Manifest.permission.RECORD_AUDIO)),
            PermissionRow("Contacts", "Find a contact by name for WhatsApp, SMS and calls.", granted(Manifest.permission.READ_CONTACTS)),
            PermissionRow("Phone calls", "Place a call after you confirm.", granted(Manifest.permission.CALL_PHONE)),
            PermissionRow("Notifications", "Show reminders.", granted(Manifest.permission.POST_NOTIFICATIONS)),
            PermissionRow("Draw over other apps", "Show the Orbit Bubble above other apps.", Settings.canDrawOverlays(context)),
            PermissionRow(
                "Notification access",
                "Suggest message replies, only for apps you pick.",
                context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context),
            ),
            PermissionRow(
                "Exact alarms",
                "Fire reminders on time.",
                context.getSystemService(android.app.AlarmManager::class.java)?.canScheduleExactAlarms() == true,
            ),
        )
    }

    Scaffold(
        containerColor = com.example.orbitai.ui.theme.OrbitTheme.colors.background,
        topBar = {
            com.example.orbitai.ui.components.OrbitSubScreenTopBar(
                title = stringResource(R.string.privacy_center),
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Everything Orbit keeps lives on this phone. Review storage, recent network requests, and permissions below.",
                    style = com.example.orbitai.ui.theme.OrbitTheme.typography.subheadline,
                    color = com.example.orbitai.ui.theme.OrbitTheme.colors.labelSecondary,
                )
            }

            item { com.example.orbitai.ui.components.OrbitSectionHeader(stringResource(R.string.stored_on_this_phone)) }
            item {
                val r = report
                if (r == null) {
                    com.example.orbitai.ui.components.OrbitGroupedCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            com.example.orbitai.ui.components.OrbitSkeleton(Modifier.fillMaxWidth(0.75f), height = 16.dp)
                            com.example.orbitai.ui.components.OrbitSkeleton(Modifier.fillMaxWidth(0.55f), height = 16.dp)
                            com.example.orbitai.ui.components.OrbitSkeleton(Modifier.fillMaxWidth(0.85f), height = 16.dp)
                        }
                    }
                } else {
                    com.example.orbitai.ui.components.OrbitGroupedCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StorageRow("Chats", "${r.chats}") {
                                confirm = ClearAction("Delete all chats?", "Every chat and message is removed.") { center.clearChats() }
                            }
                            StorageRow("Memories", "${r.memories}") {
                                confirm = ClearAction("Delete all memories?", "Facts Orbit remembered about you are removed.") { center.clearMemories() }
                            }
                            StorageRow("Spaces", "${r.spaces} Spaces, ${r.documents} documents") {
                                confirm = ClearAction("Delete all Spaces?", "Spaces and their indexed documents are removed. Your original files are not touched.") { center.clearSpaces() }
                            }
                            StorageRow("Downloaded models", formatModelSize(r.modelBytes)) {
                                confirm = ClearAction("Delete downloaded models?", "You will need to download a model again to chat.") { center.deleteModels() }
                            }
                            StorageRow("Temporary files", formatModelSize(r.cacheBytes)) {
                                confirm = ClearAction("Clear temporary files?", "Cached exports and previews are removed.") { center.clearCache() }
                            }
                            Text(
                                "Database on disk: ${formatModelSize(r.databaseBytes)} (chats, memories, Spaces, reminders: ${r.reminders}).",
                                style = com.example.orbitai.ui.theme.OrbitTheme.typography.caption1,
                                color = com.example.orbitai.ui.theme.OrbitTheme.colors.labelTertiary,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            item { com.example.orbitai.ui.components.OrbitSectionHeader(stringResource(R.string.network_activity_last_30_days)) }
            item {
                Text(
                    "Only the website, the reason and the time are recorded here, on this phone. Never what you typed, keys or addresses with details.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (network.isEmpty()) item { Text(stringResource(R.string.nothing_yet_chats_with_on), style = MaterialTheme.typography.bodyMedium) }
            items(network.take(50)) { e ->
                Column {
                    Text("${e.purpose} · ${e.host}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(e.timeMs)) +
                            if (e.bytes >= 0) " · ${formatModelSize(e.bytes)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (network.isNotEmpty()) item {
                TextButton(onClick = { NetworkLog.shared?.clear(); refresh++ }) { Text(stringResource(R.string.clear_network_log)) }
            }

            item { Text(stringResource(R.string.permissions), style = MaterialTheme.typography.labelSmall) }
            items(permissions) { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name + if (p.granted) " · on" else " · off", style = MaterialTheme.typography.titleSmall)
                        Text(p.why, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { context.startActivity(PrivacyCenter.appSettingsIntent(context)) }) { Text(stringResource(R.string.settings)) }
                }
            }

            item { HorizontalDivider() }
            item {
                Text(stringResource(R.string.delete_everything), style = MaterialTheme.typography.labelSmall)
                Text(
                    "Removes chats, memories, Spaces, reminders, settings and saved files, and returns Orbit to how it was when you first installed it.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(wipeModels, { wipeModels = it })
                    Text(stringResource(R.string.also_delete_downloaded_models))
                }
                Button(
                    onClick = { confirmWipe = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.delete_everything_2)) }
            }
        }
    }

    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(action.title) },
            text = { Text(action.text) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch { action.run(); refresh++ }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(stringResource(R.string.delete_everything_3)) },
            text = { Text(stringResource(R.string.this_can_t_be_undone_2)) },
            confirmButton = { TextButton(onClick = { scope.launch { center.wipeEverything(wipeModels) } }) { Text(stringResource(R.string.delete_everything_2)) } },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun StorageRow(label: String, value: String, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onClear) { Text(stringResource(R.string.clear)) }
    }
}
