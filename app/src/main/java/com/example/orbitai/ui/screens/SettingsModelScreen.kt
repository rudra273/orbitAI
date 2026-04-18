package com.example.orbitai.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.core.model.AVAILABLE_MODELS
import com.example.orbitai.core.model.DownloadProgress
import com.example.orbitai.core.model.DownloadStatus
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.MODEL_DOWNLOAD_SPECS
import com.example.orbitai.core.common.TokenStore
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.TextPrimary
import com.example.orbitai.viewmodel.DownloadViewModel

private val SettingsSurfaceLight = Color(0xFFF9F8F5)
private val SettingsCardLight = Color(0xFFFFFFFF)
private val SettingsCardDark = Color(0xFF1E1E1C)
private val SettingsInkLight = Color(0xFF0D0D0D)
private val SettingsGreen = Color(0xFF17A865)
private val SettingsRed = Color(0xFFD94F4F)
private val SettingsSans = FontFamily.SansSerif
private val SettingsMono = FontFamily.Monospace

private val SettingsSurface: Color
    @Composable get() = if (IsOrbitDarkTheme) SpaceDeep else SettingsSurfaceLight
private val SettingsCard: Color
    @Composable get() = if (IsOrbitDarkTheme) SettingsCardDark else SettingsCardLight
private val SettingsInk: Color
    @Composable get() = if (IsOrbitDarkTheme) TextPrimary else SettingsInkLight

@Composable
fun ModelSettingsScreen(
    downloadViewModel: DownloadViewModel,
    tokenStore: TokenStore,
    onBack: () -> Unit,
) {
    val progressMap by downloadViewModel.progress.collectAsState()

    var token by remember { mutableStateOf(tokenStore.huggingFaceToken) }
    var tokenSaved by remember { mutableStateOf(tokenStore.hasToken()) }
    var showToken by remember { mutableStateOf(false) }
    var geminiExpanded by remember { mutableStateOf(false) }
    var geminiModelName by remember { mutableStateOf(tokenStore.geminiModelName) }
    var geminiApiKey by remember { mutableStateOf(tokenStore.geminiApiKey) }
    var geminiSaved by remember { mutableStateOf(tokenStore.hasGeminiConfig()) }
    var showGeminiKey by remember { mutableStateOf(false) }
    val publicModels = AVAILABLE_MODELS.filter { MODEL_DOWNLOAD_SPECS[it.id]?.requiresAuth == false }
    val authModels = AVAILABLE_MODELS.filter { MODEL_DOWNLOAD_SPECS[it.id]?.requiresAuth == true }

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
                    ),
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
                    text = "Settings",
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
                    text = "Models",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = SettingsInk,
                    fontFamily = SettingsSans,
                )
                Text(
                    text = "Download, configure & manage",
                    color = SettingsInk.copy(alpha = 0.35f),
                    fontFamily = SettingsMono,
                    fontSize = 11.sp,
                )
            }
        }

        item {
            FlatCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "HuggingFace Config",
                            color = SettingsInk,
                            fontFamily = SettingsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        )
                        Text(
                            text = "Token · on-device model downloads",
                            color = SettingsInk.copy(alpha = 0.35f),
                            fontFamily = SettingsMono,
                            fontSize = 10.sp,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(SettingsInk.copy(alpha = 0.06f))
                                .padding(start = 10.dp, end = 4.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            val displayToken = if (showToken) token else "*".repeat(token.length)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                BasicTextField(
                                    value = displayToken,
                                    onValueChange = { entered ->
                                        if (showToken) {
                                            token = entered
                                            tokenSaved = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = showToken,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = SettingsMono,
                                        color = SettingsInk,
                                        fontSize = 11.sp,
                                    ),
                                    cursorBrush = SolidColor(SettingsInk),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done,
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (displayToken.isEmpty()) {
                                            Text(
                                                "hf_xxx...",
                                                color = SettingsInk.copy(alpha = 0.45f),
                                                fontFamily = SettingsMono,
                                                fontSize = 11.sp,
                                            )
                                        }
                                        innerTextField()
                                    },
                                )

                                IconButton(onClick = { showToken = !showToken }) {
                                    Icon(
                                        imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = SettingsInk.copy(alpha = 0.55f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

                        SmallFilledButton(
                            label = if (tokenSaved) "Saved" else "Save",
                            color = SettingsInk,
                            enabled = token.isNotBlank() && !tokenSaved,
                            onClick = {
                                tokenStore.huggingFaceToken = token
                                tokenSaved = true
                            },
                        )

                        SmallOutlineButton(
                            label = "Clear",
                            color = SettingsRed,
                            onClick = {
                                token = ""
                                tokenStore.huggingFaceToken = ""
                                tokenSaved = false
                            },
                        )
                    }
                }
            }
        }

        item { SectionHeader("ON-DEVICE") }

        if (publicModels.isNotEmpty()) {
            item {
                ModelGroupHeader(
                    title = "PUBLIC DOWNLOADS",
                    subtitle = "No token required",
                )
            }

            item {
                FlatCard(padding = 0.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        publicModels.forEachIndexed { index, model ->
                            ModelRow(
                                model = model,
                                progress = progressMap[model.id],
                                onDownload = { downloadViewModel.startDownload(model) },
                                onDelete = { downloadViewModel.deleteModel(model) },
                                onCancel = { downloadViewModel.cancelDownload(model) },
                            )
                            if (index != publicModels.lastIndex) HairlineDivider()
                        }
                    }
                }
            }
        }

        if (authModels.isNotEmpty()) {
            item {
                ModelGroupHeader(
                    title = "REQUIRES HUGGINGFACE TOKEN",
                    subtitle = "Login token needed for download",
                )
            }

            item {
                FlatCard(padding = 0.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        authModels.forEachIndexed { index, model ->
                            ModelRow(
                                model = model,
                                progress = progressMap[model.id],
                                onDownload = { downloadViewModel.startDownload(model) },
                                onDelete = { downloadViewModel.deleteModel(model) },
                                onCancel = { downloadViewModel.cancelDownload(model) },
                            )
                            if (index != authModels.lastIndex) HairlineDivider()
                        }
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
                                text = "Gemini API",
                                color = SettingsInk,
                                fontFamily = SettingsSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = "Cloud provider integration",
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
                                placeholder = "gemini-flash-latest",
                                onValueChange = {
                                    geminiModelName = it
                                    geminiSaved = false
                                },
                            )
                            CompactInputField(
                                value = if (showGeminiKey) geminiApiKey else "*".repeat(geminiApiKey.length),
                                placeholder = "api key",
                                readOnly = !showGeminiKey,
                                onValueChange = {
                                    if (showGeminiKey) {
                                        geminiApiKey = it
                                        geminiSaved = false
                                    }
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
                                    label = "Clear",
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
private fun ModelRow(
    model: LlmModel,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    val status = progress?.status ?: DownloadStatus.IDLE
    val isInstalled = status == DownloadStatus.COMPLETED
    val isDownloading = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED
    val isFailed = status == DownloadStatus.FAILED

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
                Text(
                    text = "· ${model.paramCount}",
                    color = SettingsInk.copy(alpha = 0.30f),
                    fontFamily = SettingsMono,
                    fontSize = 10.sp,
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
                        text = "Delete",
                        color = SettingsRed,
                        fontFamily = SettingsMono,
                        fontSize = 9.sp,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDelete,
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
                        text = "Cancel",
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
    readOnly: Boolean = false,
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = !readOnly,
                readOnly = readOnly,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = SettingsMono,
                    color = SettingsInk,
                    fontSize = 11.sp,
                ),
                cursorBrush = SolidColor(SettingsInk),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = SettingsInk.copy(alpha = 0.45f),
                            fontFamily = SettingsMono,
                            fontSize = 11.sp,
                        )
                    }
                    innerTextField()
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
