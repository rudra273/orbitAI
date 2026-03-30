package com.example.orbitai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.InferenceSettings
import com.example.orbitai.data.db.Mode
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.ModesViewModel

private val ModesSurfaceLight = Color(0xFFF9F8F5)
private val ModesCardLight = Color(0xFFFFFFFF)
private val ModesCardDark = Color(0xFF1E1E1C)
private val ModesInkLight = Color(0xFF0D0D0D)
private val ModesActiveGreen = Color(0xFF17A865)
private val ModesDeleteRed = Color(0xFFD94F4F)
private val ModesSans = FontFamily.SansSerif
private val ModesMono = FontFamily.Monospace
private val ModeCardHeight = 152.dp

private val ModesSurface: Color
    @Composable get() = if (IsOrbitDarkTheme) SpaceDeep else ModesSurfaceLight
private val ModesCard: Color
    @Composable get() = if (IsOrbitDarkTheme) ModesCardDark else ModesCardLight
private val ModesInk: Color
    @Composable get() = if (IsOrbitDarkTheme) TextPrimary else ModesInkLight

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MODES SCREEN — list + inline edit destination
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private sealed interface ModesDestination {
    data object List : ModesDestination
    data class Edit(val mode: Mode?)  : ModesDestination   // null = create new
}

@Composable
fun ModesScreen(viewModel: ModesViewModel) {
    val modes by viewModel.modes.collectAsState()
    var destination by remember { mutableStateOf<ModesDestination>(ModesDestination.List) }
    val isEditing = destination is ModesDestination.Edit

    BackHandler(enabled = isEditing) {
        destination = ModesDestination.List
    }

    AnimatedContent(
        targetState   = destination,
        transitionSpec = {
            if (targetState is ModesDestination.List) {
                (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "modes_nav",
    ) { dest ->
        when (dest) {
            is ModesDestination.List -> ModeListScreen(
                modes      = modes,
                onEditMode = { destination = ModesDestination.Edit(it) },
                onCreateNew = { destination = ModesDestination.Edit(null) },
                inferenceForMode = { modeId -> viewModel.inferenceForMode(modeId) },
            )
            is ModesDestination.Edit -> ModeEditScreen(
                mode      = dest.mode,
                defaultInference = viewModel.defaultInference(),
                initialInference = if (dest.mode == null) {
                    viewModel.defaultInference()
                } else {
                    viewModel.inferenceForMode(dest.mode.id)
                },
                onBack    = { destination = ModesDestination.List },
                onSave    = { name, prompt, inference, isActive ->
                    if (dest.mode == null) {
                        viewModel.createMode(name, prompt, inference, isActive)
                    } else {
                        viewModel.updateMode(dest.mode.id, name, prompt, inference, isActive)
                    }
                    destination = ModesDestination.List
                },
                onDelete  = {
                    dest.mode?.let { viewModel.deleteMode(it.id) }
                    destination = ModesDestination.List
                },
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MODE LIST
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeListScreen(
    modes:      List<Mode>,
    onEditMode: (Mode) -> Unit,
    onCreateNew: () -> Unit,
    inferenceForMode: (String) -> InferenceSettings,
) {
    val activeCustomModes = remember(modes) { modes.filterNot { it.isDefault }.filter { it.isActive } }
    val hiddenCustomModes = remember(modes) { modes.filterNot { it.isDefault }.filterNot { it.isActive } }
    val builtInModes = remember(modes) { modes.filter { it.isDefault } }
    val activeMode = remember(modes) { modes.firstOrNull { it.isActive } }
    val activeGridEntries = remember(activeCustomModes) { activeCustomModes + listOf<Mode?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ModesSurface)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 18.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Modes",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = ModesInk,
                )
                Text(
                    text = "${modes.size} mode${if (modes.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = ModesInk.copy(alpha = 0.35f),
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ModesInk)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCreateNew,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create mode",
                    tint = if (IsOrbitDarkTheme) SpaceDeep else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (activeMode != null) {
            ModeActiveBanner(
                mode = activeMode,
                description = compactPrompt(activeMode.systemPrompt),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp),
            contentPadding = PaddingValues(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activeGridEntries.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowItems.forEach { mode ->
                                if (mode == null) {
                                    NewModeCell(
                                        modifier = Modifier.weight(1f),
                                        onClick = onCreateNew,
                                    )
                                } else {
                                    FlatModeCard(
                                        mode = mode,
                                        inference = inferenceForMode(mode.id),
                                        modifier = Modifier.weight(1f),
                                        onClick = { onEditMode(mode) },
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (hiddenCustomModes.isNotEmpty()) {
                item {
                    Text(
                        text = "HIDDEN FROM CHAT",
                        color = ModesInk.copy(alpha = 0.30f),
                        fontFamily = ModesMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        hiddenCustomModes.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rowItems.forEach { mode ->
                                    FlatModeCard(
                                        mode = mode,
                                        inference = inferenceForMode(mode.id),
                                        modifier = Modifier.weight(1f),
                                        onClick = { onEditMode(mode) },
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            if (builtInModes.isNotEmpty()) {
                item {
                    Text(
                        text = "BUILT-IN",
                        color = ModesInk.copy(alpha = 0.30f),
                        fontFamily = ModesMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        builtInModes.forEachIndexed { index, mode ->
                            BuiltInModeRow(
                                mode = mode,
                                onClick = { onEditMode(mode) },
                            )
                            if (index != builtInModes.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(ModesInk.copy(alpha = 0.06f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeActiveBanner(
    mode: Mode,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ModesActiveGreen.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = ModesActiveGreen.copy(alpha = 0.22f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ModesActiveGreen),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = mode.name,
            color = ModesInk,
            fontFamily = ModesSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = " · ",
            color = ModesInk.copy(alpha = 0.4f),
            fontFamily = ModesMono,
            fontSize = 11.sp,
        )
        Text(
            text = description,
            color = ModesInk.copy(alpha = 0.4f),
            fontFamily = ModesSans,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        ActiveDot()
    }
}

@Composable
private fun FlatModeCard(
    mode: Mode,
    inference: InferenceSettings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val emoji = extractLeadingEmoji(mode.name) ?: "🙂"
    val title = stripLeadingEmoji(mode.name)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ModesCard)
            .border(
                width = 1.dp,
                color = ModesInk.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .height(ModeCardHeight)
            .padding(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = emoji,
                    fontFamily = ModesSans,
                    fontSize = 18.sp,
                )
                Text(
                    text = title,
                    color = ModesInk,
                    fontFamily = ModesSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (mode.isActive) {
                    ActiveDot()
                }
            }
            Text(
                text = compactPrompt(mode.systemPrompt),
                color = ModesInk.copy(alpha = 0.42f),
                fontFamily = ModesSans,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetaPill("T ${"%.2f".format(inference.temperature)}")
                MetaPill("TOK ${inference.maxDecodedTokens}")
            }
        }
    }
}

@Composable
private fun ActiveDot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(ModesActiveGreen),
    )
}

@Composable
private fun MetaPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ModesInk.copy(alpha = 0.06f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = ModesInk.copy(alpha = 0.55f),
            fontFamily = ModesMono,
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun NewModeCell(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .dashedRoundedBorder(
                color = ModesInk.copy(alpha = 0.18f),
                cornerRadius = 14.dp,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .height(ModeCardHeight)
            .padding(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "+",
                color = ModesInk.copy(alpha = 0.30f),
                fontFamily = ModesSans,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "New mode",
                color = ModesInk.copy(alpha = 0.35f),
                fontFamily = ModesSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Text(
                text = "Prompt + tuning",
                color = ModesInk.copy(alpha = 0.30f),
                fontFamily = ModesSans,
                fontSize = 11.sp,
            )
            Text(
                text = "Add emoji, prompt, and parameters",
                color = ModesInk.copy(alpha = 0.24f),
                fontFamily = ModesSans,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun BuiltInModeRow(
    mode: Mode,
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.name,
                color = ModesInk,
                fontFamily = ModesSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = compactPrompt(mode.systemPrompt),
                color = ModesInk.copy(alpha = 0.38f),
                fontFamily = ModesSans,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (mode.isActive) {
                ActiveDot()
            }
            Text(
                text = "›",
                color = ModesInk.copy(alpha = 0.55f),
                fontFamily = ModesSans,
                fontSize = 16.sp,
            )
        }
    }
}

private fun compactPrompt(prompt: String): String {
    return prompt
        .replace("\n", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { "No prompt" }
}

private fun extractLeadingEmoji(text: String): String? {
    val trimmed = text.trimStart()
    if (trimmed.isBlank()) return null
    val firstCodePoint = trimmed.codePointAt(0)
    val isEmoji = firstCodePoint in 0x1F300..0x1FAFF ||
        firstCodePoint in 0x2600..0x27BF ||
        firstCodePoint in 0x1F1E6..0x1F1FF
    if (!isEmoji) return null
    return String(Character.toChars(firstCodePoint))
}

private fun stripLeadingEmoji(text: String): String {
    val trimmed = text.trim()
    val emoji = extractLeadingEmoji(trimmed) ?: return trimmed
    return trimmed.removePrefix(emoji).trim().ifBlank { trimmed }
}

private fun normalizeModeNameInput(input: String): String {
    val trimmed = input.trimStart()
    if (trimmed.isBlank()) return ""

    val leadingEmoji = extractLeadingEmoji(trimmed)
    var titlePart = if (leadingEmoji != null) {
        trimmed.removePrefix(leadingEmoji).trimStart()
    } else {
        trimmed
    }

    while (true) {
        val extraEmoji = extractLeadingEmoji(titlePart) ?: break
        titlePart = titlePart.removePrefix(extraEmoji).trimStart()
    }

    val normalizedTitle = titlePart.replace(Regex("\\s+"), " ").trim()
    return when {
        leadingEmoji == null -> normalizedTitle
        normalizedTitle.isBlank() -> leadingEmoji
        else -> "$leadingEmoji $normalizedTitle"
    }
}

private fun Modifier.dashedRoundedBorder(
    color: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MODE EDIT SCREEN — full-screen, not a dialog
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeEditScreen(
    mode:     Mode?,            // null = creating new
    defaultInference: InferenceSettings,
    initialInference: InferenceSettings,
    onBack:   () -> Unit,
    onSave:   (name: String, prompt: String, inference: InferenceSettings, isActive: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var name   by rememberSaveable(mode?.id) { mutableStateOf(mode?.name ?: "") }
    var prompt by rememberSaveable(mode?.id) { mutableStateOf(mode?.systemPrompt ?: "") }
    var isActive by rememberSaveable(mode?.id) { mutableStateOf(mode?.isActive ?: true) }
    var temperature by remember(mode?.id) { mutableFloatStateOf(initialInference.temperature) }
    var topK by remember(mode?.id) { mutableIntStateOf(initialInference.topK) }
    var topP by remember(mode?.id) { mutableFloatStateOf(initialInference.topP) }
    var maxDecodedTokens by remember(mode?.id) { mutableIntStateOf(initialInference.maxDecodedTokens) }
    val isNew  = mode == null
    val isDefault = mode?.isDefault == true
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank() && prompt.isNotBlank()
    val resetInference = {
        temperature = defaultInference.temperature
        topK = defaultInference.topK
        topP = defaultInference.topP
        maxDecodedTokens = defaultInference.maxDecodedTokens
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ModesSurface),
    ) {
        Scaffold(
            containerColor = ModesSurface,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ModesInk,
                            )
                        }
                    },
                    title = {
                        Text(
                            text = if (isNew) "New Mode" else if (isDefault) "Edit Orbit" else "Edit Mode",
                            color = ModesInk,
                            fontFamily = ModesSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )
                    },
                    actions = {
                        // Active / Hidden toggle button
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isActive) ModesActiveGreen.copy(alpha = 0.12f)
                                    else ModesInk.copy(alpha = 0.08f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isActive) ModesActiveGreen.copy(alpha = 0.30f)
                                    else ModesInk.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { isActive = !isActive },
                                )
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) ModesActiveGreen
                                            else ModesInk.copy(alpha = 0.30f)
                                        ),
                                )
                                Text(
                                    text = if (isActive) "Active" else "Hidden",
                                    color = if (isActive) ModesActiveGreen
                                    else ModesInk.copy(alpha = 0.50f),
                                    fontFamily = ModesMono,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        FlatActionButton(
                            label = "Save",
                            onClick = {
                                if (canSave) {
                                    onSave(
                                        normalizeModeNameInput(name),
                                        prompt.trim(),
                                        InferenceSettings(
                                            temperature = temperature,
                                            topK = topK,
                                            topP = topP,
                                            maxDecodedTokens = maxDecodedTokens,
                                        ),
                                        isActive,
                                    )
                                }
                            },
                            modifier = Modifier
                                .padding(end = 12.dp),
                            enabled = canSave,
                            filled = true,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ModesSurface),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier        = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding  = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    top    = 4.dp,
                    bottom = 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    // ── Name field ─────────────────────────────────────────
                    ModeFieldLabel("Mode Name")
                    Spacer(Modifier.height(4.dp))
                    TextField(
                        value         = name,
                        onValueChange = { if (!isDefault) name = normalizeModeNameInput(it) },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ModesCard)
                            .border(
                                width = 1.dp,
                                color = ModesInk.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp),
                            ),
                        placeholder   = {
                            Text(
                                "e.g. 🤖 Code Reviewer",
                                fontFamily = ModesSans,
                                fontSize = 14.sp,
                                color = ModesInk.copy(alpha = 0.38f),
                            )
                        },
                        colors        = TextFieldDefaults.colors(
                            focusedContainerColor   = ModesCard,
                            unfocusedContainerColor = ModesCard,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor        = ModesInk,
                            unfocusedTextColor      = ModesInk,
                            cursorColor             = ModesInk,
                            disabledContainerColor  = ModesCard,
                            disabledIndicatorColor  = Color.Transparent,
                            disabledTextColor       = ModesInk.copy(alpha = 0.45f),
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = ModesSans,
                            fontSize = 14.sp,
                            color = ModesInk,
                        ),
                        singleLine = true,
                        readOnly   = isDefault,
                    )
                    Text(
                        text = "Tip: add emoji in the mode name, e.g. 🤖 Code Reviewer",
                        fontFamily = ModesSans,
                        fontSize = 10.sp,
                        color = ModesInk.copy(alpha = 0.38f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                }

                item {
                    // ── System prompt field ────────────────────────────────
                    ModeFieldLabel("System Prompt")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Defines how this mode behaves and responds.",
                        fontFamily = ModesSans,
                        fontSize = 11.sp,
                        color = ModesInk.copy(alpha = 0.42f),
                    )
                    Spacer(Modifier.height(6.dp))
                    TextField(
                        value         = prompt,
                        onValueChange = { prompt = it },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 148.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ModesCard)
                            .border(
                                width = 1.dp,
                                color = ModesInk.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp),
                            ),
                        placeholder   = {
                            Text(
                                "You are a helpful assistant that…",
                                fontFamily = ModesSans,
                                fontSize = 13.sp,
                                color = ModesInk.copy(alpha = 0.38f),
                            )
                        },
                        colors        = TextFieldDefaults.colors(
                            focusedContainerColor   = ModesCard,
                            unfocusedContainerColor = ModesCard,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor        = ModesInk,
                            unfocusedTextColor      = ModesInk,
                            cursorColor             = ModesInk,
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = ModesSans,
                            fontSize = 13.sp,
                            color = ModesInk,
                            lineHeight = 19.sp,
                        ),
                        maxLines  = 20,
                    )

                    // Character count
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${prompt.length} characters",
                        fontFamily = ModesMono,
                        fontSize = 10.sp,
                        color    = ModesInk.copy(alpha = 0.42f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ModeFieldLabel("Inference")
                        FlatActionButton(
                            label = "Reset Default",
                            onClick = resetInference,
                            enabled = true,
                            filled = false,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "These settings apply only to this mode.",
                        fontFamily = ModesSans,
                        fontSize = 11.sp,
                        color = ModesInk.copy(alpha = 0.42f),
                    )
                    Spacer(Modifier.height(6.dp))

                    CompactModeSlider(
                        label = "Temperature",
                        value = temperature,
                        valueStr = "%.2f".format(temperature),
                        range = 0.1f..2.0f,
                        onChange = { temperature = it },
                    )
                    Spacer(Modifier.height(6.dp))
                    CompactModeSlider(
                        label = "Top-K",
                        value = topK.toFloat(),
                        valueStr = topK.toString(),
                        range = 1f..100f,
                        steps = 98,
                        onChange = { topK = it.toInt() },
                    )
                    Spacer(Modifier.height(6.dp))
                    CompactModeSlider(
                        label = "Top-P",
                        value = topP,
                        valueStr = "%.2f".format(topP),
                        range = 0.1f..1.0f,
                        onChange = { topP = it },
                    )
                    Spacer(Modifier.height(6.dp))
                    CompactModeSlider(
                        label = "Max output tokens",
                        value = maxDecodedTokens.toFloat(),
                        valueStr = maxDecodedTokens.toString(),
                        range = 128f..2048f,
                        steps = 14,
                        onChange = { maxDecodedTokens = ((it / 128).toInt() * 128).coerceAtLeast(128) },
                    )
                }

                // Delete button — only for non-default existing modes
                if (!isNew && !isDefault) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        FlatActionButton(
                            label = "Delete Mode",
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth(),
                            enabled = true,
                            filled = false,
                            destructive = true,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete mode?",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = "This will permanently remove this mode.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                ) {
                    Text("Delete", color = Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// EMPTY STATE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ModesEmptyState(
    modifier: Modifier = Modifier,
    onCreate: () -> Unit,
) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ModesCard)
                .border(
                    width = 1.dp,
                    color = ModesInk.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint     = ModesInk.copy(alpha = 0.55f),
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "No modes yet",
            color = ModesInk,
            fontFamily = ModesSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Create custom modes with different\nsystem prompts for any purpose",
            color     = ModesInk.copy(alpha = 0.42f),
            fontFamily = ModesSans,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        Box(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ModesInk)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onCreate,
                )
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(
                    "Create Mode",
                    fontFamily = ModesSans,
                    fontSize = 13.sp,
                    color      = Color.White,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HELPERS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ModeFieldLabel(text: String) {
    Text(
        text.uppercase(),
        color = ModesInk.copy(alpha = 0.32f),
        fontFamily = ModesMono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
    )
}


@Composable
private fun CompactModeSlider(
    label: String,
    value: Float,
    valueStr: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ModesCard)
            .border(
                width = 1.dp,
                color = ModesInk.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = ModesInk,
                    fontFamily = ModesSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(ModesInk.copy(alpha = 0.06f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = valueStr,
                        color = ModesInk.copy(alpha = 0.58f),
                        fontFamily = ModesMono,
                        fontSize = 9.sp,
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                steps = steps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                colors = SliderDefaults.colors(
                    thumbColor = ModesInk,
                    activeTrackColor = ModesInk.copy(alpha = 0.78f),
                    inactiveTrackColor = ModesInk.copy(alpha = 0.15f),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun FlatActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    filled: Boolean,
    destructive: Boolean = false,
) {
    val buttonColor = if (destructive) ModesDeleteRed else ModesInk
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .background(if (filled) buttonColor else ModesCard)
            .border(
                width = 1.dp,
                color = if (filled) buttonColor else buttonColor.copy(alpha = 0.20f),
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (filled) {
                if (IsOrbitDarkTheme) SpaceDeep else Color.White
            } else {
                buttonColor
            },
            fontFamily = ModesSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
