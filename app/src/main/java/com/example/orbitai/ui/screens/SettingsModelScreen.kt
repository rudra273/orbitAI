package com.example.orbitai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.orbitai.data.AVAILABLE_EMBEDDING_MODELS
import com.example.orbitai.data.AVAILABLE_MODELS
import com.example.orbitai.data.DownloadProgress
import com.example.orbitai.data.DownloadStatus
import com.example.orbitai.data.EmbeddingModelConfig
import com.example.orbitai.data.LlmModel
import com.example.orbitai.data.TokenStore
import com.example.orbitai.ui.theme.Destructive
import com.example.orbitai.ui.theme.GlassWhite4
import com.example.orbitai.ui.theme.GlassWhite8
import com.example.orbitai.ui.theme.TextMuted
import com.example.orbitai.ui.theme.TextPrimary
import com.example.orbitai.ui.theme.VioletBright
import com.example.orbitai.ui.theme.VioletCore
import com.example.orbitai.viewmodel.DownloadViewModel

private val HfAccent = Color(0xFF8B5CF6)
private val RagAccent = Color(0xFFFBBF24)
private val CloudAccent = Color(0xFF60A5FA)

@Composable
fun ModelSettingsScreen(
    downloadViewModel: DownloadViewModel,
    tokenStore: TokenStore,
    onBack: () -> Unit,
) {
    val progressMap by downloadViewModel.progress.collectAsState()
    val embeddingProgressMap by downloadViewModel.embeddingProgress.collectAsState()

    SettingsSubScreen(
        title = "Model",
        icon = Icons.Default.Memory,
        accent = VioletCore,
        onBack = onBack,
    ) {
        SettingsDescription("Model controls grouped in compact dropdown sections.")

        // 1) HuggingFace + local model downloads
        DropdownSection(
            title = "HuggingFace Config",
            subtitle = "Token + on-device model downloads",
            accent = HfAccent,
            initiallyExpanded = true,
        ) {
            HuggingFaceTokenCompact(tokenStore)
            Spacer(Modifier.height(10.dp))
            AVAILABLE_MODELS.forEach { model ->
                CompactModelDownloadRow(
                    model = model,
                    progress = progressMap[model.id],
                    onDownload = { downloadViewModel.startDownload(model) },
                    onCancel = { downloadViewModel.cancelDownload(model) },
                    onDelete = { downloadViewModel.deleteModel(model) },
                )
                Spacer(Modifier.height(6.dp))
            }
            CompactCustomModelRow(
                onDownload = { url, fileName ->
                    val normalized = normalizeModelFileName(fileName)
                    val custom = LlmModel(
                        id = "custom-${System.currentTimeMillis()}",
                        displayName = normalized.removeSuffix(".litertlm").removeSuffix(".task"),
                        fileName = normalized,
                        description = "Custom model",
                        paramCount = "?",
                        format = inferModelFormat(normalized),
                    )
                    downloadViewModel.startDownload(custom, url)
                },
            )
        }

        // 2) Semantic model download
        DropdownSection(
            title = "Semantic Model Download",
            subtitle = "Needed for semantic RAG search in Spaces",
            accent = RagAccent,
        ) {
            AVAILABLE_EMBEDDING_MODELS.forEach { model ->
                CompactEmbeddingDownloadRow(
                    model = model,
                    progress = embeddingProgressMap[model.id],
                    onDownload = { downloadViewModel.startEmbeddingDownload(model) },
                    onCancel = { downloadViewModel.cancelEmbeddingDownload(model) },
                    onDelete = { downloadViewModel.deleteEmbeddingModel(model) },
                )
            }
        }

        // 3) Cloud API
        DropdownSection(
            title = "Cloud API",
            subtitle = "Gemini now, GPT template coming soon",
            accent = CloudAccent,
        ) {
            GeminiCompactConfig(tokenStore)
            Spacer(Modifier.height(8.dp))
            GptComingSoonTemplate()
        }
    }
}

@Composable
private fun DropdownSection(
    title: String,
    subtitle: String,
    accent: Color,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    GlassCard(accent = accent) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GlassWhite8),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) { content() }
            }
        }
    }
}

@Composable
private fun HuggingFaceTokenCompact(tokenStore: TokenStore) {
    var token by remember { mutableStateOf(tokenStore.huggingFaceToken) }
    var show by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(tokenStore.hasToken()) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            value = token,
            onValueChange = { 
                token = it
                saved = false
            },
            modifier = Modifier.weight(1f),
            placeholder = { Text("hf_xxx", color = TextMuted, style = MaterialTheme.typography.bodySmall) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GlassWhite8,
                unfocusedContainerColor = GlassWhite8,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            singleLine = true,
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { show = !show }) {
                    Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextMuted)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        )
        CompactActionButton(
            label = if (saved) "Saved" else "Save",
            enabled = token.isNotBlank() && !saved
        ) {
            tokenStore.huggingFaceToken = token
            saved = true
        }
        CompactActionButton("Clear", accent = Destructive) {
            token = ""
            tokenStore.huggingFaceToken = ""
            saved = false
        }
    }
}

@Composable
private fun CompactModelDownloadRow(
    model: LlmModel,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val status = progress?.status ?: DownloadStatus.IDLE
    val isDownloaded = status == DownloadStatus.COMPLETED
    val isActive = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite8)
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.displayName, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Text("${model.paramCount} · ${model.description}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                when {
                    isDownloaded -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                    isActive -> Text("${progress?.progressPercent ?: 0}%", style = MaterialTheme.typography.labelSmall, color = VioletBright)
                    else -> Icon(Icons.Default.CloudDownload, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    isDownloaded -> CompactActionButton("Delete", accent = Destructive, onClick = onDelete)
                    isActive -> CompactActionButton("Cancel", onClick = onCancel)
                    else -> CompactActionButton("Download", onClick = onDownload)
                }
                if (isActive && progress != null && progress.totalBytes > 0) {
                    Text(
                        "${formatBytes(progress.bytesDownloaded)} / ${formatBytes(progress.totalBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }

            if (status == DownloadStatus.FAILED && progress?.error != null) {
                Text(progress.error, color = Destructive, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CompactCustomModelRow(onDownload: (url: String, fileName: String) -> Unit) {
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite8)
            .padding(10.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Text("Custom model URL", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextMuted)
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://...", style = MaterialTheme.typography.bodySmall, color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GlassWhite4,
                            unfocusedContainerColor = GlassWhite4,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                    TextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("mymodel.task", style = MaterialTheme.typography.bodySmall, color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GlassWhite4,
                            unfocusedContainerColor = GlassWhite4,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
                    )
                    CompactActionButton("Download custom", enabled = url.isNotBlank() && fileName.isNotBlank()) {
                        onDownload(url.trim(), fileName.trim())
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactEmbeddingDownloadRow(
    model: EmbeddingModelConfig,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val status = progress?.status ?: DownloadStatus.IDLE
    val isDownloaded = status == DownloadStatus.COMPLETED
    val isActive = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite8)
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.displayName, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Text(model.description, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                when {
                    isDownloaded -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                    isActive -> Text("${progress?.progressPercent ?: 0}%", style = MaterialTheme.typography.labelSmall, color = RagAccent)
                    else -> Icon(Icons.Default.SettingsEthernet, null, tint = RagAccent, modifier = Modifier.size(16.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    isDownloaded -> CompactActionButton("Delete", accent = Destructive, onClick = onDelete)
                    isActive -> CompactActionButton("Cancel", onClick = onCancel)
                    else -> CompactActionButton("Download", accent = RagAccent, onClick = onDownload)
                }
            }

            if (status == DownloadStatus.FAILED && progress?.error != null) {
                Spacer(Modifier.height(4.dp))
                Text(progress.error, color = Destructive, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun GeminiCompactConfig(tokenStore: TokenStore) {
    var modelName by remember { mutableStateOf(tokenStore.geminiModelName) }
    var apiKey by remember { mutableStateOf(tokenStore.geminiApiKey) }
    var show by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(tokenStore.hasGeminiConfig()) }

    var expanded by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite8)
            .padding(10.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Gemini", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = CloudAccent)
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = modelName,
                        onValueChange = { 
                            modelName = it.lowercase()
                            saved = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("gemini-2.0-flash", style = MaterialTheme.typography.bodySmall, color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GlassWhite4,
                            unfocusedContainerColor = GlassWhite4,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        singleLine = true,
                    )
                    TextField(
                        value = apiKey,
                        onValueChange = { 
                            apiKey = it
                            saved = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("api key", style = MaterialTheme.typography.bodySmall, color = TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GlassWhite4,
                            unfocusedContainerColor = GlassWhite4,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Key, null, tint = TextMuted, modifier = Modifier.size(15.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { show = !show }) {
                                Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextMuted)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactActionButton(
                            label = if (saved) "Saved" else "Save",
                            accent = CloudAccent,
                            enabled = modelName.isNotBlank() && apiKey.isNotBlank() && !saved
                        ) {
                            tokenStore.geminiModelName = modelName
                            tokenStore.geminiApiKey = apiKey
                            saved = true
                        }
                        CompactActionButton("Clear", accent = Destructive) {
                            modelName = ""
                            apiKey = ""
                            tokenStore.geminiModelName = ""
                            tokenStore.geminiApiKey = ""
                            saved = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GptComingSoonTemplate() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite8)
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("GPT API", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
            Text("Template ready • Coming soon", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            TextField(
                value = "model name",
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    disabledContainerColor = GlassWhite4,
                    disabledIndicatorColor = Color.Transparent,
                    disabledTextColor = TextMuted,
                ),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                singleLine = true,
            )
            TextField(
                value = "api key",
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    disabledContainerColor = GlassWhite4,
                    disabledIndicatorColor = Color.Transparent,
                    disabledTextColor = TextMuted,
                ),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun CompactActionButton(
    label: String,
    accent: Color = VioletCore,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) accent.copy(alpha = 0.2f) else GlassWhite4)
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) accent else TextMuted,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
