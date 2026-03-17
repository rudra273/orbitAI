package com.example.orbitai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.MemoryFeatureStore
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.MemoryViewModel
import java.text.SimpleDateFormat
import java.util.*

// Memory accent — teal (matches MemorySettingsScreen in Settings)
private val MemoryAccent    = Color(0xFF34D399)
private val MemoryAccentDim = Color(0xFF10B981)
private val MemoryFrost     = Color(0x1A34D399)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MEMORY SCREEN — used as Settings sub-screen
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel:   MemoryViewModel,
    memoryStore: MemoryFeatureStore,
    onBack:      () -> Unit,
) {
    val memories         by viewModel.memories.collectAsState()
    var enabled          by remember { mutableStateOf(memoryStore.isEnabled) }
    var showAddDialog    by remember { mutableStateOf(false) }
    var showClearDialog  by remember { mutableStateOf(false) }

    // Dialogs
    if (showAddDialog) {
        AddMemoryDialog(
            onConfirm = { text -> viewModel.addMemory(text); showAddDialog = false },
            onDismiss = { showAddDialog = false },
        )
    }

    if (showClearDialog) {
        ClearMemoriesDialog(
            onConfirm = { viewModel.clearAll(); showClearDialog = false },
            onDismiss = { showClearDialog = false },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        // Ambient teal glow — top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to MemoryAccent.copy(alpha = 0.05f),
                            1.0f to Color.Transparent,
                        ),
                        radius = 650f,
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
                                    .background(MemoryFrost),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    null,
                                    tint     = MemoryAccent,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    "Memory",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                )
                                Text(
                                    "${memories.size} stored",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color      = MemoryAccent,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }
                        }
                    },
                    actions = {
                        // Clear all — only shown when there are memories
                        if (memories.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DestructiveSoft)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication        = null,
                                    ) { showClearDialog = true }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment    = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        null,
                                        tint     = Destructive,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        "Clear all",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Destructive,
                                    )
                                }
                            }
                        }

                        // Add memory button
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MemoryFrost)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                ) { showAddDialog = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add memory",
                                tint     = MemoryAccent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
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
                    top    = 4.dp,
                    bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {

                // ── Enable toggle card ────────────────────────────────────
                item {
                    MemoryToggleCard(
                        enabled  = enabled,
                        onChange = { enabled = it; memoryStore.isEnabled = it },
                    )
                }

                // ── Section header ────────────────────────────────────────
                if (memories.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "STORED MEMORIES",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight    = FontWeight.Bold,
                            ),
                            color    = MemoryAccent.copy(0.7f),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }

                    itemsIndexed(
                        items = memories,
                        key   = { _, m -> m.id },
                    ) { index, memory ->
                        StaggeredFadeSlide(index = index) {
                            MemoryCard(
                                content   = memory.content,
                                source    = memory.source,
                                createdAt = memory.createdAt,
                                onDelete  = { viewModel.deleteMemory(memory.id) },
                            )
                        }
                    }
                } else {
                    item {
                        Spacer(Modifier.height(16.dp))
                        MemoryEmptyState(enabled = enabled)
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MEMORY TOGGLE CARD
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun MemoryToggleCard(
    enabled:  Boolean,
    onChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite8)
            .background(
                brush = Brush.linearGradient(
                    if (enabled)
                        listOf(MemoryAccent.copy(0.2f), MemoryAccent.copy(0.04f))
                    else
                        listOf(GlassBorder, GlassBorder.copy(0.03f))
                ),
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        Row(
            modifier             = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Status dot — pulses when enabled
                val infiniteTransition = rememberInfiniteTransition(label = "mem_pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue  = if (enabled) 0.4f else 0f,
                    targetValue   = if (enabled) 1f else 0f,
                    animationSpec = infiniteRepeatable(
                        tween(1400, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse,
                    ),
                    label = "pulse",
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .drawBehind {
                            drawIntoCanvas { canvas ->
                                if (enabled) {
                                    val paint = Paint().apply {
                                        asFrameworkPaint().apply {
                                            isAntiAlias = true
                                            color       = android.graphics.Color.TRANSPARENT
                                            setShadowLayer(
                                                8f, 0f, 0f,
                                                MemoryAccent.copy(pulseAlpha).toArgb(),
                                            )
                                        }
                                    }
                                    canvas.drawCircle(
                                        Offset(size.width / 2f, size.height / 2f),
                                        size.minDimension / 2f, paint,
                                    )
                                }
                            }
                        }
                        .clip(CircleShape)
                        .background(if (enabled) MemoryAccent else TextMuted.copy(0.3f)),
                )

                Column {
                    Text(
                        "Use Memory",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Text(
                        if (enabled) "OrbitAI remembers across chats"
                        else         "Memory is disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MemoryAccent else TextMuted,
                    )
                }
            }

            Switch(
                checked         = enabled,
                onCheckedChange = onChange,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor   = Color.White,
                    checkedTrackColor   = MemoryAccent,
                    uncheckedTrackColor = GlassWhite8,
                    uncheckedThumbColor = TextMuted,
                ),
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MEMORY CARD
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun MemoryCard(
    content:   String,
    source:    String,
    createdAt: Long,
    onDelete:  () -> Unit,
) {
    val dateStr = remember(createdAt) {
        SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(createdAt))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassWhite4)
            .background(
                brush = Brush.linearGradient(listOf(GlassBorder, GlassBorder.copy(0.02f))),
                shape = RoundedCornerShape(14.dp),
            ),
    ) {
        // Left teal stripe
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(2.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(MemoryAccent.copy(0.6f), MemoryAccentDim.copy(0.2f))
                    )
                )
        )

        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MemoryFrost),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint     = MemoryAccent,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = content,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = TextPrimary,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted.copy(0.6f),
                    )
                    // Auto badge
                    if (source == "auto") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MemoryFrost)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "auto",
                                style = MaterialTheme.typography.labelSmall,
                                color = MemoryAccent.copy(0.8f),
                            )
                        }
                    }
                }
            }

            // Delete
            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete memory",
                    tint     = TextMuted.copy(0.4f),
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// EMPTY STATE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun MemoryEmptyState(enabled: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mem_empty_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.08f,
        targetValue   = 0.28f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
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
                                    MemoryAccent.copy(alpha = glowAlpha).toArgb(),
                                )
                            }
                        }
                        canvas.drawCircle(
                            Offset(size.width / 2f, size.height / 2f),
                            size.minDimension / 2f, paint,
                        )
                    }
                }
                .clip(RoundedCornerShape(24.dp))
                .background(MemoryFrost),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint     = MemoryAccent,
                modifier = Modifier.size(38.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            if (enabled) "No memories yet" else "Memory is off",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            if (enabled)
                "Chat with OrbitAI and it will\nautomatically remember things about you"
            else
                "Enable memory above so OrbitAI can\nremember things across conversations",
            style     = MaterialTheme.typography.bodyMedium,
            color     = TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ADD MEMORY DIALOG
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun AddMemoryDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SpaceNebula,
        tonalElevation   = 0.dp,
        shape            = RoundedCornerShape(22.dp),
        title = {
            Text(
                "Add Memory",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Manually add something for OrbitAI to remember.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value         = text,
                    onValueChange = { text = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassWhite8),
                    placeholder   = {
                        Text(
                            "e.g. I prefer concise answers, I'm a Python developer…",
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
                        cursorColor             = MemoryAccent,
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    maxLines  = 6,
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (text.isNotBlank())
                            Brush.linearGradient(listOf(MemoryAccent, MemoryAccentDim))
                        else
                            Brush.linearGradient(listOf(GlassWhite8, GlassWhite8))
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        enabled           = text.isNotBlank(),
                    ) { if (text.isNotBlank()) onConfirm(text.trim()) }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Save",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = if (text.isNotBlank()) Color.White else TextMuted,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassWhite4)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onDismiss,
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                )
            }
        },
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CLEAR CONFIRMATION DIALOG
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ClearMemoriesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SpaceNebula,
        tonalElevation   = 0.dp,
        shape            = RoundedCornerShape(22.dp),
        title = {
            Text(
                "Clear all memories?",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
        },
        text = {
            Text(
                "This will permanently delete all stored memories. OrbitAI will start fresh with no context about you.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DestructiveSoft)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onConfirm,
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Clear all",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = Destructive,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassWhite4)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onDismiss,
                    )
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                )
            }
        },
    )
}