package com.example.orbitai.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.orbitai.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.InferenceSettings
import com.example.orbitai.data.db.Mode
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.ModesViewModel

// Modes accent — teal/emerald
private val ModesAccent    = Color(0xFF10B981)
private val ModesAccentDim = Color(0xFF059669)
private val ModesFrost     = Color(0x1A10B981)   // 10% teal glass fill

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
                onSave    = { name, prompt, inference ->
                    if (dest.mode == null) {
                        viewModel.createMode(name, prompt, inference)
                    } else {
                        viewModel.updateMode(dest.mode.id, name, prompt, inference)
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
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        // Ambient teal glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to ModesAccent.copy(alpha = 0.04f),
                            0.0f to ModesAccent.copy(alpha = 0.04f),
                            1.0f to Color.Transparent,
                        ),
                        radius = 700f,
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                "Modes",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                            )
                            Text(
                                "${modes.size} mode${if (modes.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color        = ModesAccent,
                                    letterSpacing = 1.sp,
                                    fontWeight   = FontWeight.SemiBold,
                                ),
                            )
                        }
                    },
                    colors   = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(top = 4.dp),
                )
            },
            floatingActionButton = {
                ModesFAB(onClick = onCreateNew)
            },
        ) { padding ->
            if (modes.isEmpty()) {
                ModesEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onCreate = onCreateNew,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    modifier        = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding  = PaddingValues(
                        start  = 16.dp,
                        end    = 16.dp,
                        top    = 8.dp,
                        bottom = 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        items = modes,
                        key   = { _, m -> m.id },
                    ) { index, mode ->
                        StaggeredFadeSlide(index = index) {
                            ModeCard(
                                mode     = mode,
                                onClick  = { onEditMode(mode) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// FAB
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ModesFAB(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            color       = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                24f, 0f, 4f,
                                ModesAccent.copy(alpha = 0.4f).toArgb(),
                            )
                        }
                    }
                    canvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        18.dp.toPx(), 18.dp.toPx(), paint,
                    )
                }
            }
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(listOf(ModesAccent, ModesAccentDim))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Create Mode",
            tint     = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MODE CARD
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ModeCard(
    mode:     Mode,
    onClick:  () -> Unit,
) {
    // Default mode gets violet, custom modes get teal
    val cardAccent = if (mode.isDefault) VioletCore else ModesAccent
    val cardFrost  = if (mode.isDefault) VioletFrost else ModesFrost
    val isDark = IsOrbitDarkTheme
    val cardShape = RoundedCornerShape(18.dp)

    // Light mode tinted glass color per accent
    val lightGlassTint = if (mode.isDefault) Color(0xFFF0ECFF) else Color(0xFFE8FFF5)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            color       = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                if (isDark) 24f else 16f,
                                0f, 4f,
                                (if (isDark) Color.Black else cardAccent)
                                    .copy(alpha = if (isDark) 0.30f else 0.08f)
                                    .toArgb(),
                            )
                        }
                    }
                    canvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        18.dp.toPx(), 18.dp.toPx(), paint,
                    )
                }
            }
            .clip(cardShape)
            .background(
                if (isDark) Color.White.copy(alpha = 0.05f)
                else lightGlassTint.copy(alpha = 0.82f)
            )
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f  to Color.White.copy(alpha = if (isDark) 0.07f else 0.50f),
                        0.25f to Color.White.copy(alpha = if (isDark) 0.02f else 0.10f),
                        0.5f  to Color.Transparent,
                    ),
                )
            )
            .border(
                width = if (isDark) 1.dp else 1.5.dp,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to (if (isDark) Color.White else cardAccent)
                                     .copy(alpha = if (isDark) 0.18f else 0.40f),
                        0.5f to cardAccent.copy(alpha = if (isDark) 0.12f else 0.18f),
                        1.0f to (if (isDark) Color.White else cardAccent)
                                     .copy(alpha = if (isDark) 0.05f else 0.08f),
                    ),
                    start = Offset.Zero,
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
                shape = cardShape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
    ) {
        Column(
            modifier             = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardFrost)
                        .border(
                            width = 0.5.dp,
                            color = cardAccent.copy(alpha = if (isDark) 0.22f else 0.28f),
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (mode.isDefault) {
                        Image(
                            painter = painterResource(R.drawable.vector_logo),
                            contentDescription = "OrbitAI",
                            modifier = Modifier.size(26.dp),
                        )
                    } else {
                        Text(
                            text       = mode.name.take(1).uppercase(),
                            fontSize   = 18.sp,
                            color      = cardAccent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint     = TextMuted.copy(0.35f),
                    modifier = Modifier.size(12.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                mode.name,
                style     = MaterialTheme.typography.titleMedium,
                color     = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (mode.isDefault) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(VioletGlow)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        "default",
                        style = MaterialTheme.typography.labelSmall,
                        color = VioletBright,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                mode.systemPrompt,
                style    = MaterialTheme.typography.bodySmall,
                color    = TextMuted,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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
    onSave:   (name: String, prompt: String, inference: InferenceSettings) -> Unit,
    onDelete: () -> Unit,
) {
    var name   by remember { mutableStateOf(mode?.name ?: "") }
    var prompt by remember { mutableStateOf(mode?.systemPrompt ?: "") }
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
            .background(SpaceDeep),
    ) {
        // Ambient teal glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to ModesAccent.copy(alpha = 0.05f),
                            1.0f to Color.Transparent,
                        ),
                        radius = 600f,
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextSecondary,
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ModesFrost),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.SmartToy,
                                    null,
                                    tint     = ModesAccent,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Text(
                                if (isNew) "New Mode" else if (isDefault) "Edit Orbit" else "Edit Mode",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                            )
                        }
                    },
                    actions = {
                        // Save button — top right
                        GlassActionButton(
                            label = "Save",
                            onClick = {
                                if (canSave) {
                                    onSave(
                                        name.trim(),
                                        prompt.trim(),
                                        InferenceSettings(
                                            temperature = temperature,
                                            topK = topK,
                                            topP = topP,
                                            maxDecodedTokens = maxDecodedTokens,
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier
                                .padding(end = 12.dp),
                            accent = ModesAccent,
                            enabled = canSave,
                            filled = true,
                            compact = true,
                        )
                    },
                    colors   = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(top = 4.dp),
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
                    top    = 8.dp,
                    bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    // ── Name field ─────────────────────────────────────────
                    ModeFieldLabel("Mode Name")
                    Spacer(Modifier.height(6.dp))
                    TextField(
                        value         = name,
                        onValueChange = { if (!isDefault) name = it },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassWhite8)
                            .background(
                                brush = Brush.linearGradient(
                                    if (name.isNotBlank())
                                        listOf(ModesAccent.copy(0.25f), ModesAccent.copy(0.05f))
                                    else
                                        listOf(GlassBorder, GlassBorder.copy(0.02f))
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ),
                        placeholder   = {
                            Text(
                                "e.g. Code Reviewer, Tutor…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMuted,
                            )
                        },
                        colors        = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor        = TextPrimary,
                            unfocusedTextColor      = TextPrimary,
                            cursorColor             = ModesAccent,
                            disabledContainerColor  = Color.Transparent,
                            disabledIndicatorColor  = Color.Transparent,
                            disabledTextColor       = TextSecondary,
                        ),
                        textStyle  = MaterialTheme.typography.bodyLarge,
                        singleLine = true,
                        readOnly   = isDefault,
                    )
                }

                item {
                    // ── System prompt field ────────────────────────────────
                    ModeFieldLabel("System Prompt")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Defines how this mode behaves and responds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value         = prompt,
                        onValueChange = { prompt = it },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassWhite8)
                            .background(
                                brush = Brush.linearGradient(
                                    if (prompt.isNotBlank())
                                        listOf(ModesAccent.copy(0.2f), ModesAccent.copy(0.03f))
                                    else
                                        listOf(GlassBorder, GlassBorder.copy(0.02f))
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ),
                        placeholder   = {
                            Text(
                                "You are a helpful assistant that…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                            )
                        },
                        colors        = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor        = TextPrimary,
                            unfocusedTextColor      = TextPrimary,
                            cursorColor             = ModesAccent,
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                        ),
                        maxLines  = 20,
                    )

                    // Character count
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${prompt.length} characters",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = TextMuted.copy(0.5f),
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
                        GlassActionButton(
                            label = "Reset Default",
                            onClick = resetInference,
                            accent = ModesAccent,
                            enabled = true,
                            filled = false,
                            compact = true,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "These settings apply only to this mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(10.dp))

                    CompactModeSlider(
                        label = "Temperature",
                        value = temperature,
                        valueStr = "%.2f".format(temperature),
                        range = 0.1f..2.0f,
                        accent = ModesAccent,
                        onChange = { temperature = it },
                    )
                    Spacer(Modifier.height(8.dp))
                    CompactModeSlider(
                        label = "Top-K",
                        value = topK.toFloat(),
                        valueStr = topK.toString(),
                        range = 1f..100f,
                        steps = 98,
                        accent = ModesAccent,
                        onChange = { topK = it.toInt() },
                    )
                    Spacer(Modifier.height(8.dp))
                    CompactModeSlider(
                        label = "Top-P",
                        value = topP,
                        valueStr = "%.2f".format(topP),
                        range = 0.1f..1.0f,
                        accent = ModesAccent,
                        onChange = { topP = it },
                    )
                    Spacer(Modifier.height(8.dp))
                    CompactModeSlider(
                        label = "Max output tokens",
                        value = maxDecodedTokens.toFloat(),
                        valueStr = maxDecodedTokens.toString(),
                        range = 128f..2048f,
                        steps = 14,
                        accent = ModesAccent,
                        onChange = { maxDecodedTokens = ((it / 128).toInt() * 128).coerceAtLeast(128) },
                    )
                }

                // Delete button — only for non-default existing modes
                if (!isNew && !isDefault) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        GlassActionButton(
                            label = "Delete Mode",
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth(),
                            accent = Destructive,
                            enabled = true,
                            filled = false,
                            compact = false,
                            leadingIcon = Icons.Default.Delete,
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
    val infiniteTransition = rememberInfiniteTransition(label = "mode_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.1f,
        targetValue   = 0.3f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val isDark = IsOrbitDarkTheme
        val iconShape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .size(80.dp)
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                color       = android.graphics.Color.TRANSPARENT
                                setShadowLayer(
                                    40f, 0f, 0f,
                                    ModesAccent.copy(alpha = glowAlpha).toArgb(),
                                )
                            }
                        }
                        canvas.drawCircle(
                            androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                            size.minDimension / 2f, paint,
                        )
                    }
                }
                .clip(iconShape)
                .background(
                    if (isDark) ModesFrost
                    else Color.White.copy(alpha = 0.80f)
                )
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.White.copy(alpha = if (isDark) 0.08f else 0.40f),
                            0.5f to Color.Transparent,
                        ),
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to ModesAccent.copy(alpha = if (isDark) 0.35f else 0.45f),
                            1.0f to ModesAccent.copy(alpha = if (isDark) 0.08f else 0.12f),
                        ),
                    ),
                    shape = iconShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint     = ModesAccent,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "No modes yet",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Create custom modes with different\nsystem prompts for any purpose",
            style     = MaterialTheme.typography.bodyMedium,
            color     = TextMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        Box(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(listOf(ModesAccent, ModesAccentDim))
                )
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
                    style      = MaterialTheme.typography.titleMedium,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
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
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.5.sp,
            fontWeight    = FontWeight.Bold,
        ),
        color = ModesAccent.copy(0.8f),
    )
}

@Composable
private fun CompactModeSlider(
    label: String,
    value: Float,
    valueStr: String,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = if (IsOrbitDarkTheme) 0.10f else 0.10f),
                        Color.White.copy(alpha = if (IsOrbitDarkTheme) 0.035f else 0.70f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (IsOrbitDarkTheme) 0.14f else 0.32f),
                        accent.copy(alpha = if (IsOrbitDarkTheme) 0.18f else 0.14f),
                    )
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = valueStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
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
                    .height(26.dp),
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent.copy(alpha = 0.78f),
                    inactiveTrackColor = Color.White.copy(alpha = if (IsOrbitDarkTheme) 0.08f else 0.18f),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun GlassActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color,
    enabled: Boolean,
    filled: Boolean,
    compact: Boolean,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val shape = RoundedCornerShape(if (compact) 14.dp else 12.dp)
    Box(
        modifier = modifier
            .height(if (compact) 34.dp else 46.dp)
            .clip(shape)
            .background(
                if (filled) {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = if (enabled) 0.12f else 0.06f),
                            accent.copy(alpha = if (enabled) 0.36f else 0.08f),
                            Color.White.copy(alpha = if (enabled) 0.05f else 0.04f),
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = if (enabled) 0.10f else 0.04f),
                            Color.White.copy(alpha = if (IsOrbitDarkTheme) 0.035f else 0.70f),
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (enabled) 0.16f else 0.08f),
                        accent.copy(alpha = if (enabled) 0.22f else 0.08f),
                    )
                ),
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = if (compact) 12.dp else 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            leadingIcon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (enabled) accent else TextMuted,
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                )
            }
            Text(
                text = label,
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                color = if (enabled) accent else TextMuted,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
