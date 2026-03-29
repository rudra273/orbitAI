package com.example.orbitai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.painterResource
import com.example.orbitai.R
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
import com.example.orbitai.data.Chat
import com.example.orbitai.data.LlmModel
import com.example.orbitai.data.Message
import com.example.orbitai.data.Role
import com.example.orbitai.data.SUPPORTED_DOCUMENT_MIME_TYPES
import com.example.orbitai.data.extractDocumentText
import com.example.orbitai.data.isImageDocument
import com.example.orbitai.data.normalizeDocumentMimeType
import com.example.orbitai.data.db.Mode
import com.example.orbitai.data.db.Space
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.ChatUiEvent
import com.example.orbitai.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private data class PendingAttachment(
    val name: String,
    val promptText: String,
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CHAT SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId:    String,
    viewModel: ChatViewModel,
    onBack:    () -> Unit,
    onNavigateToSettings: () -> Unit = {},
) {
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onContactsPermissionResult(granted)
    }
    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationsPermissionResult(granted)
    }
    val chats          by viewModel.chats.collectAsState()
    val uiState        by viewModel.uiState.collectAsState()
    val spaces         by viewModel.spaces.collectAsState()
    val activeSpaceIds by viewModel.activeSpaceIds.collectAsState()
    val modes          by viewModel.modes.collectAsState()
    val activeModeId   by viewModel.activeModeId.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()

    val chat     = chats.find { it.id == chatId }
    val messages = chat?.messages ?: emptyList()
    val selectedModel = remember(chat?.modelId, availableModels) {
        availableModels.find { it.id == chat?.modelId } ?: availableModels.firstOrNull()
    }

    var inputText by remember { mutableStateOf("") }
    var pendingAttachment by remember { mutableStateOf<PendingAttachment?>(null) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
    var attachmentInfo by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()
    val context = LocalContext.current

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val model = selectedModel
        if (model == null || !model.supportsDocumentAttachments()) {
            pendingAttachment = null
            attachmentInfo = null
            attachmentError = "This model doesn't support file attachments yet. Use Gemma 3 or Gemini."
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val attachment = readAttachmentForPrompt(context, uri)
            if (attachment == null) {
                pendingAttachment = null
                attachmentInfo = null
                val mimeType = normalizeDocumentMimeType(
                    context.contentResolver.getType(uri),
                    "file"
                )
                attachmentError =
                    if (isImageDocument(mimeType)) {
                        "Image picking is visible now, but true image analysis is not wired into this chat engine yet."
                    } else {
                        "Couldn't read that file. Try PDF, TXT, Markdown, CSV, JSON, XML, HTML, or an image."
                    }
            } else {
                pendingAttachment = attachment
                attachmentError = null
                attachmentInfo = "${attachment.name} attached"
            }
        }
    }

    BackHandler(onBack = onBack)

    LaunchedEffect(viewModel) {
        viewModel.refreshAvailableModels()
        viewModel.events.collect { event ->
            when (event) {
                ChatUiEvent.RequestContactsPermission -> {
                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
                ChatUiEvent.RequestNotificationsPermission -> {
                    notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ChatTopBar(
                    chat          = chat,
                    modes         = modes,
                    activeModeId  = activeModeId,
                    spaces        = spaces,
                    activeSpaceIds = activeSpaceIds,
                    availableModels = availableModels,
                    onBack         = onBack,
                    onModelSelected = { model -> viewModel.selectModel(chatId, model) },
                    onSelectMode    = { viewModel.selectMode(it) },
                    onToggleSpace   = { viewModel.toggleSpace(it) },
                    onNavigateToSettings = onNavigateToSettings,
                )
            },
            bottomBar = {
                ChatInputBar(
                    text         = inputText,
                    onTextChange = { inputText = it },
                    onSend       = {
                        val text = inputText.trim()
                        val finalPrompt = buildString {
                            if (text.isNotEmpty()) append(text)
                            pendingAttachment?.let { attachment ->
                                if (isNotEmpty()) append("\n\n")
                                append(attachment.promptText)
                            }
                        }.trim()

                        if (finalPrompt.isNotEmpty() && !uiState.isGenerating && !uiState.isModelLoading) {
                            inputText = ""
                            pendingAttachment = null
                            attachmentInfo = null
                            attachmentError = null
                            viewModel.sendMessage(chatId, finalPrompt)
                            scope.launch {
                                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                            }
                        }
                    },
                    onStop       = { viewModel.stopGeneration() },
                    isGenerating = uiState.isGenerating,
                    isLoading    = uiState.isModelLoading,
                    selectedModelSupportsAttachments = selectedModel?.supportsDocumentAttachments() == true,
                    pendingAttachment = pendingAttachment,
                    onPickAttachment = {
                        attachmentError = null
                        attachmentInfo = null
                        if (selectedModel?.supportsDocumentAttachments() == true) {
                            documentPicker.launch(SUPPORTED_DOCUMENT_MIME_TYPES)
                        } else {
                            pendingAttachment = null
                            attachmentError = "Use a different model for file upload. Gemma 3 and Gemini work better here."
                        }
                    },
                    onClearAttachment = {
                        pendingAttachment = null
                        attachmentInfo = null
                    },
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
                attachmentInfo?.let {
                    GlassStatusBanner(it, Warning)
                }
                attachmentError?.let {
                    GlassStatusBanner(it, Warning)
                }
                uiState.infoMessage?.let {
                    GlassStatusBanner(it, Color(0xFF34D399))
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
                            Image(
                                painter            = painterResource(R.drawable.vector_logo),
                                contentDescription = "OrbitAI",
                                modifier           = Modifier.size(64.dp),
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text       = "How can I help you today?",
                                style      = MaterialTheme.typography.headlineMedium,
                                color      = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text  = "Ask anything, generate code, or explore ideas.",
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
// TOP BAR — OrbitAI header + context strip
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    chat:           Chat?,
    modes:          List<Mode>,
    activeModeId:   String,
    spaces:         List<Space>,
    activeSpaceIds: Set<String>,
    availableModels: List<LlmModel>,
    onBack:         () -> Unit,
    onModelSelected: (LlmModel) -> Unit,
    onSelectMode:   (String) -> Unit,
    onToggleSpace:  (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
) {
    val activeMode = modes.find { it.id == activeModeId } ?: modes.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceDeep),
    ) {
        // ── Row 1: back + OrbitAI title + on-device pill ──────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "OrbitAI",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize      = 22.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = TextPrimary,
                )
                Text(
                    text  = "COMMAND CENTER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize      = 10.sp,
                        letterSpacing = 1.5.sp,
                    ),
                    color = TextMuted,
                )
            }

            // On-device / model status pill
            if (chat != null && availableModels.isNotEmpty()) {
                OnDeviceModelPill(
                    chat            = chat,
                    availableModels = availableModels,
                    onModelSelected = onModelSelected,
                )
            }

            Spacer(Modifier.width(4.dp))
        }

        // ── Row 2: context strip — mode chip + space chips ─────────────────
        if (modes.isNotEmpty() || spaces.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (modes.isNotEmpty() && activeMode != null) {
                    ModeDropdownChip(
                        modes        = modes,
                        activeMode   = activeMode,
                        activeModeId = activeModeId,
                        onSelectMode = onSelectMode,
                    )
                }
                spaces.forEach { space ->
                    SpaceToggleChip(
                        space         = space,
                        selected      = space.id in activeSpaceIds,
                        onToggleSpace = onToggleSpace,
                    )
                }
            }
        }

        // Bottom border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GlassBorder),
        )
    }
}

// ── On-device model pill ───────────────────────────────────────────────────────

@Composable
private fun OnDeviceModelPill(
    chat:            Chat,
    availableModels: List<LlmModel>,
    onModelSelected: (LlmModel) -> Unit,
) {
    if (availableModels.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val selectedModel = availableModels.find { it.id == chat.modelId }
    val isLocal = selectedModel?.provider == com.example.orbitai.data.ModelProvider.LOCAL

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(OnDevicePillBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(OnDevicePillText),
            )
            Text(
                text  = if (isLocal) "On-device" else selectedModel?.displayName ?: "Cloud",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = OnDevicePillText,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "Select model",
                tint     = OnDevicePillText.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp),
            )
        }

        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            containerColor   = SpaceNebula,
        ) {
            availableModels.forEach { model ->
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
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SpaceDust)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Outlined.Layers,
                contentDescription = null,
                tint     = TextSecondary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                activeMode.name,
                style      = MaterialTheme.typography.labelMedium,
                color      = TextPrimary,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "Switch mode",
                tint     = TextMuted,
                modifier = Modifier.size(14.dp),
            )
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
                                    color      = if (isSelected) VioletCore else TextPrimary,
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
                                            color = VioletCore,
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
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = VioletCore,
                            modifier = Modifier.size(16.dp),
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
        targetValue   = if (selected) VioletGlow else SpaceDust,
        animationSpec = tween(200),
        label         = "space_chip_bg",
    )
    val textColor by animateColorAsState(
        targetValue   = if (selected) VioletCore else TextSecondary,
        animationSpec = tween(200),
        label         = "space_chip_text",
    )
    val borderColor by animateColorAsState(
        targetValue   = if (selected) VioletFrost else Color.Transparent,
        animationSpec = tween(200),
        label         = "space_chip_border",
    )

    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
            ) { onToggleSpace(space.id) }
            .padding(horizontal = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(VioletCore),
        )
        Text(
            text  = space.name,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint     = VioletCore,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MESSAGE BUBBLE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun MessageBubble(msg: Message) {
    val isUser = msg.role == Role.USER

    if (isUser) {
        // User: right-aligned ink bubble
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 296.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart    = 20.dp,
                            topEnd      = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd   = 4.dp,
                        )
                    )
                    .background(UserBubbleFill)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                SelectionContainer {
                    MarkdownMessageText(
                        content   = msg.content,
                        textColor = if (IsOrbitDarkTheme) Color(0xFF141413) else Color.White,
                    )
                }
            }
        }
    } else {
        // AI: no bubble — ORBIT badge header + content directly on background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        ) {
            // ORBIT badge row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.padding(bottom = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(VioletGlow)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Text(
                        text  = "ORBIT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily    = FontFamily.Monospace,
                            fontSize      = 9.sp,
                            letterSpacing = 2.sp,
                            fontWeight    = FontWeight.Medium,
                        ),
                        color = VioletCore,
                    )
                }
            }

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

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
}

@Composable
private fun MarkdownMessageText(
    content:   String,
    textColor: Color = TextPrimary,
) {
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
                        text      = parseInlineMarkdown(block.text),
                        style     = headingStyle,
                        color     = textColor,
                        lineHeight = (headingStyle.fontSize.value + 8).sp,
                    )
                }

                is MarkdownBlock.Bullet -> {
                    Row(
                        verticalAlignment     = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text       = "•",
                            style      = MaterialTheme.typography.bodyLarge,
                            color      = textColor,
                            lineHeight = 25.sp,
                        )
                        Text(
                            text       = parseInlineMarkdown(block.text),
                            style      = MaterialTheme.typography.bodyLarge,
                            color      = textColor,
                            lineHeight = 25.sp,
                        )
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text       = parseInlineMarkdown(block.text),
                        style      = MaterialTheme.typography.bodyLarge,
                        color      = textColor,
                        lineHeight = 25.sp,
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

    val codeBlockBg   = if (IsOrbitDarkTheme) Color(0xFF1A1A18) else Color(0xFFF0EFE9)
    val codeHeaderBg  = if (IsOrbitDarkTheme) Color(0xFF141413) else Color(0xFFE8E7E1)
    val codeTextColor = if (IsOrbitDarkTheme) Color(0xFFE8E6E1) else Color(0xFF1A1A1A)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(codeBlockBg)
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(10.dp),
            ),
    ) {
        // Header: language + copy
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(codeHeaderBg)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = language.ifBlank { "code" },
                style      = MaterialTheme.typography.labelSmall,
                color      = TextMuted,
                fontFamily = FontFamily.Monospace,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (copied) Success.copy(0.15f) else GlassWhite8)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = if (copied) "Copied!" else "Copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (copied) Success else TextMuted,
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
                    text     = code,
                    style    = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp,
                    ),
                    color    = codeTextColor,
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
                        background = SpaceDust,
                        color      = VioletCore,
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
                    .background(TextMuted.copy(alpha = alpha))
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
            .background(TextMuted.copy(alpha = alpha))
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// INPUT BAR — glassy floating bar
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private enum class VoiceState { Idle, Listening }

@Composable
private fun rememberVoiceInput(onTextChange: (String) -> Unit): Pair<VoiceState, () -> Unit> {
    val context  = LocalContext.current
    val callback by rememberUpdatedState(onTextChange)
    var state    by remember { mutableStateOf(VoiceState.Idle) }
    val recognizer    = remember(context) { SpeechRecognizer.createSpeechRecognizer(context) }
    // Flag to distinguish user-requested stop vs natural end-of-speech pause
    val active        = remember { booleanArrayOf(false) }
    // Accumulates confirmed sentences across auto-restarts
    val confirmedText = remember { StringBuilder() }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            confirmedText.clear()
            active[0] = true
            startListening()
        }
    }

    DisposableEffect(recognizer) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { state = VoiceState.Listening }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.takeIf { it.isNotBlank() } ?: return
                // Show confirmed sentences + current partial
                callback(confirmedText.toString() + partial)
            }
            override fun onResults(results: Bundle?) {
                val result = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.takeIf { it.isNotBlank() }
                if (result != null) {
                    confirmedText.append(result).append(" ")
                    callback(confirmedText.toString().trimEnd())
                }
                // Auto-restart so pauses don't end the session —
                // only stop when the user taps the button (active[0] = false)
                if (active[0]) startListening()
            }
            override fun onError(error: Int) {
                // On transient errors while still active, restart silently
                if (active[0]) startListening() else state = VoiceState.Idle
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { recognizer.destroy() }
    }

    val toggle: () -> Unit = {
        if (active[0]) {
            active[0] = false
            recognizer.stopListening()
            recognizer.cancel()
            state = VoiceState.Idle
            confirmedText.clear()
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                confirmedText.clear()
                active[0] = true
                startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    return state to toggle
}

@Composable
private fun ChatInputBar(
    text:         String,
    onTextChange: (String) -> Unit,
    onSend:       () -> Unit,
    onStop:       () -> Unit,
    isGenerating: Boolean,
    isLoading:    Boolean,
    selectedModelSupportsAttachments: Boolean,
    pendingAttachment: PendingAttachment?,
    onPickAttachment: () -> Unit,
    onClearAttachment: () -> Unit,
) {
    val (voiceState, toggleVoice) = rememberVoiceInput(onTextChange = onTextChange)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceDeep)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Pending attachment preview
        pendingAttachment?.let { attachment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpaceDust)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = null,
                    tint     = TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text     = attachment.name,
                    color    = TextPrimary,
                    style    = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove attachment",
                    tint     = TextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onClearAttachment,
                        ),
                )
            }
        }

        // Input row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Warm rounded input field
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SpaceDust)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Attach icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onPickAttachment,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Attach file",
                        tint     = if (selectedModelSupportsAttachments) TextSecondary else TextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Text field
                BasicTextField(
                    value         = text,
                    onValueChange = onTextChange,
                    modifier      = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    textStyle     = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush   = SolidColor(TextPrimary),
                    singleLine    = false,
                    maxLines      = 5,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Default,
                    ),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    if (voiceState == VoiceState.Listening) "Listening…" else "Ask anything…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextMuted,
                                )
                            }
                            inner()
                        }
                    },
                )

                // Mic button (inside bar, right side)
                if (voiceState != VoiceState.Listening && text.isEmpty() && pendingAttachment == null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                                onClick           = toggleVoice,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint     = TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Send / Stop / Mic-active button
            when {
                isGenerating -> StopButton(onClick = onStop)
                voiceState == VoiceState.Listening -> MicButton(isListening = true, onClick = toggleVoice)
                text.trim().isNotEmpty() || pendingAttachment != null ->
                    SendButton(enabled = !isLoading, isLoading = isLoading, onClick = onSend)
                else -> {
                    // No button — mic is embedded inside field; no external button needed
                }
            }
        }
    }
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
            .clip(CircleShape)
            .background(if (enabled) TextPrimary else SpaceDust)
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
                color       = SpaceDeep,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint     = if (enabled) SpaceDeep else TextMuted,
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
            .clip(CircleShape)
            .background(DestructiveSoft)
            .border(1.dp, Destructive.copy(alpha = 0.3f), CircleShape)
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

@Composable
private fun MicButton(isListening: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = if (isListening) 1.10f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic_scale",
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(if (isListening) Destructive else TextPrimary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = if (isListening) "Stop voice input" else "Voice input",
            tint     = SpaceDeep,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STATUS BANNER
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun GlassStatusBanner(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
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

private fun LlmModel.supportsDocumentAttachments(): Boolean {
    val normalizedId = id.lowercase()
    return provider == com.example.orbitai.data.ModelProvider.GEMINI ||
        normalizedId.startsWith("gemma3") ||
        normalizedId.startsWith("gemma-3")
}

private suspend fun readAttachmentForPrompt(
    context: android.content.Context,
    uri: Uri,
): PendingAttachment? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val name = resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        if (nameIndex >= 0) cursor.getString(nameIndex) else null
    } ?: "document"
    val mimeType = normalizeDocumentMimeType(resolver.getType(uri), name)
    val content = extractDocumentText(context, uri, mimeType)
        .replace("\r\n", "\n")
        .trim()
        .take(6000)

    if (content.isBlank()) return@withContext null

    PendingAttachment(
        name = name,
        promptText = buildString {
            append("Attached file: ").append(name).append('\n')
            append("Please use the file below while answering.\n\n")
            append(content)
        }
    )
}
