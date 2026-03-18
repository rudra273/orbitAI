package com.example.orbitai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.orbitai.data.db.RagDocument
import com.example.orbitai.data.db.Space
import com.example.orbitai.ui.theme.GlassBorder
import com.example.orbitai.ui.theme.GlassWhite4
import com.example.orbitai.ui.theme.GlassWhite8
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.SpaceNebula
import com.example.orbitai.ui.theme.TextMuted
import com.example.orbitai.ui.theme.TextPrimary
import com.example.orbitai.viewmodel.SpacesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SpacesAccent = Color(0xFFFBBF24)
private val SpacesAccentDim = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacesScreen(
    viewModel: SpacesViewModel,
    onOpenSpace: (spaceId: String) -> Unit,
) {
    val spaces by viewModel.spaces.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedSpaceId by remember { mutableStateOf<String?>(null) }
    var pendingPickerSpaceId by remember { mutableStateOf<String?>(null) }

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val targetId = pendingPickerSpaceId
        if (uri != null && targetId != null) {
            viewModel.addDocumentToSpace(uri, targetId)
        }
        pendingPickerSpaceId = null
    }

    val launchPickerForSpace: (String) -> Unit = { spaceId ->
        pendingPickerSpaceId = spaceId
        documentPicker.launch(
            arrayOf(
                "application/pdf",
                "text/plain",
                "text/markdown",
                "text/csv",
                "text/x-markdown",
            )
        )
    }

    LaunchedEffect(spaces) {
        if (spaces.isEmpty()) {
            selectedSpaceId = null
        } else if (selectedSpaceId == null || spaces.none { it.id == selectedSpaceId }) {
            selectedSpaceId = spaces.first().id
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to SpacesAccent.copy(alpha = 0.07f),
                            0.5f to SpacesAccent.copy(alpha = 0.02f),
                            1.0f to Color.Transparent,
                        ),
                        radius = 800f,
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                "Spaces",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                            )
                            Text(
                                "${spaces.size} space${if (spaces.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = SpacesAccent,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(top = 4.dp),
                )
            },
            floatingActionButton = { SpacesFAB(onClick = { showCreateDialog = true }) },
        ) { padding ->
            if (spaces.isEmpty()) {
                SpacesEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onCreate = { showCreateDialog = true },
                )
            } else {
                val selectedSpace = spaces.find { it.id == selectedSpaceId } ?: spaces.first()
                val docs by remember(selectedSpace.id) {
                    viewModel.observeDocumentsInSpace(selectedSpace.id)
                }.collectAsState(initial = emptyList())

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SpaceSelectorRail(
                        spaces = spaces,
                        selectedSpaceId = selectedSpace.id,
                        onSelect = { selectedSpaceId = it },
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                    )
                    SpacePreviewCard(
                        space = selectedSpace,
                        docs = docs,
                        onOpenSpace = { onOpenSpace(selectedSpace.id) },
                        onAddDoc = { launchPickerForSpace(selectedSpace.id) },
                        modifier = Modifier
                            .weight(4f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSpaceDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createSpace(name)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun SpacesFAB(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(24f, 0f, 4f, SpacesAccent.copy(alpha = 0.4f).toArgb())
                        }
                    }
                    canvas.drawRoundRect(0f, 0f, size.width, size.height, 18.dp.toPx(), 18.dp.toPx(), paint)
                }
            }
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(SpacesAccent, SpacesAccentDim)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Create Space",
            tint = SpaceDeep,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun SpaceSelectorRail(
    spaces: List<Space>,
    selectedSpaceId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "MY SPACES",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = TextMuted.copy(alpha = 0.50f),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            itemsIndexed(spaces, key = { _, s -> s.id }) { _, space ->
                SpaceSelectorItem(
                    space = space,
                    selected = space.id == selectedSpaceId,
                    onClick = { onSelect(space.id) },
                )
            }
        }
    }
}

@Composable
private fun SpaceSelectorItem(
    space: Space,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(listOf(SpacesAccent, SpacesAccentDim))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Text(
            text = space.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) SpaceDeep else TextPrimary.copy(alpha = 0.72f),
            maxLines = 1,
        )
    }
}

@Composable
private fun SpaceDocRow(doc: RagDocument) {
    val isDark = IsOrbitDarkTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = if (isDark) 0.05f else 0.04f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SpacesAccent.copy(alpha = if (isDark) 0.13f else 0.11f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                tint = SpacesAccent,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = doc.name,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary.copy(alpha = 0.85f),
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SpacePreviewCard(
    space: Space,
    docs: List<RagDocument>,
    onOpenSpace: () -> Unit,
    onAddDoc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = IsOrbitDarkTheme
    val dateStr = remember(space.createdAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(space.createdAt))
    }
    val daysOld = remember(space.createdAt) {
        ((System.currentTimeMillis() - space.createdAt).coerceAtLeast(0L) / (24L * 60L * 60L * 1000L)).toInt()
    }

    val cardShape = RoundedCornerShape(18.dp)
    val initial = space.name.firstOrNull()?.uppercaseChar()?.toString() ?: "S"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            // Glassy base — dark: white shimmer, light: warm amber tint
            .background(
                if (isDark) Color.White.copy(alpha = 0.06f)
                else SpacesAccent.copy(alpha = 0.08f)
            )
            // Frosted highlight sweep on top
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.White.copy(alpha = if (isDark) 0.10f else 0.40f),
                        0.30f to Color.White.copy(alpha = if (isDark) 0.02f else 0.10f),
                        1.0f to Color.Transparent,
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to SpacesAccent.copy(alpha = if (isDark) 0.35f else 0.50f),
                        0.5f to SpacesAccent.copy(alpha = if (isDark) 0.12f else 0.22f),
                        1.0f to Color.White.copy(alpha = if (isDark) 0.08f else 0.20f),
                    )
                ),
                shape = cardShape,
            ),
    ) {
        // ── Amber gradient header ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to SpacesAccentDim.copy(alpha = 0.90f),
                            1.0f to SpacesAccent.copy(alpha = 0.75f),
                        ),
                    )
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.Black.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                }
                Column {
                    Text(
                        text = space.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = SpaceDeep,
                        maxLines = 1,
                    )
                    Text(
                        text = "Knowledge Space",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = SpaceDeep.copy(alpha = 0.55f),
                    )
                }
            }
        }

        // ── Meta info row ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Date chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SpacesAccent.copy(alpha = if (isDark) 0.10f else 0.08f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = SpacesAccent,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = SpacesAccent,
                )
            }
            // Age chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = if (isDark) 0.05f else 0.04f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = if (daysOld == 0) "Today" else "$daysOld day${if (daysOld == 1) "" else "s"} old",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }

        // ── Divider ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SpacesAccent.copy(alpha = if (isDark) 0.08f else 0.10f))
                .padding(horizontal = 14.dp),
        )

        // ── Documents ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (docs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = SpacesAccent.copy(alpha = 0.22f),
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = "No documents yet\nTap \"+Add Doc\" to get started",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                docs.take(4).forEach { doc ->
                    SpaceDocRow(doc = doc)
                }
                if (docs.size > 4) {
                    Text(
                        text = "+${docs.size - 4} more  •  Open to see all",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted.copy(alpha = 0.45f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    )
                }
            }
        }

        // ── Action buttons ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(SpacesAccent, SpacesAccentDim)))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenSpace,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Open",
                        style = MaterialTheme.typography.titleSmall,
                        color = SpaceDeep,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = SpaceDeep,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) GlassWhite8 else Color.White.copy(alpha = 0.07f))
                    .border(
                        width = 1.dp,
                        color = SpacesAccent.copy(alpha = if (isDark) 0.30f else 0.28f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddDoc,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = SpacesAccent,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Add Doc",
                        style = MaterialTheme.typography.titleSmall,
                        color = SpacesAccent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateSpaceDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val isDark = IsOrbitDarkTheme
    val dialogShape = RoundedCornerShape(22.dp)
    val inputShape = RoundedCornerShape(14.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) SpaceNebula else Color.White.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shape = dialogShape,
        modifier = Modifier.border(
            width = if (isDark) 1.dp else 1.5.dp,
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to SpacesAccent.copy(alpha = if (isDark) 0.30f else 0.40f),
                    0.6f to SpacesAccent.copy(alpha = if (isDark) 0.08f else 0.15f),
                    1.0f to (if (isDark) Color.White else SpacesAccent)
                        .copy(alpha = if (isDark) 0.04f else 0.08f),
                ),
            ),
            shape = dialogShape,
        ),
        title = {
            Text(
                "New Space",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Give your space a name to organise documents.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(inputShape)
                        .background(if (isDark) GlassWhite8 else Color.White.copy(alpha = 0.70f))
                        .border(
                            width = 0.5.dp,
                            color = SpacesAccent.copy(alpha = if (isDark) 0.15f else 0.25f),
                            shape = inputShape,
                        ),
                    placeholder = {
                        Text(
                            "e.g. Research, Work, Books...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = SpacesAccent,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (name.isNotBlank()) {
                            Brush.linearGradient(listOf(SpacesAccent, SpacesAccentDim))
                        } else {
                            Brush.linearGradient(listOf(GlassWhite8, GlassWhite8))
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = name.isNotBlank(),
                    ) {
                        if (name.isNotBlank()) {
                            onCreate(name)
                        }
                    }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Create",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (name.isNotBlank()) SpaceDeep else TextMuted,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            val cancelShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(cancelShape)
                    .background(if (isDark) GlassWhite4 else Color.White.copy(alpha = 0.60f))
                    .border(width = 0.5.dp, color = GlassBorder, shape = cancelShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
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

@Composable
private fun SpacesEmptyState(
    modifier: Modifier = Modifier,
    onCreate: () -> Unit,
) {
    val isDark = IsOrbitDarkTheme
    val infiniteTransition = rememberInfiniteTransition(label = "spaces_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    val iconShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier,
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
                                color = android.graphics.Color.TRANSPARENT
                                setShadowLayer(44f, 0f, 0f, SpacesAccent.copy(alpha = glowAlpha).toArgb())
                            }
                        }
                        canvas.drawCircle(Offset(size.width / 2f, size.height / 2f), size.minDimension / 2f, paint)
                    }
                }
                .clip(iconShape)
                .background(if (isDark) SpacesAccent.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.80f))
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
                            0.0f to SpacesAccent.copy(alpha = if (isDark) 0.35f else 0.45f),
                            1.0f to SpacesAccent.copy(alpha = if (isDark) 0.08f else 0.15f),
                        ),
                    ),
                    shape = iconShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = SpacesAccent, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(28.dp))

        Text("No spaces yet", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)

        Spacer(Modifier.height(8.dp))

        Text(
            "Create a space to organise your\ndocuments for context-aware chats",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        Box(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(SpacesAccent, SpacesAccentDim)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCreate,
                )
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = SpaceDeep, modifier = Modifier.size(18.dp))
                Text(
                    "Create Space",
                    style = MaterialTheme.typography.titleMedium,
                    color = SpaceDeep,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
