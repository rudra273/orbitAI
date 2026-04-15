package com.example.orbitai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.core.common.SUPPORTED_DOCUMENT_MIME_TYPES
import com.example.orbitai.core.database.RagDocument
import com.example.orbitai.core.database.RagStatus
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.TextMuted
import com.example.orbitai.ui.theme.TextPrimary
import com.example.orbitai.ui.theme.TextSecondary
import com.example.orbitai.viewmodel.SpacesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardSurfaceLight = Color(0xFFFFFFFF)
private val CardSurfaceDark = Color(0xFF1E1E1C)
private val CardBorderLight = Color(0xFFE8E5E0)
private val CardBorderDark = Color(0xFF2A2A28)
private val RecessedLight = Color(0xFFF5F4F0)
private val RecessedDark = Color(0xFF252523)
private val ProcessingLight = Color(0xFF5B4FE8)
private val ProcessingDark = Color(0xFFA89EFF)
private val IndexedGreen = Color(0xFF17A865)
private val AddButtonBgLight = Color(0xFFEDE9FE)
private val AddButtonTextLight = Color(0xFF5B4FE8)
private val AddButtonBgDark = Color(0xFF252340)
private val AddButtonTextDark = Color(0xFFA89EFF)
private val DeleteBorderLight = Color(0xFFF0C4C4)
private val DeleteTextLight = Color(0xFFC0392B)
private val DeleteBorderDark = Color(0xFF5A2A2A)
private val DeleteTextDark = Color(0xFFE74C3C)

private val cardSurface @Composable get() = if (IsOrbitDarkTheme) CardSurfaceDark else CardSurfaceLight
private val cardBorder @Composable get() = if (IsOrbitDarkTheme) CardBorderDark else CardBorderLight
private val recessedBg @Composable get() = if (IsOrbitDarkTheme) RecessedDark else RecessedLight
private val processingTint @Composable get() = if (IsOrbitDarkTheme) ProcessingDark else ProcessingLight
private val addButtonBg @Composable get() = if (IsOrbitDarkTheme) AddButtonBgDark else AddButtonBgLight
private val addButtonText @Composable get() = if (IsOrbitDarkTheme) AddButtonTextDark else AddButtonTextLight
private val deleteBorder @Composable get() = if (IsOrbitDarkTheme) DeleteBorderDark else DeleteBorderLight
private val deleteText @Composable get() = if (IsOrbitDarkTheme) DeleteTextDark else DeleteTextLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceDetailScreen(
    spaceId: String,
    viewModel: SpacesViewModel,
    onBack: () -> Unit,
) {
    val spaces by viewModel.spaces.collectAsState()
    val space = spaces.find { it.id == spaceId }
    val docs by viewModel.observeDocumentsInSpace(spaceId).collectAsState(initial = emptyList())
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteSpaceConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.addDocumentToSpace(it, spaceId) }
    }

    val totalSizeBytes = remember(docs) { docs.sumOf { it.sizeBytes } }
    val totalSizeLabel = remember(totalSizeBytes) { formatSize(totalSizeBytes) }

    Scaffold(
        containerColor = SpaceDeep,
        topBar = {
            SpaceDetailHeader(
                spaceName = space?.name ?: "Space",
                onBack = onBack,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },
                onRename = { showRenameDialog = true },
                onDelete = { showDeleteSpaceConfirm = true },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatsRow(
                    documentCount = docs.size,
                    totalSize = totalSizeLabel,
                )
            }

            item {
                DocumentsHeader(onAdd = { documentPicker.launch(SUPPORTED_DOCUMENT_MIME_TYPES) })
            }

            if (docs.isEmpty()) {
                item {
                    EmptyDocumentsCard()
                }
            } else {
                items(docs, key = { it.id }) { doc ->
                    SwipeableDocumentRow(
                        doc = doc,
                        onDelete = { viewModel.deleteDocument(doc.id) },
                    )
                }
            }

            item {
                DeleteSpaceButton(onClick = { showDeleteSpaceConfirm = true })
            }
        }
    }

    if (showDeleteSpaceConfirm && space != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSpaceConfirm = false },
            title = { Text("Delete this Space?", color = TextPrimary) },
            text = {
                Text(
                    "This removes the Space and all documents inside it.",
                    color = TextMuted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSpaceConfirm = false
                        viewModel.deleteSpace(space.id)
                        onBack()
                    },
                ) { Text("Delete", color = deleteText) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSpaceConfirm = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = cardSurface,
        )
    }

    if (showRenameDialog && space != null) {
        RenameSpaceDialog(
            initialName = space.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.renameSpace(space.id, newName)
                showRenameDialog = false
            },
        )
    }
}

@Composable
private fun SpaceDetailHeader(
    spaceName: String,
    onBack: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .combinedClickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextSecondary,
            )
        }

        Text(
            text = spaceName,
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp),
        )

        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .combinedClickable(onClick = { onMenuExpandedChange(true) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = TextSecondary,
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
                containerColor = cardSurface,
            ) {
                DropdownMenuItem(
                    text = { Text("Rename", color = TextPrimary) },
                    onClick = {
                        onMenuExpandedChange(false)
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = deleteText) },
                    onClick = {
                        onMenuExpandedChange(false)
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun StatsRow(
    documentCount: Int,
    totalSize: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            value = documentCount.toString(),
            label = "Documents",
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = totalSize,
            label = "Total size",
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(recessedBg)
            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.3.sp,
            ),
            color = TextMuted,
        )
    }
}

@Composable
private fun DocumentsHeader(onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "DOCUMENTS",
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = TextMuted,
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(addButtonBg)
                .combinedClickable(onClick = onAdd)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = addButtonText, modifier = Modifier.size(14.dp))
                Text(
                    text = "Add",
                    color = addButtonText,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableDocumentRow(
    doc: RagDocument,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.32f },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(deleteText.copy(alpha = 0.12f))
                    .border(1.dp, deleteBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = deleteText, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Delete",
                        color = deleteText,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
    ) {
        DocumentRowContent(
            doc = doc,
            onLongPressDelete = onDelete,
        )
    }
}

@Composable
private fun DocumentRowContent(
    doc: RagDocument,
    onLongPressDelete: () -> Unit,
) {
    val dateStr = remember(doc.addedAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(doc.addedAt))
    }
    val sizeStr = remember(doc.sizeBytes) { formatSize(doc.sizeBytes) }
    val isProcessing = doc.status == RagStatus.PROCESSING || doc.status == RagStatus.PENDING

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(recessedBg)
            .combinedClickable(onClick = {}, onLongClick = onLongPressDelete)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.name,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (isProcessing) "Processing..." else "$sizeStr · $dateStr",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = if (isProcessing) processingTint else TextMuted,
            )
        }

        DocumentStatusIndicator(status = doc.status)
    }
}

@Composable
private fun DocumentStatusIndicator(status: RagStatus) {
    when (status) {
        RagStatus.DONE -> {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(IndexedGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Indexed",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        RagStatus.PROCESSING, RagStatus.PENDING -> {
            val transition = rememberInfiniteTransition(label = "processing_spin")
            val angle by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "processing_angle",
            )
            Icon(
                Icons.Default.HourglassEmpty,
                contentDescription = "Processing",
                tint = processingTint,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(angle),
            )
        }

        RagStatus.ERROR -> {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = "Error",
                tint = deleteText,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun EmptyDocumentsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardSurface)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "No documents yet",
            color = TextPrimary,
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Tap + Add to include files in this Space.",
            color = TextMuted,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DeleteSpaceButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, deleteBorder, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = deleteText, modifier = Modifier.size(16.dp))
            Text(
                text = "Delete this Space",
                color = deleteText,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun RenameSpaceDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Space", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Update the name used across Spaces.",
                    color = TextMuted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(recessedBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        cursorBrush = SolidColor(TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name.trim() != initialName.trim(),
            ) {
                Text("Save", color = addButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = cardSurface,
    )
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576f)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024f)
    else -> "$bytes B"
}
