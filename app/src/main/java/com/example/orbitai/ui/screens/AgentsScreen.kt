package com.example.orbitai.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.db.Agent
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.AgentsViewModel

// Agents accent — teal/emerald
private val AgentsAccent    = Color(0xFF10B981)
private val AgentsAccentDim = Color(0xFF059669)
private val AgentsFrost     = Color(0x1A10B981)   // 10% teal glass fill

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// AGENTS SCREEN — list + inline edit destination
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private sealed interface AgentsDestination {
    data object List : AgentsDestination
    data class Edit(val agent: Agent?)  : AgentsDestination   // null = create new
}

@Composable
fun AgentsScreen(viewModel: AgentsViewModel) {
    val agents by viewModel.agents.collectAsState()
    var destination by remember { mutableStateOf<AgentsDestination>(AgentsDestination.List) }

    AnimatedContent(
        targetState   = destination,
        transitionSpec = {
            if (targetState is AgentsDestination.List) {
                (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "agents_nav",
    ) { dest ->
        when (dest) {
            is AgentsDestination.List -> AgentListScreen(
                agents      = agents,
                onEditAgent = { destination = AgentsDestination.Edit(it) },
                onDelete    = { viewModel.deleteAgent(it) },
                onCreateNew = { destination = AgentsDestination.Edit(null) },
            )
            is AgentsDestination.Edit -> AgentEditScreen(
                agent     = dest.agent,
                onBack    = { destination = AgentsDestination.List },
                onSave    = { name, prompt ->
                    if (dest.agent == null) {
                        viewModel.createAgent(name, prompt)
                    } else {
                        viewModel.updateAgent(dest.agent.id, name, prompt)
                    }
                    destination = AgentsDestination.List
                },
                onDelete  = {
                    dest.agent?.let { viewModel.deleteAgent(it.id) }
                    destination = AgentsDestination.List
                },
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// AGENT LIST
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentListScreen(
    agents:      List<Agent>,
    onEditAgent: (Agent) -> Unit,
    onDelete:    (String) -> Unit,
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
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to AgentsAccent.copy(alpha = 0.04f),
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
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                "Agents",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                            )
                            Text(
                                "${agents.size} agent${if (agents.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color        = AgentsAccent,
                                    letterSpacing = 1.sp,
                                    fontWeight   = FontWeight.SemiBold,
                                ),
                            )
                        }
                    },
                    colors   = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.statusBarsPadding(),
                )
            },
            floatingActionButton = {
                AgentsFAB(onClick = onCreateNew)
            },
        ) { padding ->
            if (agents.isEmpty()) {
                AgentsEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onCreate = onCreateNew,
                )
            } else {
                LazyColumn(
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
                ) {
                    itemsIndexed(
                        items = agents,
                        key   = { _, a -> a.id },
                    ) { index, agent ->
                        StaggeredFadeSlide(index = index) {
                            AgentCard(
                                agent    = agent,
                                onClick  = { onEditAgent(agent) },
                                onDelete = { onDelete(agent.id) },
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
private fun AgentsFAB(onClick: () -> Unit) {
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
                                AgentsAccent.copy(alpha = 0.4f).toArgb(),
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
                Brush.linearGradient(listOf(AgentsAccent, AgentsAccentDim))
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
            contentDescription = "Create Agent",
            tint     = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// AGENT CARD
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun AgentCard(
    agent:    Agent,
    onClick:  () -> Unit,
    onDelete: () -> Unit,
) {
    // Default agent gets violet, custom agents get teal
    val cardAccent = if (agent.isDefault) VioletCore else AgentsAccent
    val cardFrost  = if (agent.isDefault) VioletFrost else AgentsFrost

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            color       = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                20f, 0f, 2f,
                                cardAccent.copy(alpha = 0.1f).toArgb(),
                            )
                        }
                    }
                    canvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        16.dp.toPx(), 16.dp.toPx(), paint,
                    )
                }
            }
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite8)
            .background(
                brush = Brush.linearGradient(listOf(GlassBorder, GlassBorder.copy(0.03f))),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
    ) {
        // Left accent stripe
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(cardAccent.copy(0.7f), cardAccent.copy(0.2f))
                    )
                )
        )

        Row(
            modifier             = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment    = Alignment.CenterVertically,
        ) {
            // Avatar — initial letter or ✦ for default
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardFrost),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = if (agent.isDefault) "✦" else agent.name.take(1).uppercase(),
                    fontSize   = if (agent.isDefault) 20.sp else 18.sp,
                    color      = cardAccent,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        agent.name,
                        style     = MaterialTheme.typography.titleMedium,
                        color     = TextPrimary,
                    )
                    if (agent.isDefault) {
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
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    agent.systemPrompt,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(6.dp))

            // Delete — only for non-default agents, very subtle
            if (!agent.isDefault) {
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete agent",
                        tint     = TextMuted.copy(0.4f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint     = TextMuted.copy(0.35f),
                modifier = Modifier.size(12.dp),
            )

            Spacer(Modifier.width(4.dp))
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// AGENT EDIT SCREEN — full-screen, not a dialog
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentEditScreen(
    agent:    Agent?,           // null = creating new
    onBack:   () -> Unit,
    onSave:   (name: String, prompt: String) -> Unit,
    onDelete: () -> Unit,
) {
    var name   by remember { mutableStateOf(agent?.name ?: "") }
    var prompt by remember { mutableStateOf(agent?.systemPrompt ?: "") }
    val isNew  = agent == null
    val isDefault = agent?.isDefault == true

    val canSave = name.isNotBlank() && prompt.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        // Ambient teal glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to AgentsAccent.copy(alpha = 0.05f),
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
                                    .background(AgentsFrost),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.SmartToy,
                                    null,
                                    tint     = AgentsAccent,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Text(
                                if (isNew) "New Agent" else if (isDefault) "Edit Orbit" else "Edit Agent",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                            )
                        }
                    },
                    actions = {
                        // Save button — top right
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (canSave)
                                        Brush.linearGradient(listOf(AgentsAccent, AgentsAccentDim))
                                    else
                                        Brush.linearGradient(listOf(GlassWhite8, GlassWhite8))
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                    enabled           = canSave,
                                ) { if (canSave) onSave(name.trim(), prompt.trim()) }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Save",
                                style      = MaterialTheme.typography.labelLarge,
                                color      = if (canSave) Color.White else TextMuted,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    colors   = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.statusBarsPadding(),
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
                    AgentFieldLabel("Agent Name")
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
                                        listOf(AgentsAccent.copy(0.25f), AgentsAccent.copy(0.05f))
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
                            cursorColor             = AgentsAccent,
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
                    AgentFieldLabel("System Prompt")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Defines how this agent behaves and responds.",
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
                                        listOf(AgentsAccent.copy(0.2f), AgentsAccent.copy(0.03f))
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
                            cursorColor             = AgentsAccent,
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

                // Delete button — only for non-default existing agents
                if (!isNew && !isDefault) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DestructiveSoft)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                    onClick           = onDelete,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment    = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint     = Destructive,
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    "Delete Agent",
                                    style      = MaterialTheme.typography.titleSmall,
                                    color      = Destructive,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// EMPTY STATE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun AgentsEmptyState(
    modifier: Modifier = Modifier,
    onCreate: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "agent_pulse")
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
                                    AgentsAccent.copy(alpha = glowAlpha).toArgb(),
                                )
                            }
                        }
                        canvas.drawCircle(
                            androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                            size.minDimension / 2f, paint,
                        )
                    }
                }
                .clip(RoundedCornerShape(24.dp))
                .background(AgentsFrost),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint     = AgentsAccent,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "No agents yet",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Create custom agents with different\nsystem prompts for any purpose",
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
                    Brush.linearGradient(listOf(AgentsAccent, AgentsAccentDim))
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
                    "Create Agent",
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
private fun AgentFieldLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.5.sp,
            fontWeight    = FontWeight.Bold,
        ),
        color = AgentsAccent.copy(0.8f),
    )
}