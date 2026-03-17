package com.example.orbitai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.db.Space
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.SpacesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Spaces accent — warm amber to differentiate from Chat's violet
private val SpacesAccent  = Color(0xFFFBBF24)
private val SpacesAccentDim = Color(0xFFF59E0B)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SPACES SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacesScreen(
    viewModel:    SpacesViewModel,
    onOpenSpace:  (spaceId: String) -> Unit,
) {
    val spaces             by viewModel.spaces.collectAsState()
    var showCreateDialog   by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        // Ambient amber glow — top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to SpacesAccent.copy(alpha = 0.04f),
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
                                "Spaces",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                            )
                            Text(
                                "${spaces.size} space${if (spaces.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color        = SpacesAccent,
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
                SpacesFAB(onClick = { showCreateDialog = true })
            },
        ) { padding ->
            if (spaces.isEmpty()) {
                SpacesEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onCreate = { showCreateDialog = true },
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
                        bottom = 100.dp,   // clear FAB
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        items = spaces,
                        key   = { _, space -> space.id },
                    ) { index, space ->
                        StaggeredFadeSlide(index = index) {
                            SpaceCard(
                                space    = space,
                                onClick  = { onOpenSpace(space.id) },
                                onDelete = { viewModel.deleteSpace(space.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSpaceDialog(
            onDismiss = { showCreateDialog = false },
            onCreate  = { name ->
                viewModel.createSpace(name)
                showCreateDialog = false
            },
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// FAB
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

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
                            color       = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                24f, 0f, 4f,
                                SpacesAccent.copy(alpha = 0.4f).toArgb(),
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
                Brush.linearGradient(listOf(SpacesAccent, SpacesAccentDim))
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
            contentDescription = "Create Space",
            tint     = SpaceDeep,
            modifier = Modifier.size(26.dp),
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SPACE CARD
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SpaceCard(
    space:    Space,
    onClick:  () -> Unit,
    onDelete: () -> Unit,
) {
    val dateStr = remember(space.createdAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(space.createdAt))
    }

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
                                SpacesAccent.copy(alpha = 0.08f).toArgb(),
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
                brush = Brush.linearGradient(
                    listOf(GlassBorder, GlassBorder.copy(0.03f))
                ),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
    ) {
        // Left accent stripe — amber
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(SpacesAccent.copy(0.7f), SpacesAccentDim.copy(0.3f))
                    )
                )
        )

        Row(
            modifier             = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment    = Alignment.CenterVertically,
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(SpacesAccent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint     = SpacesAccent,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text      = space.name,
                    style     = MaterialTheme.typography.titleMedium,
                    color     = TextPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text  = "Created $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }

            // Delete — muted, small
            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete space",
                    tint     = TextMuted.copy(0.45f),
                    modifier = Modifier.size(17.dp),
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint     = TextMuted.copy(0.4f),
                modifier = Modifier.size(13.dp),
            )

            Spacer(Modifier.width(4.dp))
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CREATE DIALOG — glassy bottom-sheet style alert dialog
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun CreateSpaceDialog(
    onDismiss: () -> Unit,
    onCreate:  (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SpaceNebula,
        tonalElevation   = 0.dp,
        shape            = RoundedCornerShape(22.dp),
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
                    value         = name,
                    onValueChange = { name = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassWhite8),
                    placeholder   = {
                        Text(
                            "e.g. Research, Work, Books…",
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
                        cursorColor             = SpacesAccent,
                    ),
                    textStyle  = MaterialTheme.typography.bodyLarge,
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
                        if (name.isNotBlank())
                            Brush.linearGradient(listOf(SpacesAccent, SpacesAccentDim))
                        else
                            Brush.linearGradient(listOf(GlassWhite8, GlassWhite8))
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        enabled           = name.isNotBlank(),
                    ) { if (name.isNotBlank()) onCreate(name) }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Create",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = if (name.isNotBlank()) SpaceDeep else TextMuted,
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
// EMPTY STATE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SpacesEmptyState(
    modifier: Modifier = Modifier,
    onCreate: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spaces_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.1f,
        targetValue   = 0.3f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "glow_alpha",
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
                                    SpacesAccent.copy(alpha = glowAlpha).toArgb(),
                                )
                            }
                        }
                        canvas.drawCircle(
                            androidx.compose.ui.geometry.Offset(
                                size.width / 2f, size.height / 2f
                            ),
                            size.minDimension / 2f, paint,
                        )
                    }
                }
                .clip(RoundedCornerShape(24.dp))
                .background(SpacesAccent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                tint     = SpacesAccent,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "No spaces yet",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Create a space to organise your\ndocuments for context-aware chats",
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
                    Brush.linearGradient(listOf(SpacesAccent, SpacesAccentDim))
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
                Icon(Icons.Default.Add, null, tint = SpaceDeep, modifier = Modifier.size(18.dp))
                Text(
                    "Create Space",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = SpaceDeep,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}