package com.example.orbitai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.WavingHand
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.Chat
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HOME / CHAT LIST SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel:  ChatViewModel,
    onOpenChat: (String) -> Unit,
) {
    val chats by viewModel.chats.collectAsState()
    val sorted = remember(chats) { chats.sortedByDescending { it.createdAt } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        // ── Ambient radial glow — top-centre ─────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to VioletGlowSoft,
                            0.5f to VioletGlowSoft.copy(alpha = 0.03f),
                            1.0f to Color.Transparent,
                        ),
                        center = Offset(Float.POSITIVE_INFINITY / 2f, 0f),
                        radius = 600f,
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                ChatListFab(
                    onClick = {
                        val id = viewModel.createNewChat()
                        onOpenChat(id)
                    }
                )
            },
            topBar = {
                ChatListTopBar()
            },
        ) { padding ->
            if (sorted.isEmpty()) {
                EmptyState(
                    modifier  = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onNewChat = {
                        val id = viewModel.createNewChat()
                        onOpenChat(id)
                    }
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
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        items = sorted,
                        key   = { _, chat -> chat.id },
                    ) { index, chat ->
                        StaggeredFadeSlide(index = index) {
                            ChatListCard(
                                chat     = chat,
                                onClick  = { onOpenChat(chat.id) },
                                onDelete = { viewModel.deleteChat(chat.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TOP BAR
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatListTopBar() {
    TopAppBar(
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text       = "Chats",
                    style      = MaterialTheme.typography.headlineLarge,
                    color      = TextPrimary,
                )
                Text(
                    text  = "OrbitAI",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color        = VioletBright,
                        letterSpacing = 1.5.sp,
                        fontWeight   = FontWeight.SemiBold,
                    ),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        ),
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun ChatListFab(onClick: () -> Unit) {
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
                                VioletCore.copy(alpha = 0.35f).toArgb(),
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
            .background(OrbitGradients.primaryButton)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "New chat",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// CHAT LIST CARD
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun ChatListCard(
    chat:     Chat,
    onClick:  () -> Unit,
    onDelete: () -> Unit,
) {
    val lastMsg = chat.messages.lastOrNull()
    val timeStr = remember(chat.createdAt) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(chat.createdAt))
    }
    val preview = lastMsg?.content?.take(72) ?: "No messages yet"
    val isDark = IsOrbitDarkTheme

    // Press animation
    var pressed by remember { mutableStateOf(false) }
    val cardAlpha by animateFloatAsState(
        targetValue   = if (pressed) 0.7f else 1f,
        animationSpec = tween(100),
        label         = "card_press",
    )

    val cardShape = RoundedCornerShape(18.dp)

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
                                if (isDark) 24f else 16f,
                                0f, 4f,
                                (if (isDark) Color.Black else VioletCore)
                                    .copy(alpha = if (isDark) 0.30f else 0.08f)
                                    .toArgb()
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
            // Glass fill
            .background(
                if (isDark) Color.White.copy(alpha = 0.05f)
                else Color(0xFFF0ECFF).copy(alpha = 0.82f) // violet-tinted frost for light
            )
            // Top sheen
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f  to Color.White.copy(alpha = if (isDark) 0.07f else 0.50f),
                        0.25f to Color.White.copy(alpha = if (isDark) 0.02f else 0.10f),
                        0.5f  to Color.Transparent,
                    ),
                )
            )
            // Glass border
            .border(
                width = if (isDark) 1.dp else 1.5.dp,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to (if (isDark) Color.White else VioletCore)
                                     .copy(alpha = if (isDark) 0.18f else 0.40f),
                        0.5f to VioletCore.copy(alpha = if (isDark) 0.12f else 0.18f),
                        1.0f to (if (isDark) Color.White else VioletCore)
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
            ) {
                pressed = true
                onClick()
                pressed = false
            },
    ) {
        Row(
            modifier             = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment    = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text      = chat.title,
                    style     = MaterialTheme.typography.titleMedium,
                    color     = TextPrimary,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text     = preview,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text  = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted.copy(alpha = 0.6f),
                )
            }

            // Delete — muted, small, non-intrusive
            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = "Delete chat",
                    tint               = TextMuted.copy(alpha = 0.5f),
                    modifier           = Modifier.size(17.dp),
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// EMPTY STATE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun EmptyState(
    modifier:  Modifier = Modifier,
    onNewChat: () -> Unit,
) {
    // Pulsing glow animation on the icon
    val infiniteTransition = rememberInfiniteTransition(label = "empty_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue   = 0.15f,
        targetValue    = 0.4f,
        animationSpec  = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val isDark = IsOrbitDarkTheme
        val iconShape = RoundedCornerShape(24.dp)
        // Glowing icon container
        Box(
            modifier         = Modifier
                .size(80.dp)
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                color       = android.graphics.Color.TRANSPARENT
                                setShadowLayer(
                                    40f, 0f, 0f,
                                    VioletCore.copy(alpha = glowAlpha).toArgb(),
                                )
                            }
                        }
                        canvas.drawCircle(
                            Offset(size.width / 2f, size.height / 2f),
                            size.minDimension / 2f,
                            paint,
                        )
                    }
                }
                .clip(iconShape)
                .background(
                    if (isDark) VioletFrost
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
                            0.0f to VioletCore.copy(alpha = if (isDark) 0.35f else 0.40f),
                            1.0f to VioletCore.copy(alpha = if (isDark) 0.08f else 0.12f),
                        ),
                    ),
                    shape = iconShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint               = VioletBright,
                modifier           = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text  = "No conversations yet",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text  = "Start a new chat to explore OrbitAI",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )

        Spacer(Modifier.height(36.dp))

        // CTA button — gradient violet pill
        Box(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(OrbitGradients.primaryButton)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onNewChat,
                )
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(18.dp),
                )
                Text(
                    text      = "Start a Chat",
                    style     = MaterialTheme.typography.titleMedium,
                    color     = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SHARED UTILITIES
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * Staggered entrance animation: each item fades in and slides up,
 * delayed by its index so they cascade visually.
 */
@Composable
fun StaggeredFadeSlide(
    index:   Int,
    content: @Composable () -> Unit,
) {
    val delayMs = (index * 50).coerceAtMost(400)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label         = "stagger_alpha",
    )
    val offsetY by animateFloatAsState(
        targetValue   = if (visible) 0f else 24f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label         = "stagger_y",
    )

    Box(
        modifier = Modifier
            .graphicsLayer(
                alpha        = alpha,
                translationY = offsetY,
            )
    ) {
        content()
    }
}

/**
 * Draws a subtle glow border ring using drawBehind + shadow layer.
 * Use on any Modifier chain to add a soft coloured rim.
 */
fun Modifier.glowBorder(
    color:  Color,
    radius: Dp = 12.dp,
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                this.color  = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    radius.toPx() * 0.6f, 0f, 0f,
                    color.toArgb(),
                )
            }
        }
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            radius.toPx(), radius.toPx(), paint,
        )
    }
}