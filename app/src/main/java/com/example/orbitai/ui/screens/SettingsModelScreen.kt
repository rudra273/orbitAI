package com.example.orbitai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.*
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.DownloadViewModel

@Composable
fun ModelSettingsScreen(
    downloadViewModel: DownloadViewModel,
    onBack: () -> Unit,
) {
    val progressMap by downloadViewModel.progress.collectAsState()

    SettingsSubScreen(
        title = "Model",
        icon = Icons.Default.Memory,
        accent = VioletCore,
        onBack = onBack,
    ) {
        SettingsDescription("Download models to chat on-device. No internet needed after download.")

        AVAILABLE_MODELS.forEach { model ->
            ModelDownloadCard(
                model = model,
                progress = progressMap[model.id],
                onDownload = { url -> downloadViewModel.startDownload(model, url) },
                onCancel = { downloadViewModel.cancelDownload(model) },
                onDelete = { downloadViewModel.deleteModel(model) },
            )
        }
    }
}

@Composable
private fun ModelDownloadCard(
    model: LlmModel,
    progress: DownloadProgress?,
    onDownload: (customUrl: String?) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val status = progress?.status ?: DownloadStatus.IDLE
    val isDownloaded = status == DownloadStatus.COMPLETED
    val isActive = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED
    var showUrl by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf(MODEL_DOWNLOAD_URLS[model.id] ?: "") }

    val accent = if (isDownloaded) Color(0xFF34D399) else VioletCore

    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    model.paramCount,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GlassWhite8)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    model.format.badgeLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            when {
                isDownloaded -> Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(22.dp),
                )

                isActive -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = VioletBright,
                    strokeWidth = 2.dp,
                )

                else -> Icon(
                    Icons.Default.CloudDownload,
                    null,
                    tint = TextMuted.copy(0.5f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        if (isActive && (progress?.totalBytes ?: 0L) > 0L) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress!!.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                color = VioletBright,
                trackColor = GlassWhite8,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${progress!!.progressPercent}% · ${formatBytes(progress.bytesDownloaded)} / ${formatBytes(progress.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Text(
                    "Downloading…",
                    style = MaterialTheme.typography.labelSmall,
                    color = VioletBright,
                )
            }
        }

        if (status == DownloadStatus.FAILED && progress?.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                progress.error,
                style = MaterialTheme.typography.bodySmall,
                color = Destructive,
                lineHeight = 16.sp,
            )
        }

        AnimatedVisibility(
            visible = showUrl && !isDownloaded && !isActive,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassWhite4),
                    placeholder = {
                        Text(
                            "https://.../model.task",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = TextMuted,
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = VioletBright,
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                isDownloaded -> OrbitDestructiveButton(label = "Delete", onClick = onDelete)
                isActive -> Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassWhite8)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCancel,
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OrbitGradients.primaryButton)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onDownload(customUrl.takeIf { showUrl && it.isNotBlank() }) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Default.Download,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                "Download",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (showUrl) VioletFrost else GlassWhite8)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { showUrl = !showUrl },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (showUrl) Icons.Default.LinkOff else Icons.Default.Link,
                            contentDescription = "Custom URL",
                            tint = if (showUrl) VioletBright else TextMuted,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}
