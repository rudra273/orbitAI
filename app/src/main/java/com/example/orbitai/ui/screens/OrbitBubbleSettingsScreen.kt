package com.example.orbitai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.orbitai.data.LlmModel
import com.example.orbitai.data.ModelProvider
import com.example.orbitai.data.ToolSettingsStore
import com.example.orbitai.tools.bubble.OrbitBubbleService
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.TextPrimary

private val BubbleSurfaceLight = Color(0xFFF9F8F5)
private val BubbleCardLight = Color(0xFFFFFFFF)
private val BubbleCardDark = Color(0xFF1E1E1C)
private val BubbleInkLight = Color(0xFF0D0D0D)
private val BubbleGreen = Color(0xFF17A865)
private val BubbleSans = FontFamily.SansSerif
private val BubbleMono = FontFamily.Monospace

private val BubbleSurface: Color
    @Composable get() = if (IsOrbitDarkTheme) SpaceDeep else BubbleSurfaceLight
private val BubbleCard: Color
    @Composable get() = if (IsOrbitDarkTheme) BubbleCardDark else BubbleCardLight
private val BubbleInk: Color
    @Composable get() = if (IsOrbitDarkTheme) TextPrimary else BubbleInkLight

@Composable
fun OrbitBubbleSettingsScreen(
    toolSettingsStore: ToolSettingsStore,
    tokenStore: com.example.orbitai.data.TokenStore,
    availableModels: List<LlmModel>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var bubbleEnabled by remember { mutableStateOf(toolSettingsStore.isFloatingBubbleEnabled) }
    var bubbleSizeDp by remember { mutableIntStateOf(toolSettingsStore.bubbleSizeDp) }
    var responseHeightDp by remember { mutableIntStateOf(toolSettingsStore.bubbleResponseHeightDp) }
    var bubbleIdleAlphaPercent by remember { mutableIntStateOf(toolSettingsStore.bubbleIdleAlphaPercent) }
    var bubbleStyle by remember { mutableStateOf(toolSettingsStore.bubbleStyle) }
    var resultsInOverlay by remember { mutableStateOf(toolSettingsStore.bubbleResultsInOverlay) }
    var bubbleModelId by remember { mutableStateOf(toolSettingsStore.bubbleModelId) }

    var overlayGranted by remember { mutableStateOf(OrbitBubbleService.canDrawOverlays(context)) }
    var audioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var pendingBubbleEnable by remember { mutableStateOf(false) }

    val cloudModel = remember(availableModels, tokenStore.geminiModelName) {
        availableModels.firstOrNull { it.provider == ModelProvider.GEMINI }
            ?: LlmModel(
                id = "gemini-api",
                displayName = tokenStore.geminiModelName.ifBlank { "gemini-flash-latest" },
                fileName = "",
                description = "Cloud model via API",
                paramCount = "API",
                provider = ModelProvider.GEMINI,
            )
    }

    val onDeviceModel = remember(availableModels) {
        availableModels.firstOrNull { it.id == "gemma3-1b" }
            ?: availableModels.firstOrNull { it.provider == ModelProvider.LOCAL }
            ?: LlmModel(
                id = "gemma3-1b",
                displayName = "Gemma 3 1B",
                fileName = "",
                description = "On-device model",
                paramCount = "1B",
                provider = ModelProvider.LOCAL,
            )
    }

    fun applyBubbleModel(model: LlmModel) {
        bubbleModelId = model.id
        toolSettingsStore.bubbleModelId = model.id
        if (bubbleEnabled) OrbitBubbleService.start(context)
    }

    fun updateSize(newSize: Int) {
        bubbleSizeDp = newSize
        toolSettingsStore.bubbleSizeDp = newSize
        if (bubbleEnabled) OrbitBubbleService.start(context)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        audioGranted = granted
        if (granted && pendingBubbleEnable && overlayGranted) {
            bubbleEnabled = true
            toolSettingsStore.isFloatingBubbleEnabled = true
            OrbitBubbleService.start(context)
        } else if (!granted) {
            bubbleEnabled = false
            toolSettingsStore.isFloatingBubbleEnabled = false
            OrbitBubbleService.stop(context)
        }
        pendingBubbleEnable = false
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        overlayGranted = OrbitBubbleService.canDrawOverlays(context)
        if (!overlayGranted) {
            pendingBubbleEnable = false
            bubbleEnabled = false
            toolSettingsStore.isFloatingBubbleEnabled = false
            OrbitBubbleService.stop(context)
        } else if (pendingBubbleEnable) {
            if (audioGranted) {
                bubbleEnabled = true
                toolSettingsStore.isFloatingBubbleEnabled = true
                OrbitBubbleService.start(context)
                pendingBubbleEnable = false
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun updateBubble(enabled: Boolean) {
        if (!enabled) {
            pendingBubbleEnable = false
            bubbleEnabled = false
            toolSettingsStore.isFloatingBubbleEnabled = false
            OrbitBubbleService.stop(context)
            return
        }

        pendingBubbleEnable = true
        if (!overlayGranted) {
            overlayPermissionLauncher.launch(OrbitBubbleService.overlayPermissionIntent(context))
            return
        }
        if (!audioGranted) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        bubbleEnabled = true
        toolSettingsStore.isFloatingBubbleEnabled = true
        OrbitBubbleService.start(context)
        pendingBubbleEnable = false
    }

    LaunchedEffect(Unit) {
        overlayGranted = OrbitBubbleService.canDrawOverlays(context)
        audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        bubbleEnabled = toolSettingsStore.isFloatingBubbleEnabled && overlayGranted && audioGranted
        bubbleSizeDp = toolSettingsStore.bubbleSizeDp
        responseHeightDp = toolSettingsStore.bubbleResponseHeightDp
        bubbleIdleAlphaPercent = toolSettingsStore.bubbleIdleAlphaPercent
        bubbleStyle = toolSettingsStore.bubbleStyle
        resultsInOverlay = toolSettingsStore.bubbleResultsInOverlay
        bubbleModelId = toolSettingsStore.bubbleModelId
        if (bubbleEnabled) OrbitBubbleService.start(context)
    }

    val bubbleSizePercent = ((bubbleSizeDp - 40f) / (80f - 40f) * 100f).coerceIn(0f, 100f)
    val responseHeightPercent = ((responseHeightDp - 100f) / (400f - 100f) * 100f).coerceIn(0f, 100f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BubbleSurface),
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
                    tint = BubbleInk.copy(alpha = 0.40f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Settings",
                    color = BubbleInk.copy(alpha = 0.40f),
                    fontFamily = BubbleMono,
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
                    text = "Orbit Bubble",
                    color = BubbleInk,
                    fontFamily = BubbleSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                )
                Text(
                    text = "Floating assistant behavior",
                    color = BubbleInk.copy(alpha = 0.35f),
                    fontFamily = BubbleMono,
                    fontSize = 11.sp,
                )
            }
        }

        item { BubbleSectionLabel("MODEL") }
        item {
            GroupedCard {
                ModelRow(
                    dotColor = BubbleInk.copy(alpha = 0.20f),
                    title = cloudModel.displayName.ifBlank { "gemini-flash-latest" },
                    subtitle = "API · Cloud",
                    selected = bubbleModelId == cloudModel.id,
                    onClick = { applyBubbleModel(cloudModel) },
                )
                HairlineDivider()
                ModelRow(
                    dotColor = BubbleGreen,
                    title = onDeviceModel.displayName.ifBlank { "Gemma 3 1B" },
                    subtitle = "On-device · ${onDeviceModel.paramCount.ifBlank { "1B" }}",
                    selected = bubbleModelId == onDeviceModel.id,
                    onClick = { applyBubbleModel(onDeviceModel) },
                )
            }
        }

        item { BubbleSectionLabel("BEHAVIOUR") }
        item {
            GroupedCard {
                ToggleRow(
                    title = "Enable bubble",
                    subtitle = if (bubbleEnabled) {
                        "Bubble is active over other apps"
                    } else {
                        "Show a draggable floating bubble"
                    },
                    checked = bubbleEnabled,
                    onCheckedChange = ::updateBubble,
                )
                HairlineDivider()
                ToggleRow(
                    title = "Response mode",
                    subtitle = if (resultsInOverlay) {
                        "Response appears directly over current app"
                    } else {
                        "Response opens inside Orbit chat"
                    },
                    checked = resultsInOverlay,
                    onCheckedChange = {
                        resultsInOverlay = it
                        toolSettingsStore.bubbleResultsInOverlay = it
                    },
                )
                HairlineDivider()
                BubbleStyleRow(
                    currentStyle = bubbleStyle,
                    onChange = {
                        bubbleStyle = it
                        toolSettingsStore.bubbleStyle = it
                        if (bubbleEnabled) OrbitBubbleService.start(context)
                    },
                )
            }
        }

        item { BubbleSectionLabel("APPEARANCE") }
        item {
            GroupedCard(paddingHorizontal = 0.dp) {
                AppearanceSliderRow(
                    title = "Bubble size",
                    valueLabel = "${bubbleSizePercent.toInt()}% · ${bubbleSizeDp}px",
                    value = bubbleSizeDp.toFloat(),
                    range = 40f..80f,
                    onValueChange = { value ->
                        updateSize(value.toInt().coerceIn(40, 80))
                    },
                )
                HairlineDivider()
                AppearanceSliderRow(
                    title = "Idle opacity",
                    valueLabel = "${bubbleIdleAlphaPercent}%",
                    value = bubbleIdleAlphaPercent.toFloat(),
                    range = 20f..100f,
                    description = "How visible bubble stays when idle",
                    onValueChange = { value ->
                        bubbleIdleAlphaPercent = value.toInt().coerceIn(20, 100)
                        toolSettingsStore.bubbleIdleAlphaPercent = bubbleIdleAlphaPercent
                        if (bubbleEnabled) OrbitBubbleService.start(context)
                    },
                )
                HairlineDivider()
                AppearanceSliderRow(
                    title = "Response height",
                    valueLabel = "${responseHeightPercent.toInt()}%",
                    value = responseHeightDp.toFloat(),
                    range = 100f..400f,
                    description = "Overlay response panel height",
                    onValueChange = { value ->
                        responseHeightDp = value.toInt().coerceIn(100, 400)
                        toolSettingsStore.bubbleResponseHeightDp = responseHeightDp
                        if (bubbleEnabled) OrbitBubbleService.start(context)
                    },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "OrbitAI · On-device LLM",
                modifier = Modifier.fillMaxWidth(),
                color = BubbleInk.copy(alpha = 0.20f),
                fontFamily = BubbleMono,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GroupedCard(
    paddingHorizontal: androidx.compose.ui.unit.Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BubbleCard)
            .border(
                width = 1.dp,
                color = BubbleInk.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = paddingHorizontal),
        content = content,
    )
}

@Composable
private fun BubbleSectionLabel(text: String) {
    Text(
        text = text,
        color = BubbleInk.copy(alpha = 0.30f),
        fontFamily = BubbleMono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

@Composable
private fun ModelRow(
    dotColor: Color,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                color = BubbleInk,
                fontFamily = BubbleSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = BubbleInk.copy(alpha = 0.28f),
                fontFamily = BubbleMono,
                fontSize = 10.sp,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = if (selected) BubbleInk.copy(alpha = 0.85f) else BubbleInk.copy(alpha = 0.45f),
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                color = BubbleInk,
                fontFamily = BubbleSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Text(
                text = subtitle,
                color = BubbleInk.copy(alpha = 0.35f),
                fontFamily = BubbleSans,
                fontSize = 11.sp,
            )
        }

        BubbleToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun BubbleToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val knobColor = if (checked && IsOrbitDarkTheme) SpaceDeep else Color.White

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(CircleShape)
            .background(if (checked) BubbleInk else BubbleInk.copy(alpha = 0.12f))
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
                .size(18.dp)
                .clip(CircleShape)
                .background(knobColor),
        )
    }
}

@Composable
private fun BubbleStyleRow(
    currentStyle: String,
    onChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = "Bubble style",
                color = BubbleInk,
                fontFamily = BubbleSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Text(
                text = "Round bubble or right-side slide tab",
                color = BubbleInk.copy(alpha = 0.35f),
                fontFamily = BubbleSans,
                fontSize = 11.sp,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SegmentedChip(
                label = "Round",
                selected = currentStyle == "round",
                onClick = { onChange("round") },
            )
            SegmentedChip(
                label = "Slide",
                selected = currentStyle == "slide",
                onClick = { onChange("slide") },
            )
        }
    }
}

@Composable
private fun SegmentedChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) BubbleInk else BubbleInk.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) {
                if (IsOrbitDarkTheme) SpaceDeep else Color.White
            } else {
                BubbleInk.copy(alpha = 0.45f)
            },
            fontFamily = BubbleMono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    description: String = "",
    onValueChange: (Float) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.coerceIn(range.start, range.endInclusive)) }

    LaunchedEffect(value) {
        sliderValue = value.coerceIn(range.start, range.endInclusive)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = BubbleInk,
                fontFamily = BubbleSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Text(
                text = valueLabel,
                color = BubbleInk.copy(alpha = 0.35f),
                fontFamily = BubbleMono,
                fontSize = 11.sp,
            )
        }

        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it)
            },
            valueRange = range,
            thumb = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(BubbleInk),
                )
            },
            colors = SliderDefaults.colors(
                thumbColor = BubbleInk,
                activeTrackColor = BubbleInk,
                inactiveTrackColor = BubbleInk.copy(alpha = 0.10f),
            ),
        )

        if (description.isNotBlank()) {
            Text(
                text = description,
                color = BubbleInk.copy(alpha = 0.35f),
                fontFamily = BubbleSans,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun HairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(BubbleInk.copy(alpha = 0.08f)),
    )
}
