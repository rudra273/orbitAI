package com.example.orbitai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.AVAILABLE_MODELS
import com.example.orbitai.data.Chat
import com.example.orbitai.data.Message
import com.example.orbitai.data.Role
import com.example.orbitai.data.db.Agent
import com.example.orbitai.data.db.Space
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
) {
    val chats by viewModel.chats.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val chat = chats.find { it.id == chatId }
    val spaces by viewModel.spaces.collectAsState()
    val activeSpaceIds by viewModel.activeSpaceIds.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val activeAgentId by viewModel.activeAgentId.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll on new messages
    val messages = chat?.messages ?: emptyList()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = Void,
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextMuted,
                            )
                        }
                    },
                    title = {
                        Text(
                            chat?.title ?: "Chat",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Void),
                )
                // Model selector row
                if (chat != null) {
                    ModelSelectorBar(
                        chat = chat,
                        onModelSelected = { model ->
                            viewModel.selectModel(chatId, model)
                        }
                    )
                }
                // Agent selector row
                if (agents.isNotEmpty()) {
                    AgentSelectorRow(
                        agents        = agents,
                        activeAgentId = activeAgentId,
                        onSelectAgent = { viewModel.selectAgent(it) },
                    )
                }
                // Space selector row (only shown when spaces exist)
                if (spaces.isNotEmpty()) {
                    SpaceSelectorRow(
                        spaces         = spaces,
                        activeSpaceIds = activeSpaceIds,
                        onToggleSpace  = { viewModel.toggleSpace(it) },
                    )
                }
                HorizontalDivider(color = Outline, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    val text = inputText.trim()
                    if (text.isNotEmpty() && !uiState.isGenerating && !uiState.isModelLoading) {
                        inputText = ""
                        viewModel.sendMessage(chatId, text)
                        scope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.lastIndex)
                            }
                        }
                    }
                },
                onStop = { viewModel.stopGeneration() },
                isGenerating = uiState.isGenerating,
                isLoading = uiState.isModelLoading,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status banners
            if (uiState.isModelLoading) {
                StatusBanner("Loading model…", CyanCore)
            }
            uiState.loadError?.let {
                StatusBanner(it, MaterialTheme.colorScheme.error)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg)
                }
            }
        }
    }
}

// ── Model Selector ────────────────────────────────────────────────────────────

@Composable
private fun ModelSelectorBar(chat: Chat, onModelSelected: (com.example.orbitai.data.LlmModel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedModel = AVAILABLE_MODELS.find { it.id == chat.modelId } ?: AVAILABLE_MODELS.first()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        TextButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.textButtonColors(contentColor = CyanCore),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CyanCore.copy(alpha = 0.08f)),
        ) {
            Text(
                selectedModel.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "Select model",
                modifier = Modifier.size(16.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Surface1,
        ) {
            AVAILABLE_MODELS.forEach { model ->
                val isSelected = model.id == chat.modelId
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                model.displayName,
                                color = if (isSelected) CyanCore else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            Text(
                                model.description,
                                color = TextMuted,
                                fontSize = 12.sp,
                            )
                        }
                    },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    },
                    trailingIcon = if (isSelected) ({
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyanCore)
                        )
                    }) else null,
                )
            }
        }
    }
}

// ── Message Bubble ────────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(msg: Message) {
    val isUser = msg.role == Role.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            // AI avatar dot
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(CyanCore.copy(0.3f), AiAccent.copy(0.2f)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("✦", fontSize = 12.sp, color = CyanCore)
            }
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 18.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp,
                        )
                    )
                    .background(if (isUser) UserBubble else AiBubble)
                    .then(
                        if (isUser) Modifier.background(
                            Brush.linearGradient(listOf(CyanCore.copy(0.18f), Color.Transparent))
                        ) else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (msg.isStreaming && msg.content.isEmpty()) {
                    TypingIndicator()
                } else {
                    Text(
                        msg.content,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontFamily = if (!isUser) FontFamily.Default else FontFamily.Default,
                    )
                    if (msg.isStreaming) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyanCore)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(600, delayMillis = i * 150),
                    RepeatMode.Reverse,
                ),
                label = "dot$i"
            )
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(CyanCore.copy(alpha = alpha))
            )
        }
    }
}

// ── Input Bar ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isGenerating: Boolean,
    isLoading: Boolean,
) {
    Surface(color = Void) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Message…", color = TextMuted, fontSize = 15.sp)
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Surface2,
                    unfocusedContainerColor = Surface2,
                    focusedBorderColor = CyanCore.copy(0.6f),
                    unfocusedBorderColor = Outline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanCore,
                ),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
            )

            Spacer(Modifier.width(10.dp))

            if (isGenerating) {
                // Stop button
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop generation",
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                val canSend = text.trim().isNotEmpty() && !isLoading
                FilledIconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (canSend) CyanCore else Surface2,
                        contentColor = if (canSend) Void else TextMuted,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CyanCore,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

// ── Status Banner ─────────────────────────────────────────────────────────────

@Composable
private fun StatusBanner(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Agent Selector ────────────────────────────────────────────────────────────

@Composable
private fun AgentSelectorRow(
    agents: List<Agent>,
    activeAgentId: String,
    onSelectAgent: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val active = agents.find { it.id == activeAgentId } ?: agents.firstOrNull() ?: return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
    ) {
        TextButton(
            onClick        = { expanded = true },
            colors         = ButtonDefaults.textButtonColors(contentColor = AiAccent),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier       = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AiAccent.copy(alpha = 0.08f)),
        ) {
            Text(
                "✦  ${active.name}",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "Select agent",
                modifier = Modifier.size(16.dp),
            )
        }

        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            containerColor   = Surface1,
        ) {
            agents.forEach { agent ->
                val isSelected = agent.id == activeAgentId
                DropdownMenuItem(
                    text = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    agent.name,
                                    color      = if (isSelected) AiAccent else TextPrimary,
                                    fontSize   = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                if (agent.isDefault) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "default",
                                        color    = TextMuted,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                            Text(
                                agent.systemPrompt,
                                color    = TextMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                    },
                    onClick = {
                        onSelectAgent(agent.id)
                        expanded = false
                    },
                    trailingIcon = if (isSelected) ({
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(AiAccent)
                        )
                    }) else null,
                )
            }
        }
    }
}

// ── Space Selector ────────────────────────────────────────────────────────────

@Composable
private fun SpaceSelectorRow(
    spaces: List<Space>,
    activeSpaceIds: Set<String>,
    onToggleSpace: (String) -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Spaces:",
            color    = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(end = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(spaces, key = { it.id }) { space ->
                val selected = space.id in activeSpaceIds
                FilterChip(
                    selected = selected,
                    onClick  = { onToggleSpace(space.id) },
                    label    = {
                        Text(space.name, fontSize = 11.sp)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanCore.copy(alpha = 0.15f),
                        selectedLabelColor     = CyanCore,
                        containerColor         = Surface2,
                        labelColor             = TextMuted,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        selected            = selected,
                        enabled             = true,
                        selectedBorderColor = CyanCore.copy(alpha = 0.5f),
                        borderColor         = Outline,
                    ),
                    shape  = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(28.dp),
                )
            }
        }
    }
}