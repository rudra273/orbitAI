package com.example.orbitai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
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
import com.example.orbitai.data.db.RagDocument
import com.example.orbitai.data.db.RagStatus
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.SpacesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SpacesAccent    = Color(0xFFFBBF24)
private val SpacesAccentDim = Color(0xFFF59E0B)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SPACE DETAIL SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceDetailScreen(
    spaceId:   String,
    viewModel: SpacesViewModel,
    onBack:    () -> Unit,
) {
    val spaces by viewModel.spaces.collectAsState()
    val space  = spaces.find { it.id == spaceId }
    val docs   by viewModel.observeDocumentsInSpace(spaceId).collectAsState(initial = emptyList())

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.addDocumentToSpace(it, spaceId) }
    }

    val launchPicker = {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to SpacesAccent.copy(alpha = 0.04f),
                            1.0f to Color.Transparent,
                        ),
                        radius = 600f,
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                SpaceDetailTopBar(
                    spaceName  = space?.name ?: "Space",
                    docCount   = docs.size,
                    onBack     = onBack,
                    onAddDoc   = launchPicker,
                )
            },
            floatingActionButton = {
                SpaceDetailFAB(onClick = launchPicker)
            },
        ) { padding ->
            if (docs.isEmpty()) {
                SpaceDetailEmptyState(
                    modifier       = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onPickDocument = launchPicker,
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
                        items = docs,
                        key   = { _, doc -> doc.id },
                    ) { index, doc ->
                        StaggeredFadeSlide(index = index) {
                            SpaceDocumentCard(
                                doc      = doc,
                                onDelete = { viewModel.deleteDocument(doc.id) },
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
private fun SpaceDetailTopBar(
    spaceName: String,
    docCount:  Int,
    onBack:    () -> Unit,
    onAddDoc:  () -> Unit,
) {
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
                    spaceName,
                    style    = MaterialTheme.typography.headlineMedium,
                    color    = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$docCount document${if (docCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color      = SpacesAccent,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        },
        colors   = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = Modifier.padding(top = 4.dp),
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// FAB
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SpaceDetailFAB(onClick: () -> Unit) {
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
            contentDescription = "Add Document",
            tint     = SpaceDeep,
            modifier = Modifier.size(26.dp),
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// DOCUMENT CARD
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SpaceDocumentCard(
    doc:      RagDocument,
    onDelete: () -> Unit,
) {
    val dateStr = remember(doc.addedAt) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(doc.addedAt))
    }
    val sizeStr = remember(doc.sizeBytes) {
        when {
            doc.sizeBytes >= 1_048_576 -> "%.1f MB".format(doc.sizeBytes / 1_048_576f)
            doc.sizeBytes >= 1_024     -> "%.1f KB".format(doc.sizeBytes / 1_024f)
            else                       -> "${doc.sizeBytes} B"
        }
    }

    // Accent per file type
    val fileAccent = when {
        doc.mimeType == "application/pdf"      -> Color(0xFFEF4444)   // red for PDF
        doc.mimeType.startsWith("text/")       -> Color(0xFF60A5FA)   // blue for text
        else                                   -> SpacesAccent
    }

    val isDark = IsOrbitDarkTheme
    val cardShape = RoundedCornerShape(18.dp)

    val lightGlassTint = when {
        fileAccent == Color(0xFFEF4444) -> Color(0xFFFFF0F0)
        fileAccent == Color(0xFF60A5FA) -> Color(0xFFEBF2FF)
        else                           -> Color(0xFFFFF8E7)
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
                                if (isDark) 20f else 14f,
                                0f, 4f,
                                (if (isDark) Color.Black else fileAccent)
                                    .copy(alpha = if (isDark) 0.22f else 0.07f)
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
                        0.0f to (if (isDark) Color.White else fileAccent)
                                     .copy(alpha = if (isDark) 0.18f else 0.35f),
                        0.5f to fileAccent.copy(alpha = if (isDark) 0.10f else 0.15f),
                        1.0f to (if (isDark) Color.White else fileAccent)
                                     .copy(alpha = if (isDark) 0.05f else 0.06f),
                    ),
                    start = Offset.Zero,
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
                shape = cardShape,
            ),
    ) {
        // Left stripe removed — glass border replaces it

        Row(
            modifier             = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment    = Alignment.CenterVertically,
        ) {
            // File type icon badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(fileAccent.copy(alpha = 0.1f))
                    .border(
                        width = 0.5.dp,
                        color = fileAccent.copy(alpha = if (isDark) 0.22f else 0.25f),
                        shape = RoundedCornerShape(13.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val icon = when {
                    doc.mimeType == "application/pdf"     -> Icons.Default.PictureAsPdf
                    doc.mimeType.startsWith("text/")      -> Icons.AutoMirrored.Filled.TextSnippet
                    else                                  -> Icons.AutoMirrored.Filled.Article
                }
                Icon(icon, null, tint = fileAccent, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    doc.name,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(sizeStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Box(
                        Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(TextMuted.copy(0.4f))
                    )
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                Spacer(Modifier.height(6.dp))
                DocStatusChip(status = doc.status, chunkCount = doc.chunkCount)
            }

            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete document",
                    tint     = TextMuted.copy(0.4f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// STATUS CHIP
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun DocStatusChip(status: RagStatus, chunkCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val angle by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(1200)),
        label         = "spin",
    )

    val (icon, label, chipBg, chipText) = when (status) {
        RagStatus.PENDING    -> DocChipStyle(
            Icons.Default.HourglassEmpty,
            "Pending",
            Color(0xFF2A2000),
            Color(0xFFFBBF24),
        )
        RagStatus.PROCESSING -> DocChipStyle(
            Icons.Default.HourglassEmpty,
            "Processing",
            VioletGlowSoft,
            VioletBright,
        )
        RagStatus.DONE       -> DocChipStyle(
            Icons.Default.Verified,
            if (chunkCount > 0) "$chunkCount chunks" else "Indexed",
            Color(0xFF002010),
            Color(0xFF34D399),
        )
        RagStatus.ERROR      -> DocChipStyle(
            Icons.Default.ErrorOutline,
            "Error",
            DestructiveSoft,
            Destructive,
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(chipBg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = chipText,
                modifier = Modifier
                    .size(11.dp)
                    .then(
                        if (status == RagStatus.PROCESSING) Modifier.rotate(angle)
                        else Modifier
                    ),
            )
            Text(
                label,
                style      = MaterialTheme.typography.labelSmall,
                color      = chipText,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private data class DocChipStyle(
    val icon:   androidx.compose.ui.graphics.vector.ImageVector,
    val label:  String,
    val bg:     Color,
    val text:   Color,
)

// Destructuring operator to match old code pattern
private operator fun DocChipStyle.component1() = icon
private operator fun DocChipStyle.component2() = label
private operator fun DocChipStyle.component3() = bg
private operator fun DocChipStyle.component4() = text

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// EMPTY STATE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun SpaceDetailEmptyState(
    modifier:       Modifier = Modifier,
    onPickDocument: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "doc_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.08f,
        targetValue   = 0.25f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "doc_glow",
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
                .background(SpacesAccent.copy(alpha = 0.08f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to SpacesAccent.copy(alpha = 0.25f),
                            1.0f to SpacesAccent.copy(alpha = 0.05f),
                        ),
                    ),
                    shape = RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Article,
                contentDescription = null,
                tint     = SpacesAccent,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "No documents yet",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Add PDFs or text files to this space\nfor context-aware responses",
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
                    onClick           = onPickDocument,
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
                    "Add Document",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = SpaceDeep,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}