package com.example.orbitai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.*
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.DownloadViewModel

@Composable
fun RagSettingsScreen(
    downloadViewModel: DownloadViewModel,
    onBack: () -> Unit,
) {
    val embeddingProgressMap by downloadViewModel.embeddingProgress.collectAsState()

    SettingsSubScreen(
        title = "Knowledge / RAG",
        icon = Icons.Default.Search,
        accent = Color(0xFFFBBF24),
        onBack = onBack,
    ) {
        SettingsDescription("Download the Gecko embedding model to enable semantic (meaning-based) search in your documents. Without it, keyword search is used.")

        AVAILABLE_EMBEDDING_MODELS.forEach { model ->
            EmbeddingModelDownloadCard(
                model = model,
                progress = embeddingProgressMap[model.id],
                onDownload = { downloadViewModel.startEmbeddingDownload(model) },
                onCancel = { downloadViewModel.cancelEmbeddingDownload(model) },
                onDelete = { downloadViewModel.deleteEmbeddingModel(model) },
            )
        }
    }
}

@Composable
private fun EmbeddingModelDownloadCard(
    model: EmbeddingModelConfig,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val status = progress?.status ?: DownloadStatus.IDLE
    val isDownloaded = status == DownloadStatus.COMPLETED
    val isActive = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED
    val accent = Color(0xFFFBBF24)

    GlassCard(accent = accent) {
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
                    "~8 MB",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(model.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(model.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            when {
                isDownloaded -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(22.dp))
                isActive -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
                else -> Icon(Icons.Default.CloudDownload, null, tint = TextMuted.copy(0.5f), modifier = Modifier.size(22.dp))
            }
        }

        if (isDownloaded) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34D399)))
                Text(
                    "Semantic search active — RAG queries use Gecko embeddings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF34D399).copy(0.8f),
                )
            }
        }

        if (isActive && (progress?.totalBytes ?: 0L) > 0L) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress!!.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = GlassWhite8,
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${progress!!.progressPercent}% · ${formatBytes(progress.bytesDownloaded)} / ${formatBytes(progress.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Text("Downloading…", style = MaterialTheme.typography.labelSmall, color = accent)
            }
        }

        if (status == DownloadStatus.FAILED && progress?.error != null) {
            Spacer(Modifier.height(6.dp))
            Text(progress.error, style = MaterialTheme.typography.bodySmall, color = Destructive, lineHeight = 16.sp)
        }

        Spacer(Modifier.height(12.dp))

        when {
            isDownloaded -> OrbitDestructiveButton("Delete", onDelete)
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
            else -> Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(accent.copy(0.9f), accent.copy(0.7f))))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDownload,
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Text("Download", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
