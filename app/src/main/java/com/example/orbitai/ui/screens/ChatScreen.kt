package com.example.orbitai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.AVAILABLE_MODELS
import com.example.orbitai.data.Chat
import com.example.orbitai.data.Message
import com.example.orbitai.data.Role
import com.example.orbitai.data.db.Mode
import com.example.orbitai.data.db.Space
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CHAT SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId:    String,
    viewModel: ChatViewModel,
    onBack:    () -> Unit,
) {
    val chats          by viewModel.chats.collectAsState()
    val uiState        by viewModel.uiState.collectAsState()
    val spaces         by viewModel.spaces.collectAsState()
    val activeSpaceIds by viewModel.activeSpaceIds.collectAsState()
    val modes          by viewModel.modes.collectAsState()
    val activeModeId   by viewModel.activeModeId.collectAsState()

    val chat     = chats.find { it.id == chatId }
    val messages = chat?.messages ?: emptyList()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        // Ambient glow — top
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to VioletGlowSoft.copy(alpha = 0.06f),
                            1.0f to Color.Transparent,
                        ),
                        radius = 700f,
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ChatTopBar(
                    chat          = chat,
                    modes         = modes,
                    activeModeId  = activeModeId,
                    spaces        = spaces,
                    activeSpaceIds = activeSpaceIds,
                    onBack         = onBack,
                    onModelSelected = { model -> viewModel.selectModel(chatId, model) },
                    onSelectMode    = { viewModel.selectMode(it) },
                    onToggleSpace   = { viewModel.toggleSpace(it) },
                )
            },
            bottomBar = {
                ChatInputBar(
                    text         = inputText,
                    onTextChange = { inputText = it },
                    onSend       = {
                        val text = inputText.trim()
                        if (text.isNotEmpty() && !uiState.isGenerating && !uiState.isModelLoading) {
                            inputText = ""
                            viewModel.sendMessage(chatId, text)
                            scope.launch {
                                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                            }
                        }
                    },
                    onStop       = { viewModel.stopGeneration() },
                    isGenerating = uiState.isGenerating,
                    isLoading    = uiState.isModelLoading,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // Status banners
                if (uiState.isModelLoading) {
                    GlassStatusBanner("Loading model…", VioletCore)
                }
                uiState.loadError?.let {
                    GlassStatusBanner(it, Destructive)
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state               = listState,
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageBubble(msg)
                        }
                    }

                    if (messages.isEmpty() && !uiState.isGenerating && !uiState.isModelLoading) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(VioletFrost)
                                    .border(
                                        width = 1.dp,
                                        color = VioletCore.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(22.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "✦",
                                    fontSize = 30.sp,
                                    color = VioletBright,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "How can I help you today?",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary.copy(alpha = 0.92f),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Ask anything, generate code, or explore ideas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TOP BAR — mode name, space chips, model pill, back button
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    chat:           Chat?,
    modes:          List<Mode>,
    activeModeId:   String,
    spaces:         List<Space>,
    activeSpaceIds: Set<String>,
    onBack:         () -> Unit,
    onModelSelected: (com.example.orbitai.data.LlmModel) -> Unit,
    onSelectMode:   (String) -> Unit,
    onToggleSpace:  (String) -> Unit,
) {
    val activeMode = modes.find { it.id == activeModeId } ?: modes.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(SpaceDust.copy(alpha = 0.95f), Color.Transparent)
                )
            )
            .statusBarsPadding(),
    ) {
        // ── Row 1: back + title + model pill ──────────────────────────────
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
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text   = chat?.title ?: "Chat",
                        style  = MaterialTheme.typography.titleLarge,
                        color  = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (activeMode != null) {
                        Text(
                            text  = "✦  ${activeMode.name}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color      = VioletBright,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            },
            actions = {
                // Model selector pill
                if (chat != null) {
                    ModelPill(chat = chat, onModelSelected = onModelSelected)
                }
                Spacer(Modifier.width(8.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.padding(top = 4.dp),
        )

        // ── Row 2: mode + space selectors ────────────────────────────────
        if (modes.isNotEmpty() || spaces.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Mode single-select dropdown
                if (modes.isNotEmpty() && activeMode != null) {
                    ModeDropdownChip(
                        modes        = modes,
                        activeMode   = activeMode,
                        activeModeId = activeModeId,
                        onSelectMode = onSelectMode,
                    )
                }

                // Space multi-select chips
                if (spaces.isNotEmpty()) {
                    LazyRow(
                        modifier              = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(spaces, key = { it.id }) { space ->
                            SpaceToggleChip(
                                space          = space,
                                selected       = space.id in activeSpaceIds,
                                onToggleSpace  = onToggleSpace,
                            )
                        }
                    }
                }
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, GlassBorder, Color.Transparent)
                    )
                )
        )
    }
}

// ── Model pill ────────────────────────────────────────────────────────────────

@Composable
private fun ModelPill(
    chat:            Chat,
    onModelSelected: (com.example.orbitai.data.LlmModel) -> Unit,
) {
    var expanded    by remember { mutableStateOf(false) }
    val selectedModel = AVAILABLE_MODELS.find { it.id == chat.modelId } ?: AVAILABLE_MODELS.first()

    Box {
        val isDark = IsOrbitDarkTheme
        val pillShape = RoundedCornerShape(15.dp)
        Box(
            modifier = Modifier
                .height(30.dp)
                .clip(pillShape)
                .background(
                    if (isDark) Color.White.copy(alpha = 0.05f)
                    else Color(0xFFF0ECFF).copy(alpha = 0.78f)
                )
                .border(
                    width = if (isDark) 0.5.dp else 1.dp,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to (if (isDark) Color.White else VioletCore)
                                         .copy(alpha = if (isDark) 0.15f else 0.30f),
                            1.0f to VioletCore.copy(alpha = if (isDark) 0.05f else 0.08f),
                        ),
                    ),
                    shape = pillShape,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { expanded = true }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text  = selectedModel.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Select model",
                    tint     = TextMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            containerColor   = SpaceNebula,
        ) {
            AVAILABLE_MODELS.forEach { model ->
                val isSelected = model.id == chat.modelId
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                model.displayName,
                                color      = if (isSelected) VioletBright else TextPrimary,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            Text(
                                model.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    },
                    onClick = { onModelSelected(model); expanded = false },
                    trailingIcon = if (isSelected) ({
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(VioletCore)
                        )
                    }) else null,
                )
            }
        }
    }
}

// ── Mode dropdown chip ────────────────────────────────────────────────────────

@Composable
private fun ModeDropdownChip(
    modes:         List<Mode>,
    activeMode:    Mode,
    activeModeId:  String,
    onSelectMode:  (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(VioletFrost)
                .glowBorder(VioletCore.copy(0.25f), 15.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { expanded = true }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "✦",
                    fontSize = 9.sp,
                    color    = VioletBright,
                )
                Text(
                    activeMode.name,
                    style  = MaterialTheme.typography.labelMedium,
                    color  = VioletBright,
                    fontWeight = FontWeight.Medium,
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Switch mode",
                    tint     = VioletBright.copy(0.7f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            containerColor   = SpaceNebula,
        ) {
            modes.forEach { mode ->
                val isSelected = mode.id == activeModeId
                DropdownMenuItem(
                    text = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    mode.name,
                                    color      = if (isSelected) VioletBright else TextPrimary,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                if (mode.isDefault) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(VioletGlow)
                                            .padding(horizontal = 5.dp, vertical = 1.dp),
                                    ) {
                                        Text(
                                            "default",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VioletBright,
                                        )
                                    }
                                }
                            }
                            Text(
                                mode.systemPrompt,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    onClick      = { onSelectMode(mode.id); expanded = false },
                    trailingIcon = if (isSelected) ({
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(VioletCore)
                        )
                    }) else null,
                )
            }
        }
    }
}

// ── Space toggle chip ─────────────────────────────────────────────────────────

@Composable
private fun SpaceToggleChip(
    space:         Space,
    selected:      Boolean,
    onToggleSpace: (String) -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue   = if (selected) VioletFrost else GlassWhite4,
        animationSpec = tween(200),
        label         = "space_chip_bg",
    )
    val textColor by animateColorAsState(
        targetValue   = if (selected) VioletBright else TextMuted,
        animationSpec = tween(200),
        label         = "space_chip_text",
    )
    val borderColor by animateColorAsState(
        targetValue   = if (selected) VioletCore.copy(0.5f) else GlassBorder,
        animationSpec = tween(200),
        label         = "space_chip_border",
    )

    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .background(
                brush = Brush.linearGradient(listOf(borderColor, borderColor.copy(0.1f))),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
            ) { onToggleSpace(space.id) }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = space.name,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MESSAGE BUBBLE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun MessageBubble(msg: Message) {
    val isUser = msg.role == Role.USER

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = if (isUser) {
                Modifier.widthIn(max = 296.dp)
            } else {
                Modifier.fillMaxWidth()
            },
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            // Bubble
            Box(
                modifier = Modifier
                    .then(
                        if (isUser) {
                            Modifier
                                .drawBehind {
                                    // User bubble: soft violet glow
                                    drawIntoCanvas { canvas ->
                                        val paint = Paint().apply {
                                            asFrameworkPaint().apply {
                                                isAntiAlias = true
                                                color       = android.graphics.Color.TRANSPARENT
                                                setShadowLayer(
                                                    16f, 0f, 2f,
                                                    VioletCore.copy(0.2f).toArgb(),
                                                )
                                            }
                                        }
                                        canvas.drawRoundRect(
                                            0f, 0f, size.width, size.height,
                                            18.dp.toPx(), 18.dp.toPx(), paint,
                                        )
                                    }
                                }
                                .clip(
                                    RoundedCornerShape(
                                        topStart    = 18.dp,
                                        topEnd      = 4.dp,
                                        bottomStart = 18.dp,
                                        bottomEnd   = 18.dp,
                                    )
                                )
                                .background(OrbitGradients.userBubble)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(UserBubbleBorder, UserBubbleBorder.copy(0.15f))
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart    = 18.dp,
                                        topEnd      = 4.dp,
                                        bottomStart = 18.dp,
                                        bottomEnd   = 18.dp,
                                    ),
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                        }
                    ),
            ) {
                if (msg.isStreaming && msg.content.isEmpty()) {
                    TypingIndicator()
                } else {
                    SelectionContainer {
                        MarkdownMessageText(msg.content)
                    }
                    if (msg.isStreaming) {
                        Spacer(Modifier.height(4.dp))
                        StreamingCursor()
                    }
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
}

@Composable
private fun MarkdownMessageText(content: String) {
    val blocks = remember(content) { parseMarkdownBlocks(content) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val headingStyle = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    Text(
                        text = parseInlineMarkdown(block.text),
                        style = headingStyle,
                        color = TextPrimary,
                        lineHeight = (headingStyle.fontSize.value + 8).sp,
                    )
                }

                is MarkdownBlock.Bullet -> {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            lineHeight = 23.sp,
                        )
                        Text(
                            text = parseInlineMarkdown(block.text),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            lineHeight = 23.sp,
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        lineHeight = 23.sp,
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    CodeBlockView(language = block.language, code = block.code)
                }
            }
        }
    }
}

@Composable
private fun CodeBlockView(language: String, code: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A2E))
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        // Header bar: language label + copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D0D1A))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = language.ifBlank { "code" },
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (copied) Color(0xFF10B981).copy(0.15f) else Color.White.copy(0.06f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (copied) "Copied!" else "Copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (copied) Color(0xFF10B981) else TextMuted,
                )
            }
        }

        // Code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            SelectionContainer {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight  = 20.sp,
                    ),
                    color = Color(0xFFE2E8F0),
                    softWrap = false,
                )
            }
        }
    }

    // Auto-reset "Copied!" label after 2 s
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }
}

private fun parseMarkdownBlocks(content: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = mutableListOf<String>()
    val lines = content.lines()
    var i = 0

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(paragraphBuffer.joinToString("\n").trim())
            paragraphBuffer.clear()
        }
    }

    while (i < lines.size) {
        val line = lines[i]

        // Fenced code block: opening ```[lang]
        if (line.trimStart().startsWith("```")) {
            flushParagraph()
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines += lines[i]
                i++
            }
            // skip closing ```
            if (i < lines.size) i++
            blocks += MarkdownBlock.CodeBlock(language = lang, code = codeLines.joinToString("\n"))
            continue
        }

        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").find(line)
        val bulletMatch  = Regex("^\\s*[-*+]\\s+(.+)$").find(line)
        val orderedMatch = Regex("^\\s*\\d+\\.\\s+(.+)$").find(line)

        when {
            headingMatch != null -> {
                flushParagraph()
                blocks += MarkdownBlock.Heading(
                    level = headingMatch.groupValues[1].length,
                    text  = headingMatch.groupValues[2],
                )
            }
            bulletMatch != null -> {
                flushParagraph()
                blocks += MarkdownBlock.Bullet(text = bulletMatch.groupValues[1])
            }
            orderedMatch != null -> {
                flushParagraph()
                blocks += MarkdownBlock.Bullet(text = orderedMatch.groupValues[1])
            }
            line.isBlank() -> flushParagraph()
            else -> paragraphBuffer += line
        }
        i++
    }

    flushParagraph()
    return if (blocks.isEmpty()) listOf(MarkdownBlock.Paragraph(content)) else blocks
}

private fun parseInlineMarkdown(input: String): AnnotatedString = buildAnnotatedString {
    // Matches in priority order: **bold**, *emphasis*, `code`
    val pattern = Regex("""\*\*(.+?)\*\*|\*(.+?)\*|`+(.+?)`+""")
    var lastIndex = 0

    pattern.findAll(input).forEach { match ->
        if (match.range.first > lastIndex) {
            append(input.substring(lastIndex, match.range.first))
        }

        val boldText = match.groups[1]?.value
        val emphText = match.groups[2]?.value
        val codeText = match.groups[3]?.value

        when {
            boldText != null -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(boldText)
                pop()
            }
            emphText != null -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(emphText)
                pop()
            }
            codeText != null -> {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = GlassWhite8,
                        color = VioletBright,
                    )
                )
                append(codeText)
                pop()
            }
        }

        lastIndex = match.range.last + 1
    }

    if (lastIndex < input.length) {
        append(input.substring(lastIndex))
    }
}

// ── Typing dots ───────────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.padding(vertical = 2.dp),
    ) {
        repeat(3) { i ->
            val alpha by infiniteTransition.animateFloat(
                initialValue  = 0.25f,
                targetValue   = 1f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = i * 140, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse,
                ),
                label = "dot$i",
            )
            val scale by infiniteTransition.animateFloat(
                initialValue  = 0.8f,
                targetValue   = 1.2f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = i * 140, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse,
                ),
                label = "dot_scale$i",
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(CircleShape)
                    .background(VioletBright.copy(alpha = alpha))
            )
        }
    }
}

// ── Streaming cursor ──────────────────────────────────────────────────────────

@Composable
private fun StreamingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label         = "cursor_alpha",
    )
    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .size(width = 2.dp, height = 14.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(VioletBright.copy(alpha = alpha))
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// INPUT BAR — glassy floating bar
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ChatInputBar(
    text:         String,
    onTextChange: (String) -> Unit,
    onSend:       () -> Unit,
    onStop:       () -> Unit,
    isGenerating: Boolean,
    isLoading:    Boolean,
) {
    // Outer scrim fade — same as nav bar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, SpaceDeep.copy(alpha = 0.98f))
                )
            )
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier             = Modifier.fillMaxWidth(),
            verticalAlignment    = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Text field — glass pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (IsOrbitDarkTheme) SpaceDust.copy(alpha = 0.85f)
                        else Color(0xFFF0ECFF).copy(alpha = 0.82f)
                    )
                    .border(
                        width = if (IsOrbitDarkTheme) 0.5.dp else 1.dp,
                        brush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0.0f to (if (IsOrbitDarkTheme) Color.White else VioletCore)
                                             .copy(alpha = if (IsOrbitDarkTheme) 0.12f else 0.25f),
                                1.0f to VioletCore.copy(alpha = if (IsOrbitDarkTheme) 0.04f else 0.08f),
                            ),
                        ),
                        shape = RoundedCornerShape(22.dp),
                    ),
            ) {
                BasicChatTextField(
                    text         = text,
                    onTextChange = onTextChange,
                    onSend       = onSend,
                    isGenerating = isGenerating,
                    isLoading    = isLoading,
                )
            }

            // Send / Stop button
            if (isGenerating) {
                StopButton(onClick = onStop)
            } else {
                SendButton(
                    enabled   = text.trim().isNotEmpty() && !isLoading,
                    isLoading = isLoading,
                    onClick   = onSend,
                )
            }
        }
    }
}

@Composable
private fun BasicChatTextField(
    text:         String,
    onTextChange: (String) -> Unit,
    onSend:       () -> Unit,
    isGenerating: Boolean,
    isLoading:    Boolean,
) {
    TextField(
        value            = text,
        onValueChange    = onTextChange,
        modifier         = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        placeholder      = {
            Text(
                "Message…",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted,
            )
        },
        colors           = TextFieldDefaults.colors(
            focusedContainerColor    = Color.Transparent,
            unfocusedContainerColor  = Color.Transparent,
            focusedIndicatorColor    = Color.Transparent,
            unfocusedIndicatorColor  = Color.Transparent,
            focusedTextColor         = TextPrimary,
            unfocusedTextColor       = TextPrimary,
            cursorColor              = VioletBright,
        ),
        maxLines         = 6,
        keyboardOptions  = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction      = ImeAction.Default,
        ),
        textStyle        = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun SendButton(
    enabled:   Boolean,
    isLoading: Boolean,
    onClick:   () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) OrbitGradients.primaryButton
                else Brush.linearGradient(listOf(GlassWhite8, GlassWhite8))
            )
            .then(
                if (enabled) Modifier.glowBorder(VioletCore.copy(0.4f), 16.dp)
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                enabled           = enabled,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(20.dp),
                color       = VioletBright,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint     = if (enabled) Color.White else TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun StopButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DestructiveSoft)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Stop,
            contentDescription = "Stop generation",
            tint     = Destructive,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STATUS BANNER
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun GlassStatusBanner(text: String, color: Color) {
    val isDark = IsOrbitDarkTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = if (isDark) 0.10f else 0.08f))
            .border(
                width = 0.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(0.dp),
            )
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text  = text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// animateColorAsState alias (avoids import conflicts)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun animateColorAsState(
    targetValue:   androidx.compose.ui.graphics.Color,
    animationSpec: androidx.compose.animation.core.AnimationSpec<androidx.compose.ui.graphics.Color> = tween(200),
    label:         String,
): State<androidx.compose.ui.graphics.Color> =
    androidx.compose.animation.animateColorAsState(
        targetValue   = targetValue,
        animationSpec = animationSpec,
        label         = label,
    )