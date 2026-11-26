package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
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
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.database.Mode
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.ModesViewModel

private val ModesActiveGreen @Composable get() = OrbitTheme.colors.success
private val ModesDeleteRed @Composable get() = OrbitTheme.colors.destructive
private val ModesSans = OrbitUiFont
private val ModesMono = OrbitMonoFont
private val ModeCardHeight = 152.dp

private val ModesSurface: Color
    @Composable get() = OrbitTheme.colors.background
private val ModesCard: Color
    @Composable get() = OrbitTheme.colors.surface
private val ModesInk: Color
    @Composable get() = OrbitTheme.colors.labelPrimary

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
                modes = modes,
                onEditMode = { destination = ModesDestination.Edit(it) },
                onCreateNew = { destination = ModesDestination.Edit(null) },
                inferenceForMode = { modeId -> viewModel.inferenceForMode(modeId) },
                libraryBar = { ModeLibraryBar(viewModel) },
            )
            is ModesDestination.Edit -> {
              var preset by remember(dest.mode?.id) { mutableStateOf(dest.mode?.preset ?: com.example.orbitai.core.database.ModePreset()) }
              val spaces by viewModel.spaces.collectAsState()
              val context = androidx.compose.ui.platform.LocalContext.current
              ModeEditScreen(
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
                        viewModel.createMode(name, prompt, inference, isActive, preset)
                    } else {
                        viewModel.updateMode(dest.mode.id, name, prompt, inference, isActive, preset)
                    }
                    destination = ModesDestination.List
                },
                presetEditor = {
                    ModePresetEditor(
                        preset = preset,
                        models = remember { viewModel.availableModels() },
                        spaces = spaces,
                        onChange = { preset = it },
                    )
                },
                onExport = dest.mode?.let { mode -> { shareMode(context, mode.name, viewModel.exportMode(mode)) } },
                onDelete  = {
                    dest.mode?.let { viewModel.deleteMode(it.id) }
                    destination = ModesDestination.List
                },
            )
            }
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
    libraryBar: @Composable () -> Unit = {},
) {
    val customModes = remember(modes) { modes.filterNot { it.isDefault } }
    val builtInModes = remember(modes) { modes.filter { it.isDefault } }
    val gridEntries = remember(customModes) { customModes + listOf<Mode?>(null) }
    val colors = OrbitTheme.colors

    Scaffold(
        containerColor = colors.background,
        topBar = {
            com.example.orbitai.ui.components.OrbitTopLevelHeader(
                title = stringResource(R.string.modes),
                subtitle = "${modes.size} mode${if (modes.size == 1) "" else "s"}",
            ) {
                com.example.orbitai.ui.components.OrbitIconButton(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_mode),
                    onClick = onCreateNew,
                    tint = colors.labelOnInk,
                    containerColor = colors.inkPrimary,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(padding),
            contentPadding = PaddingValues(
                start = OrbitTheme.spacing.screenHorizontal,
                end = OrbitTheme.spacing.screenHorizontal,
                top = OrbitTheme.spacing.xs,
                bottom = OrbitTheme.spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { libraryBar() }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    gridEntries.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
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

            if (builtInModes.isNotEmpty()) {
                item {
                    com.example.orbitai.ui.components.OrbitSectionHeader(
                        title = stringResource(R.string.built_in),
                    )
                }
                item {
                    com.example.orbitai.ui.components.OrbitGroupedCard {
                        builtInModes.forEachIndexed { index, mode ->
                            BuiltInModeRow(
                                mode = mode,
                                onClick = { onEditMode(mode) },
                            )
                            if (index != builtInModes.lastIndex) {
                                com.example.orbitai.ui.components.OrbitInsetDivider(startIndent = 16.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlatModeCard(
    mode: Mode,
    inference: InferenceSettings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = OrbitTheme.colors
    val shapes = OrbitTheme.shapes
    val emoji = extractLeadingEmoji(mode.name) ?: "🙂"
    val title = stripLeadingEmoji(mode.name)
    Box(
        modifier = modifier
            .clip(shapes.card)
            .background(colors.surface)
            .then(
                if (colors.isDark) Modifier.border(0.5.dp, colors.separator, shapes.card)
                else Modifier,
            )
            .clickable(onClick = onClick)
            .heightIn(min = ModeCardHeight)
            .padding(14.dp),
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
                    fontSize = 18.sp,
                )
                Text(
                    text = title,
                    style = OrbitTheme.typography.callout,
                    color = colors.labelPrimary,
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
                style = OrbitTheme.typography.footnote,
                color = colors.labelSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
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
            .size(7.dp)
            .clip(CircleShape)
            .background(ModesActiveGreen),
    )
}

@Composable
private fun MetaPill(text: String) {
    val colors = OrbitTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surfaceInput)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = OrbitTheme.typography.caption1.tabular(),
            color = colors.labelSecondary,
        )
    }
}

@Composable
private fun NewModeCell(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = OrbitTheme.colors
    val shapes = OrbitTheme.shapes
    Box(
        modifier = modifier
            .clip(shapes.card)
            .dashedRoundedBorder(
                color = colors.border,
                cornerRadius = shapes.cardRadius,
            )
            .clickable(onClick = onClick)
            .heightIn(min = ModeCardHeight)
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "+",
                style = OrbitTheme.typography.title2,
                color = colors.labelSecondary,
            )
            Text(
                text = stringResource(R.string.new_mode),
                style = OrbitTheme.typography.callout,
                color = colors.labelPrimary,
            )
            Text(
                text = stringResource(R.string.prompt_tuning),
                style = OrbitTheme.typography.caption1,
                color = colors.labelSecondary,
            )
            Text(
                text = stringResource(R.string.add_emoji_prompt_and_parameters),
                style = OrbitTheme.typography.caption1,
                color = colors.labelTertiary,
            )
        }
    }
}

@Composable
private fun BuiltInModeRow(
    mode: Mode,
    onClick: () -> Unit,
) {
    com.example.orbitai.ui.components.OrbitListRow(
        title = mode.name,
        subtitle = compactPrompt(mode.systemPrompt),
        trailing = if (mode.isActive) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ActiveDot()
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.Transparent,
                        modifier = Modifier.size(0.dp),
                    )
                }
            }
        } else null,
        onClick = onClick,
    )
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
    presetEditor: @Composable () -> Unit = {},
    onExport: (() -> Unit)? = null,
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
                                contentDescription = stringResource(R.string.back),
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
                            label = stringResource(R.string.save),
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
                        text = stringResource(R.string.tip_add_emoji_in_the),
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
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        presetEditor()
                        onExport?.let { export ->
                            androidx.compose.material3.TextButton(onClick = export) { Text(stringResource(R.string.share_this_mode)) }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ModeFieldLabel("Inference")
                        FlatActionButton(
                            label = stringResource(R.string.reset_default),
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
                        label = stringResource(R.string.temperature),
                        value = temperature,
                        valueStr = "%.2f".format(temperature),
                        range = 0.1f..2.0f,
                        onChange = { temperature = it },
                    )
                    Spacer(Modifier.height(6.dp))
                    CompactModeSlider(
                        label = stringResource(R.string.top_k),
                        value = topK.toFloat(),
                        valueStr = topK.toString(),
                        range = 1f..100f,
                        steps = 98,
                        onChange = { topK = it.toInt() },
                    )
                    Spacer(Modifier.height(6.dp))
                    CompactModeSlider(
                        label = stringResource(R.string.top_p),
                        value = topP,
                        valueStr = "%.2f".format(topP),
                        range = 0.1f..1.0f,
                        onChange = { topP = it },
                    )
                    Spacer(Modifier.height(6.dp))
                    CompactModeSlider(
                        label = stringResource(R.string.max_output_tokens),
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
                            label = stringResource(R.string.delete_mode),
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
                    text = stringResource(R.string.delete_mode_2),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.this_will_permanently_remove_this),
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
                    Text(stringResource(R.string.delete), color = Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = TextMuted)
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

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MODES 2.0: TEMPLATES, IMPORT / EXPORT, PRESETS (f039)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/** "Templates" and "Import" above the mode list. */
@Composable
private fun ModeLibraryBar(viewModel: ModesViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showTemplates by remember { mutableStateOf(false) }
    val importer = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        // Read a little past the limit so oversized files are reported, not truncated.
                        String(input.readNBytes(com.example.orbitai.feature.modes.ModeJson.MAX_BYTES + 1))
                    }
                }.getOrNull()
            }
            val message = if (text == null) "Couldn't read that file." else viewModel.importMode(text).fold(
                onSuccess = { "Added mode “$it”." },
                onFailure = { it.message ?: "That file isn't a valid Orbit mode." },
            )
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.material3.OutlinedButton(onClick = { showTemplates = true }) { Text(stringResource(R.string.templates)) }
        androidx.compose.material3.OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text(stringResource(R.string.action_import)) }
    }
    if (showTemplates) {
        AlertDialog(
            onDismissRequest = { showTemplates = false },
            title = { Text(stringResource(R.string.add_a_mode)) },
            text = {
                Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    com.example.orbitai.feature.modes.ModeTemplates.all.forEach { template ->
                        Column(
                            Modifier.fillMaxWidth().clickable {
                                viewModel.addTemplate(template)
                                showTemplates = false
                                android.widget.Toast.makeText(context, "Added ${template.name}. Edit it any time.", android.widget.Toast.LENGTH_SHORT).show()
                            }.padding(vertical = 10.dp),
                        ) {
                            Text(template.name, style = MaterialTheme.typography.titleSmall)
                            Text(template.systemPrompt, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { showTemplates = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}

private val ModeAccentColors = listOf(null, 0x8B5CF6, 0x3B82F6, 0x06B6D4, 0x10B981, 0xF59E0B, 0xEF4444, 0xEC4899)

/** Default model, default Spaces and chip colour for a mode. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ModePresetEditor(
    preset: com.example.orbitai.core.database.ModePreset,
    models: List<com.example.orbitai.core.model.LlmModel>,
    spaces: List<com.example.orbitai.core.database.Space>,
    onChange: (com.example.orbitai.core.database.ModePreset) -> Unit,
) {
    var modelMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeFieldLabel("When this mode is selected")
        Box {
            val current = models.find { it.id == preset.defaultModelId }?.displayName
                ?: preset.defaultModelId?.let { id -> com.example.orbitai.core.model.findModel(id)?.displayName?.plus(" (not downloaded)") }
                ?: "Keep the chat's model"
            androidx.compose.material3.OutlinedButton(onClick = { modelMenu = true }) { Text("Model: $current", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            androidx.compose.material3.DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.keep_the_chat_s_model)) }, onClick = { modelMenu = false; onChange(preset.copy(defaultModelId = null)) })
                models.forEach { model ->
                    androidx.compose.material3.DropdownMenuItem(text = { Text(model.displayName) }, onClick = { modelMenu = false; onChange(preset.copy(defaultModelId = model.id)) })
                }
            }
        }
        if (spaces.isNotEmpty()) {
            Text(stringResource(R.string.spaces_to_turn_on), style = MaterialTheme.typography.bodySmall)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                spaces.forEach { space ->
                    val on = space.id in preset.defaultSpaceIds
                    androidx.compose.material3.FilterChip(
                        selected = on,
                        onClick = { onChange(preset.copy(defaultSpaceIds = if (on) preset.defaultSpaceIds - space.id else preset.defaultSpaceIds + space.id)) },
                        label = { Text(space.name) },
                    )
                }
            }
        }
        Text(stringResource(R.string.colour), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeAccentColors.forEach { color ->
                val selected = preset.accentColor == color
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color?.let { Color(0xFF000000 or it.toLong()) } ?: SpaceDust)
                        .border(if (selected) 3.dp else 1.dp, if (selected) TextPrimary else GlassBorder, CircleShape)
                        .clickable(onClickLabel = if (color == null) "No colour" else "Colour") { onChange(preset.copy(accentColor = color)) },
                )
            }
        }
    }
}

/** Shares a mode as a small .json file through the share sheet. */
private fun shareMode(context: android.content.Context, name: String, json: String) {
    val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
    val safe = name.replace(Regex("""[^\p{L}\p{M}\p{N} _-]+"""), "").trim().replace(Regex("\\s+"), "-").ifBlank { "mode" }
    val file = java.io.File(dir, "$safe.orbitmode.json").apply { writeText(json) }
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
        .setType("application/json")
        .putExtra(android.content.Intent.EXTRA_STREAM, uri)
        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(android.content.Intent.createChooser(send, "Share mode").addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION))
}
