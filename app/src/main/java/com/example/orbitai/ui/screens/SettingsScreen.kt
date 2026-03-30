package com.example.orbitai.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.TextPrimary
import com.example.orbitai.viewmodel.AppUpdateViewModel
import com.example.orbitai.viewmodel.DownloadViewModel

private val SettingsSurfaceLight = Color(0xFFF9F8F5)
private val SettingsCardLight = Color(0xFFFFFFFF)
private val SettingsCardDark = Color(0xFF1E1E1C)
private val SettingsInkLight = Color(0xFF0D0D0D)
private val SettingsSans = FontFamily.SansSerif
private val SettingsMono = FontFamily.Monospace

private val SettingsSurface: Color
    @Composable get() = if (IsOrbitDarkTheme) SpaceDeep else SettingsSurfaceLight
private val SettingsCard: Color
    @Composable get() = if (IsOrbitDarkTheme) SettingsCardDark else SettingsCardLight
private val SettingsInk: Color
    @Composable get() = if (IsOrbitDarkTheme) TextPrimary else SettingsInkLight

private data class SettingsRow(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: (() -> Unit)? = null,
    val showChevron: Boolean = true,
    val trailing: (@Composable () -> Unit)? = null,
)

@Composable
fun SettingsScreen(
    downloadViewModel: DownloadViewModel,
    appUpdateViewModel: AppUpdateViewModel,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { downloadViewModel.refreshStatus() }
    val context = LocalContext.current
    val updateState by appUpdateViewModel.uiState.collectAsState()
    val versionDescription = remember(
        updateState.installedVersion,
        updateState.latestVersion,
        updateState.isChecking,
        updateState.isUpdateAvailable,
        updateState.errorMessage,
    ) {
        when {
            updateState.isChecking -> "Installed ${updateState.installedVersion.ifBlank { "Unknown" }} · Checking for updates"
            updateState.isUpdateAvailable && !updateState.latestVersion.isNullOrBlank() ->
                "Installed ${updateState.installedVersion.ifBlank { "Unknown" }} · New ${updateState.latestVersion} available"
            !updateState.errorMessage.isNullOrBlank() -> "Installed ${updateState.installedVersion.ifBlank { "Unknown" }} · Tap to retry"
            else -> "Installed ${updateState.installedVersion.ifBlank { "Unknown" }} · Up to date"
        }
    }
    val versionActionLabel = remember(
        updateState.isChecking,
        updateState.isUpdateAvailable,
        updateState.errorMessage,
    ) {
        when {
            updateState.isChecking -> "Checking"
            updateState.isUpdateAvailable -> "Update"
            !updateState.errorMessage.isNullOrBlank() -> "Retry"
            else -> "Current"
        }
    }
    val versionActionUrl = updateState.downloadUrl ?: updateState.releaseUrl
    val openReleasePage = remember(versionActionUrl) {
        {
            if (!versionActionUrl.isNullOrBlank()) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(versionActionUrl))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                appUpdateViewModel.checkForUpdates()
            }
        }
    }

    val aiRows = remember {
        listOf(
            SettingsRow(
                icon = Icons.Default.DeveloperBoard,
                title = "Models",
                description = "Download, configure & manage",
            ),
            SettingsRow(
                icon = Icons.Default.Psychology,
                title = "Memory",
                description = "Stored memories and controls",
            ),
            SettingsRow(
                icon = Icons.Default.Build,
                title = "Tools",
                description = "Tool availability and support",
            ),
        )
    }

    val aboutRows = listOf(
            SettingsRow(
                icon = Icons.Default.Tag,
                title = "Version",
                description = versionDescription,
                showChevron = false,
                trailing = {
                    StatusChip(
                        label = versionActionLabel,
                        emphasized = updateState.isUpdateAvailable,
                    )
                },
            ),
            SettingsRow(
                icon = Icons.Default.Gavel,
                title = "Privacy policy",
                description = "Read our data and privacy terms",
                onClick = { },
            ),
        )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsSurface),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 0.dp, top = 18.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Settings",
                        style = androidx.compose.material3.MaterialTheme.typography.displaySmall.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = SettingsInk,
                        fontFamily = SettingsSans,
                    )
                    Text(
                        text = "OrbitAI · On-device LLM",
                        color = SettingsInk.copy(alpha = 0.35f),
                        fontFamily = SettingsMono,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        item { SettingsSectionHeader("AI") }
        item {
            GroupedCard {
                SettingsGroupRow(
                    row = aiRows[0],
                    onClick = { onNavigate("settings/model") },
                )
                HairlineDivider()
                SettingsGroupRow(
                    row = aiRows[1],
                    onClick = { onNavigate("settings/memory") },
                )
                HairlineDivider()
                SettingsGroupRow(
                    row = aiRows[2],
                    onClick = { onNavigate("settings/tools") },
                )
            }
        }

        item { SettingsSectionHeader("SYSTEM") }
        item {
            GroupedCard {
                SettingsGroupRow(
                    row = SettingsRow(
                        icon = Icons.Default.ChatBubble,
                        title = "Orbit Bubble",
                        description = "Floating assistant behavior",
                    ),
                    onClick = { onNavigate("settings/orbit_bubble") },
                )
                HairlineDivider()
                SettingsGroupRow(
                    row = SettingsRow(
                        icon = Icons.Default.NightsStay,
                        title = "Dark mode",
                        description = "App theme preference",
                        showChevron = false,
                        trailing = {
                            InkToggle(
                                checked = isDarkTheme,
                                onCheckedChange = onThemeChanged,
                            )
                        },
                    ),
                    onClick = { onThemeChanged(!isDarkTheme) },
                )
            }
        }

        item { SettingsSectionHeader("ABOUT") }
        item {
            GroupedCard {
                SettingsGroupRow(
                    row = aboutRows[0],
                    onClick = {
                        if (updateState.isUpdateAvailable && !versionActionUrl.isNullOrBlank()) {
                            openReleasePage()
                        } else {
                            appUpdateViewModel.checkForUpdates()
                        }
                    },
                )
                HairlineDivider()
                SettingsGroupRow(row = aboutRows[1], onClick = aboutRows[1].onClick)
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "OrbitAI · On-device LLM",
                modifier = Modifier.fillMaxWidth(),
                color = SettingsInk.copy(alpha = 0.20f),
                fontFamily = SettingsMono,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    emphasized: Boolean,
) {
    val backgroundColor = if (emphasized) Color(0xFF1F6B45) else SettingsInk.copy(alpha = 0.08f)
    val textColor = if (emphasized) Color.White else SettingsInk.copy(alpha = 0.70f)

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontFamily = SettingsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 0.3.sp,
        )
    }
}

@Composable
private fun GroupedCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SettingsCard)
            .border(
                width = 1.dp,
                color = SettingsInk.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 10.dp),
        content = content,
    )
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        color = SettingsInk.copy(alpha = 0.30f),
        fontFamily = SettingsMono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

@Composable
private fun SettingsGroupRow(
    row: SettingsRow,
    onClick: (() -> Unit)?,
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(SettingsInk.copy(alpha = 0.06f))
                .border(
                    width = 1.dp,
                    color = SettingsInk.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(9.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                tint = SettingsInk.copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp),
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = row.title,
                color = SettingsInk,
                fontFamily = SettingsSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Text(
                text = row.description,
                color = SettingsInk.copy(alpha = 0.38f),
                fontFamily = SettingsSans,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        when {
            row.trailing != null -> row.trailing()
            row.showChevron -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = SettingsInk.copy(alpha = 0.45f),
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun InkToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val checkedTrack = if (IsOrbitDarkTheme) Color(0xFFE8E6E1) else SettingsInk
    val uncheckedTrack = if (IsOrbitDarkTheme) Color(0xFF2A2A28) else SettingsInk.copy(alpha = 0.18f)
    val knobColor = if (checked && IsOrbitDarkTheme) SpaceDeep else Color.White

    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(CircleShape)
            .background(if (checked) checkedTrack else uncheckedTrack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(horizontal = 3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(knobColor),
        )
    }
}

@Composable
private fun HairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(SettingsInk.copy(alpha = 0.08f)),
    )
}
