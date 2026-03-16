package com.example.orbitai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceDetailScreen(
    spaceId: String,
    viewModel: SpacesViewModel,
    onBack: () -> Unit,
) {
    val spaces by viewModel.spaces.collectAsState()
    val space  = spaces.find { it.id == spaceId }
    val docs   by viewModel.observeDocumentsInSpace(spaceId).collectAsState(initial = emptyList())

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.addDocumentToSpace(it, spaceId) }
    }

    Scaffold(
        containerColor = Void,
        topBar = {
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
                    Column {
                        Text(
                            space?.name ?: "Space",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary,
                        )
                        Text(
                            "${docs.size} document${if (docs.size != 1) "s" else ""}",
                            fontSize = 12.sp,
                            color    = TextMuted,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    documentPicker.launch(
                        arrayOf(
                            "application/pdf",
                            "text/plain",
                            "text/markdown",
                            "text/csv",
                            "text/x-markdown",
                        )
                    )
                },
                containerColor = AiAccent,
                contentColor   = Void,
                shape          = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Document")
            }
        },
    ) { padding ->
        if (docs.isEmpty()) {
            SpaceDetailEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onPickDocument = {
                    documentPicker.launch(
                        arrayOf("application/pdf", "text/plain", "text/markdown", "text/csv")
                    )
                },
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(docs) { _, doc ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                    ) {
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

@Composable
private fun SpaceDocumentCard(doc: RagDocument, onDelete: () -> Unit) {
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        color    = Surface1,
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(AiAccent.copy(0.12f), CyanCore.copy(0.06f)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val icon = when {
                    doc.mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
                    doc.mimeType.startsWith("text/")  -> Icons.AutoMirrored.Filled.TextSnippet
                    else                               -> Icons.AutoMirrored.Filled.Article
                }
                Icon(icon, contentDescription = null, tint = AiAccent, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    doc.name,
                    color      = TextPrimary,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(8.dp),
                ) {
                    Text(sizeStr, color = TextMuted, fontSize = 12.sp)
                    Text("·", color = TextMuted, fontSize = 12.sp)
                    Text(dateStr, color = TextMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(6.dp))
                SpaceDocStatusChip(doc.status, doc.chunkCount)
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint     = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SpaceDocStatusChip(status: RagStatus, chunkCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val angle by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(1200)),
        label         = "spin",
    )

    val (icon, label, bgColor, textColor) = when (status) {
        RagStatus.PENDING    -> StatusStyle(Icons.Default.HourglassEmpty, "Pending",    Color(0xFF2A2200), Color(0xFFFFCC00))
        RagStatus.PROCESSING -> StatusStyle(Icons.Default.HourglassEmpty, "Processing", Color(0xFF00203A), CyanCore)
        RagStatus.DONE       -> StatusStyle(Icons.Default.Verified, if (chunkCount > 0) "$chunkCount chunks" else "Indexed", Color(0xFF00200E), AiAccent)
        RagStatus.ERROR      -> StatusStyle(Icons.Default.ErrorOutline, "Error",        Color(0xFF280000), Color(0xFFFF5252))
    }

    Surface(shape = RoundedCornerShape(6.dp), color = bgColor) {
        Row(
            modifier              = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint     = textColor,
                modifier = Modifier
                    .size(12.dp)
                    .then(if (status == RagStatus.PROCESSING) Modifier.rotate(angle) else Modifier),
            )
            Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SpaceDetailEmptyState(modifier: Modifier = Modifier, onPickDocument: () -> Unit) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(AiAccent.copy(0.12f), Void))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Article,
                contentDescription = null,
                tint     = AiAccent.copy(0.6f),
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("No documents yet", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add PDFs or text files to this space\nfor context-aware responses",
            color     = TextMuted,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick        = onPickDocument,
            colors         = ButtonDefaults.buttonColors(containerColor = AiAccent, contentColor = Void),
            shape          = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Document", fontWeight = FontWeight.SemiBold)
        }
    }
}

// Tiny helper to avoid multiple destructuring declarations
private data class StatusStyle<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
