package com.example.orbitai.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.orbitai.R
import com.example.orbitai.ui.components.OrbitGroupedCard
import com.example.orbitai.ui.components.OrbitInsetDivider
import com.example.orbitai.ui.components.OrbitListRow
import com.example.orbitai.ui.components.OrbitSectionHeader
import com.example.orbitai.ui.components.OrbitStatusBadge
import com.example.orbitai.ui.components.OrbitTopLevelHeader
import com.example.orbitai.ui.theme.OrbitTheme
import com.example.orbitai.viewmodel.AppUpdateViewModel
import com.example.orbitai.viewmodel.DownloadViewModel

@Composable
fun SettingsScreen(
    downloadViewModel: DownloadViewModel,
    appUpdateViewModel: AppUpdateViewModel,
    appearance: com.example.orbitai.core.common.Appearance,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    var showWhatsNew by rememberSaveable { mutableStateOf(false) }
    if (showWhatsNew) com.example.orbitai.feature.update.WhatsNewDialog { showWhatsNew = false }
    LaunchedEffect(Unit) { downloadViewModel.refreshStatus() }

    val context = LocalContext.current
    val updateState by appUpdateViewModel.uiState.collectAsState()
    val colors = OrbitTheme.colors
    val spacing = OrbitTheme.spacing

    val versionDescription = remember(
        updateState.installedVersion,
        updateState.latestVersion,
        updateState.isChecking,
        updateState.isDownloading,
        updateState.downloadProgress,
        updateState.isUpdateAvailable,
        updateState.isReadyToInstall,
        updateState.errorMessage,
        updateState.installMessage,
    ) {
        val installed = updateState.installedVersion.ifBlank { "Unknown" }
        when {
            updateState.isDownloading -> "Installed $installed · Downloading update"
            updateState.isChecking -> "Installed $installed · Checking for updates"
            !updateState.installMessage.isNullOrBlank() -> updateState.installMessage.orEmpty()
            updateState.isUpdateAvailable && !updateState.latestVersion.isNullOrBlank() ->
                "Installed $installed · New ${updateState.latestVersion} available"
            !updateState.errorMessage.isNullOrBlank() -> "Installed $installed · Tap to retry"
            else -> "Installed $installed · Up to date"
        }
    }
    val versionActionLabel = remember(
        updateState.isChecking,
        updateState.isDownloading,
        updateState.downloadProgress,
        updateState.isUpdateAvailable,
        updateState.isReadyToInstall,
        updateState.errorMessage,
    ) {
        when {
            updateState.isDownloading -> "${updateState.downloadProgress}%"
            updateState.isChecking -> "Checking"
            updateState.isUpdateAvailable -> "Update"
            !updateState.errorMessage.isNullOrBlank() -> "Retry"
            else -> "Current"
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            OrbitTopLevelHeader(
                title = stringResource(R.string.settings),
                subtitle = stringResource(R.string.orbitai_on_device_llm),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(padding),
            contentPadding = PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.xs,
                bottom = spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            item { OrbitSectionHeader(stringResource(R.string.section_ai)) }
            item {
                OrbitGroupedCard {
                    OrbitListRow(
                        icon = Icons.Default.DeveloperBoard,
                        title = stringResource(R.string.models),
                        subtitle = stringResource(R.string.download_configure_manage),
                        onClick = { onNavigate("settings/model") },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.Psychology,
                        title = stringResource(R.string.memory),
                        subtitle = stringResource(R.string.stored_memories_and_controls),
                        onClick = { onNavigate("settings/memory") },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.Build,
                        title = stringResource(R.string.tools),
                        subtitle = stringResource(R.string.tool_availability_and_support),
                        onClick = { onNavigate("settings/tools") },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.Bookmarks,
                        title = stringResource(R.string.saved_prompts),
                        subtitle = stringResource(R.string.reusable_prompts_for_the_menu),
                        onClick = { onNavigate("settings/saved_prompts") },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.Forum,
                        title = stringResource(R.string.chat),
                        subtitle = stringResource(R.string.smart_titles_and_generation_details),
                        onClick = { onNavigate("settings/chat") },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.RecordVoiceOver,
                        title = stringResource(R.string.voice),
                        subtitle = stringResource(R.string.read_aloud_speech_speed_and),
                        onClick = { onNavigate("settings/voice") },
                    )
                }
            }

            item { OrbitSectionHeader(stringResource(R.string.section_system)) }
            item {
                OrbitGroupedCard {
                    OrbitListRow(
                        icon = Icons.Default.ChatBubble,
                        title = stringResource(R.string.orbit_bubble),
                        subtitle = stringResource(R.string.floating_assistant_behavior),
                        onClick = { onNavigate("settings/orbit_bubble") },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.app_language),
                        subtitle = stringResource(R.string.app_language_description),
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APP_LOCALE_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.ChatBubble,
                        title = stringResource(R.string.assistant_gesture),
                        subtitle = stringResource(R.string.open_orbit_with_the_power),
                        onClick = {
                            val open = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(open) }.onFailure {
                                context.startActivity(
                                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.NightsStay,
                        title = stringResource(R.string.appearance),
                        subtitle = "${appearance.mode.label} · ${(appearance.chatTextScale * 100).toInt()}% text",
                        onClick = { onNavigate("settings/appearance") },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.Backup,
                        title = stringResource(R.string.backup_restore),
                        subtitle = stringResource(R.string.encrypted_file_of_your_chats),
                        onClick = { onNavigate("settings/backup") },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.privacy_center),
                        subtitle = stringResource(R.string.what_is_stored_network_activity),
                        onClick = { onNavigate("settings/privacy_center") },
                    )
                }
            }

            item { OrbitSectionHeader(stringResource(R.string.section_about)) }
            item {
                OrbitGroupedCard {
                    val emphasized = updateState.isUpdateAvailable || updateState.isReadyToInstall
                    OrbitListRow(
                        icon = Icons.Default.Tag,
                        title = stringResource(R.string.version),
                        subtitle = versionDescription,
                        showChevron = false,
                        trailing = {
                            OrbitStatusBadge(
                                label = versionActionLabel,
                                containerColor = if (emphasized) colors.successContainer else colors.surfaceInput,
                                contentColor = if (emphasized) colors.onSuccessContainer else colors.labelSecondary,
                            )
                        },
                        onClick = {
                            if (updateState.isDownloading) return@OrbitListRow
                            if (updateState.isUpdateAvailable) {
                                appUpdateViewModel.openUpdatePage()
                            } else {
                                appUpdateViewModel.checkForUpdates()
                            }
                        },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.Gavel,
                        title = stringResource(R.string.privacy_policy),
                        subtitle = stringResource(R.string.read_our_data_and_privacy),
                        onClick = { onNavigate("settings/privacy") },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.BugReport,
                        title = stringResource(R.string.diagnostics),
                        subtitle = stringResource(R.string.review_and_send_crash_reports),
                        onClick = { onNavigate("settings/diagnostics") },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.onboarding_open),
                        subtitle = stringResource(R.string.onboarding_open_desc),
                        onClick = { onNavigate("onboarding") },
                    )
                    OrbitInsetDivider()
                    OrbitListRow(
                        icon = Icons.Default.NewReleases,
                        title = "What's new in Orbit ${com.example.orbitai.feature.update.WhatsNew.VERSION}.0",
                        subtitle = stringResource(R.string.see_the_headline_features),
                        onClick = { showWhatsNew = true },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(spacing.md))
                Text(
                    text = stringResource(R.string.orbitai_on_device_llm),
                    modifier = Modifier.fillMaxWidth(),
                    style = OrbitTheme.typography.caption1,
                    color = colors.labelTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
