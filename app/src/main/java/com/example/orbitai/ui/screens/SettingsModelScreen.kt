package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import com.example.orbitai.core.model.DeviceFit
import com.example.orbitai.core.model.ModelFit
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.core.model.AVAILABLE_MODELS
import com.example.orbitai.core.model.formatModelSize
import com.example.orbitai.core.model.DownloadProgress
import com.example.orbitai.core.model.DownloadStatus
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.MODEL_DOWNLOAD_SPECS
import com.example.orbitai.core.model.AVAILABLE_EMBEDDING_MODELS
import com.example.orbitai.core.model.EmbeddingModelConfig
import com.example.orbitai.core.common.TokenStore
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.TextPrimary
import com.example.orbitai.viewmodel.DownloadViewModel

private val SettingsGreen @Composable get() = com.example.orbitai.ui.theme.OrbitTheme.colors.success
private val SettingsRed @Composable get() = com.example.orbitai.ui.theme.OrbitTheme.colors.destructive
private val SettingsSans = com.example.orbitai.ui.theme.OrbitUiFont
private val SettingsMono = com.example.orbitai.ui.theme.OrbitMonoFont

private val SettingsSurface: Color
    @Composable get() = com.example.orbitai.ui.theme.OrbitTheme.colors.background
private val SettingsCard: Color
    @Composable get() = com.example.orbitai.ui.theme.OrbitTheme.colors.surface
private val SettingsSearchBg: Color
    @Composable get() = com.example.orbitai.ui.theme.OrbitTheme.colors.surfaceInput
private val SettingsInk: Color
    @Composable get() = com.example.orbitai.ui.theme.OrbitTheme.colors.labelPrimary

@Composable
fun ModelSettingsScreen(
    downloadViewModel: DownloadViewModel,
    tokenStore: TokenStore,
    onNavigateToToken: () -> Unit,
    onBack: () -> Unit,
) {
    val progressMap by downloadViewModel.progress.collectAsState()
    val embeddingProgressMap by downloadViewModel.embeddingProgress.collectAsState()

    var tokenSaved by remember { mutableStateOf(tokenStore.hasToken()) }
    var geminiExpanded by remember { mutableStateOf(false) }
    var geminiModelName by remember { mutableStateOf(tokenStore.geminiModelName) }
    var geminiApiKey by remember { mutableStateOf(tokenStore.geminiApiKey) }
    var geminiSaved by remember { mutableStateOf(tokenStore.hasGeminiConfig()) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showAllModels by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeEmbedderId by remember { mutableStateOf(downloadViewModel.activeEmbeddingModelId) }
    val reindexRemaining by downloadViewModel.reindexRemaining(activeEmbedderId).collectAsState(0)

    val recommendedId = downloadViewModel.recommendedModelId
    val topPickIds = (listOfNotNull(recommendedId) + listOf("gemma4-e2b", "onnx-phi3-mini-4k", "onnx-gemma3-4b-it", "onnx-llama3.2-3b", "gemma3-1b"))
        .distinct()
    // Sort top picks based on the order in the set
    val topPickModels = topPickIds.mapNotNull { id -> AVAILABLE_MODELS.find { it.id == id } }
        .filter { it.displayName.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true) }
    val allModels = AVAILABLE_MODELS.filter { it.id !in topPickIds }
        .filter { it.displayName.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true) }
    val embedModels = AVAILABLE_EMBEDDING_MODELS
        .filter { it.displayName.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsSurface),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    )
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = SettingsInk.copy(alpha = 0.40f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.settings),
                    color = SettingsInk.copy(alpha = 0.40f),
                    fontFamily = SettingsMono,
                    fontSize = 13.sp,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 8.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.models),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = SettingsInk,
                    fontFamily = SettingsSans,
                )
                Text(
                    text = stringResource(R.string.download_configure_manage),
                    color = SettingsInk.copy(alpha = 0.35f),
                    fontFamily = SettingsMono,
                    fontSize = 11.sp,
                )
            }
        }



        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SettingsSearchBg)
                    .border(1.dp, SettingsInk.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = SettingsInk.copy(alpha = 0.40f),
                    modifier = Modifier.size(18.dp),
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = SettingsSans,
                        color = SettingsInk,
                    ),
                    cursorBrush = SolidColor(SettingsInk),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search models...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SettingsInk.copy(alpha = 0.40f),
                                    fontFamily = SettingsSans,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }

        item { SectionHeader("ON-DEVICE LLMs") }

        item {
            var wifiOnly by remember { mutableStateOf(downloadViewModel.downloadSettings.largeModelsOnWifiOnly) }
            FlatCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.download_large_models_on_wi), color = SettingsInk, fontFamily = SettingsSans, fontSize = 14.sp)
                        Text(
                            "This phone: ${formatModelSize(downloadViewModel.deviceSpecs.totalRamBytes)} RAM · ${formatModelSize(downloadViewModel.deviceSpecs.freeModelStorageBytes())} free",
                            color = SettingsInk.copy(alpha = 0.6f),
                            fontFamily = SettingsMono,
                            fontSize = 11.sp,
                        )
                        Text(
                            "Models over 1 GB wait for Wi-Fi. Downloads continue in the background and resume after interruptions.",
                            color = SettingsInk.copy(alpha = 0.45f),
                            fontFamily = SettingsSans,
                            fontSize = 11.sp,
                        )
                    }
                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = {
                            wifiOnly = it
                            downloadViewModel.setLargeModelsOnWifiOnly(it)
                        },
                    )
                }
            }
        }

        item {
            FlatCard(padding = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    topPickModels.forEachIndexed { index, model ->
                        ModelRow(
                            model = model,
                            progress = progressMap[model.id],
                            onDownload = {
                                if (MODEL_DOWNLOAD_SPECS[model.id]?.requiresAuth == true && !tokenStore.hasToken()) {
                                    onNavigateToToken()
                                } else {
                                    downloadViewModel.startDownload(model)
                                }
                            },
                            onDelete = { downloadViewModel.deleteModel(model) },
                            onCancel = { downloadViewModel.cancelDownload(model) },
                            onPause = { downloadViewModel.pauseDownload(model) },
                            fit = downloadViewModel.fitOf(model),
                            isRecommendedForPhone = model.id == recommendedId,
                        )
                        if (index != topPickModels.lastIndex) HairlineDivider()
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showAllModels = !showAllModels },
                    )
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showAllModels) "HIDE OTHER MODELS" else "SHOW ALL MODELS",
                    color = SettingsInk.copy(alpha = 0.6f),
                    fontFamily = SettingsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (showAllModels) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = SettingsInk.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (showAllModels) {
            item { SectionHeader("ALL MODELS") }
            item {
                FlatCard(padding = 0.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        allModels.forEachIndexed { index, model ->
                            ModelRow(
                                model = model,
                                progress = progressMap[model.id],
                                onDownload = {
                                    if (MODEL_DOWNLOAD_SPECS[model.id]?.requiresAuth == true && !tokenStore.hasToken()) {
                                        onNavigateToToken()
                                    } else {
                                        downloadViewModel.startDownload(model)
                                    }
                                },
                                onDelete = { downloadViewModel.deleteModel(model) },
                                onCancel = { downloadViewModel.cancelDownload(model) },
                                onPause = { downloadViewModel.pauseDownload(model) },
                                fit = downloadViewModel.fitOf(model),
                            )
                            if (index != allModels.lastIndex) HairlineDivider()
                        }
                    }
                }
            }
        }

        if (embedModels.isNotEmpty()) {
            item { SectionHeader("EMBEDDING") }
            item {
                FlatCard(padding = 0.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        embedModels.forEachIndexed { index, model ->
                            EmbeddingModelRow(
                                model = model,
                                progress = embeddingProgressMap[model.id],
                                isActive = activeEmbedderId == model.id,
                                onUse = { downloadViewModel.useEmbeddingModel(model); activeEmbedderId = model.id },
                                onDownload = { downloadViewModel.startEmbeddingDownload(model) },
                                onDelete = {
                                    downloadViewModel.deleteEmbeddingModel(model)
                                    if (activeEmbedderId == model.id) activeEmbedderId = downloadViewModel.activeEmbeddingModelId
                                },
                                onCancel = { downloadViewModel.cancelEmbeddingDownload(model) },
                            )
                            if (index != embedModels.lastIndex) HairlineDivider()
                        }
                        if (reindexRemaining > 0) {
                            Text(
                                text = "Updating search for your documents and memories: $reindexRemaining left. Until then they are found by keywords.",
                                color = SettingsInk.copy(alpha = 0.55f),
                                fontFamily = SettingsSans,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        item {
            FlatCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.huggingface_config),
                        color = SettingsInk,
                        fontFamily = SettingsSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (tokenStore.hasToken()) {
                            Text(
                                text = stringResource(R.string.configured),
                                color = SettingsGreen,
                                fontFamily = SettingsMono,
                                fontSize = 11.sp,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.no_token),
                                color = SettingsInk.copy(alpha = 0.45f),
                                fontFamily = SettingsMono,
                                fontSize = 11.sp,
                            )
                        }
                        SmallFilledButton(
                            label = if (tokenStore.hasToken()) "Update" else "Set Token",
                            color = SettingsInk,
                            enabled = true,
                            onClick = onNavigateToToken,
                        )
                    }
                }
            }
        }

        item { SectionHeader("CLOUD") }

        item {
            FlatCard(padding = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { geminiExpanded = !geminiExpanded },
                            )
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = stringResource(R.string.gemini_api),
                                color = SettingsInk,
                                fontFamily = SettingsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = stringResource(R.string.cloud_provider_integration),
                                color = SettingsInk.copy(alpha = 0.38f),
                                fontFamily = SettingsSans,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = SettingsInk.copy(alpha = 0.45f),
                            modifier = Modifier.size(13.dp),
                        )
                    }

                    if (geminiExpanded) {
                        HairlineDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CompactInputField(
                                value = geminiModelName,
                                placeholder = stringResource(R.string.gemini_flash_latest),
                                onValueChange = {
                                    geminiModelName = it
                                    geminiSaved = false
                                },
                            )
                            CompactInputField(
                                value = geminiApiKey,
                                placeholder = stringResource(R.string.api_key),
                                isPassword = !showGeminiKey,
                                onValueChange = {
                                    geminiApiKey = it
                                    geminiSaved = false
                                },
                                trailing = {
                                    IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                        Icon(
                                            imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = SettingsInk.copy(alpha = 0.55f),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                },
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SmallFilledButton(
                                    label = if (geminiSaved) "Saved" else "Save",
                                    color = SettingsInk,
                                    enabled = geminiModelName.isNotBlank() && geminiApiKey.isNotBlank() && !geminiSaved,
                                    onClick = {
                                        tokenStore.geminiModelName = geminiModelName
                                        tokenStore.geminiApiKey = geminiApiKey
                                        geminiSaved = true
                                    },
                                )
                                SmallOutlineButton(
                                    label = stringResource(R.string.clear),
                                    color = SettingsRed,
                                    onClick = {
                                        geminiModelName = ""
                                        geminiApiKey = ""
                                        tokenStore.geminiModelName = ""
                                        tokenStore.geminiApiKey = ""
                                        geminiSaved = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        item { CustomModelSection() }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.orbitai_on_device_llm),
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
private fun EmbeddingModelRow(
    model: EmbeddingModelConfig,
    progress: DownloadProgress?,
    isActive: Boolean,
    onUse: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    val status = progress?.status ?: DownloadStatus.IDLE
    val isInstalled = status == DownloadStatus.COMPLETED
    val isDownloading = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED
    val isFailed = status == DownloadStatus.FAILED
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_model), fontFamily = SettingsSans, fontSize = 18.sp, color = SettingsInk) },
            text = { Text("Are you sure you want to delete ${model.displayName}? You will need to re-download it to use it again.", fontFamily = SettingsSans, fontSize = 14.sp, color = SettingsInk.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.delete), color = SettingsRed, fontFamily = SettingsSans)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel), color = SettingsInk, fontFamily = SettingsSans)
                }
            },
            containerColor = SettingsCard,
            shape = RoundedCornerShape(14.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = model.displayName,
                    color = SettingsInk,
                    fontFamily = SettingsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
            }
            Text(
                text = if (isFailed) (progress?.error ?: "Download failed") else model.description,
                color = if (isFailed) SettingsRed else SettingsInk.copy(alpha = 0.38f),
                fontFamily = SettingsSans,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            model.sizeBytes?.let { size ->
                Text(
                    text = "Download size: ${formatModelSize(size)}",
                    color = SettingsInk.copy(alpha = 0.60f),
                    fontFamily = SettingsMono,
                    fontSize = 11.sp,
                )
            }
        }

        when {
            isInstalled -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SettingsGreen),
                    )
                    Text(
                        text = if (isActive) "In use" else "Use",
                        color = if (isActive) SettingsGreen else SettingsInk,
                        fontFamily = SettingsMono,
                        fontSize = 9.sp,
                        modifier = Modifier.clickable(
                            enabled = !isActive,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onUse,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.delete),
                        color = SettingsRed,
                        fontFamily = SettingsMono,
                        fontSize = 9.sp,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showDeleteDialog = true },
                        ),
                    )
                }
            }

            isDownloading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${progress?.progressPercent ?: 0}%",
                        color = SettingsInk.copy(alpha = 0.55f),
                        fontFamily = SettingsMono,
                        fontSize = 9.sp,
                    )
                    Text(
                        text = stringResource(R.string.cancel),
                        color = SettingsRed,
                        fontFamily = SettingsMono,
                        fontSize = 9.sp,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCancel,
                        ),
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SettingsInk.copy(alpha = 0.06f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDownload,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = if (isFailed) "Retry ${model.displayName}" else "Download ${model.displayName}",
                        tint = SettingsInk.copy(alpha = 0.62f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelRow(
    model: LlmModel,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onPause: () -> Unit = {},
    fit: ModelFit = ModelFit.GOOD,
    isRecommendedForPhone: Boolean = false,
) {
    var confirmLargeDownload by remember { mutableStateOf(false) }
    if (confirmLargeDownload) {
        AlertDialog(
            onDismissRequest = { confirmLargeDownload = false },
            title = { Text(stringResource(R.string.model_may_not_run), fontFamily = SettingsSans, fontSize = 18.sp, color = SettingsInk) },
            text = {
                Text(
                    "${model.displayName} probably needs more memory than this phone has. It may fail to load or close Orbit. Download anyway?",
                    fontFamily = SettingsSans, fontSize = 14.sp, color = SettingsInk.copy(alpha = 0.8f),
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmLargeDownload = false; onDownload() }) {
                    Text(stringResource(R.string.download), color = SettingsRed, fontFamily = SettingsSans)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLargeDownload = false }) { Text(stringResource(R.string.cancel), color = SettingsInk, fontFamily = SettingsSans) }
            },
            containerColor = SettingsCard,
            shape = RoundedCornerShape(14.dp),
        )
    }
    val startDownload = { if (fit == ModelFit.TOO_LARGE) confirmLargeDownload = true else onDownload() }
    val status = progress?.status ?: DownloadStatus.IDLE
    val isInstalled = status == DownloadStatus.COMPLETED
    val isDownloading = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.VERIFYING
    val isWaiting = status == DownloadStatus.PAUSED && progress?.canResume != true
    val isResumable = status == DownloadStatus.PAUSED && progress?.canResume == true
    val isFailed = status == DownloadStatus.FAILED
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_model), fontFamily = SettingsSans, fontSize = 18.sp, color = SettingsInk) },
            text = { Text("Are you sure you want to delete ${model.displayName}? You will need to re-download it to use it again.", fontFamily = SettingsSans, fontSize = 14.sp, color = SettingsInk.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.delete), color = SettingsRed, fontFamily = SettingsSans)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel), color = SettingsInk, fontFamily = SettingsSans)
                }
            },
            containerColor = SettingsCard,
            shape = RoundedCornerShape(14.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = model.displayName,
                    color = SettingsInk,
                    fontFamily = SettingsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
                if (isRecommendedForPhone) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF5B4FE8).copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.recommended_for_this_phone),
                            color = Color(0xFF5B4FE8),
                            fontFamily = SettingsMono,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (MODEL_DOWNLOAD_SPECS[model.id]?.requiresAuth == true) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SettingsInk.copy(alpha = 0.08f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.token_req),
                            color = SettingsInk.copy(alpha = 0.5f),
                            fontFamily = SettingsMono,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "· ${model.paramCount}",
                    color = SettingsInk.copy(alpha = 0.30f),
                    fontFamily = SettingsMono,
                    fontSize = 10.sp,
                )
            }
            Text(
                text = when {
                    isFailed -> progress?.error ?: "Download failed"
                    status == DownloadStatus.PAUSED -> progress?.error ?: "Paused"
                    else -> model.description
                },
                color = if (isFailed) SettingsRed else SettingsInk.copy(alpha = 0.38f),
                fontFamily = SettingsSans,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MODEL_DOWNLOAD_SPECS[model.id]?.totalSizeBytes?.let { size ->
                Text(
                    text = buildAnnotatedString {
                        append("${formatModelSize(size)} · ${DeviceFit.bestFor(model)} · ")
                        withStyle(SpanStyle(color = when (fit) {
                            ModelFit.GOOD -> SettingsGreen
                            ModelFit.SLOW -> Color(0xFFD48806)
                            ModelFit.TOO_LARGE -> SettingsRed
                        })) {
                            append(when (fit) {
                                ModelFit.GOOD -> "Fits this phone"
                                ModelFit.SLOW -> "May be slow"
                                ModelFit.TOO_LARGE -> "Too large for this phone"
                            })
                        }
                    },
                    color = SettingsInk.copy(alpha = 0.60f),
                    fontFamily = SettingsMono,
                    fontSize = 11.sp,
                )
            }
        }

        when {
            isInstalled -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SettingsGreen),
                    )
                    Text(
                        text = stringResource(R.string.delete),
                        color = SettingsRed,
                        fontFamily = SettingsMono,
                        fontSize = 9.sp,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showDeleteDialog = true },
                        ),
                    )
                }
            }

            isDownloading || isWaiting || isResumable -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isDownloading) {
                        Text(
                            text = if (status == DownloadStatus.VERIFYING) "Verifying" else "${progress?.progressPercent ?: 0}%",
                            color = SettingsInk.copy(alpha = 0.55f),
                            fontFamily = SettingsMono,
                            fontSize = 9.sp,
                        )
                    }
                    if (isDownloading || isResumable) {
                        Text(
                            text = if (isDownloading) "Pause" else "Resume",
                            color = SettingsInk.copy(alpha = 0.75f),
                            fontFamily = SettingsMono,
                            fontSize = 9.sp,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = if (isDownloading) onPause else startDownload,
                            ),
                        )
                    }
                    Text(
                        text = stringResource(R.string.cancel),
                        color = SettingsRed,
                        fontFamily = SettingsMono,
                        fontSize = 9.sp,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCancel,
                        ),
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SettingsInk.copy(alpha = 0.06f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = startDownload,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = if (isFailed) "Retry ${model.displayName}" else "Download ${model.displayName}",
                        tint = SettingsInk.copy(alpha = 0.62f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FlatCard(
    padding: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
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
            .padding(padding),
        content = content,
    )
}

@Composable
private fun SectionHeader(text: String) {
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
private fun ModelGroupHeader(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = title,
            color = SettingsInk.copy(alpha = 0.30f),
            fontFamily = SettingsMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
        )
        Text(
            text = subtitle,
            color = SettingsInk.copy(alpha = 0.22f),
            fontFamily = SettingsSans,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SmallFilledButton(
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (enabled) color else color.copy(alpha = 0.32f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (IsOrbitDarkTheme) SpaceDeep else Color.White,
            fontFamily = SettingsMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CompactInputField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(SettingsInk.copy(alpha = 0.06f))
            .padding(start = 10.dp, end = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = true,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = SettingsMono,
                    color = SettingsInk,
                    fontSize = 11.sp,
                ),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                cursorBrush = SolidColor(SettingsInk),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                color = SettingsInk.copy(alpha = 0.45f),
                                fontFamily = SettingsMono,
                                fontSize = 11.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            trailing?.invoke()
        }
    }
}

@Composable
private fun SmallOutlineButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.10f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = color,
            fontFamily = SettingsMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
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
