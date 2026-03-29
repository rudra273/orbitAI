package com.example.orbitai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.db.RagDocument
import com.example.orbitai.data.db.RagStatus
import com.example.orbitai.data.db.Space
import com.example.orbitai.ui.theme.Destructive
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.SpaceDust
import com.example.orbitai.ui.theme.SpaceNebula
import com.example.orbitai.ui.theme.TextMuted
import com.example.orbitai.ui.theme.TextPrimary
import com.example.orbitai.ui.theme.TextSecondary
import com.example.orbitai.ui.theme.VioletCore
import com.example.orbitai.viewmodel.SpacesViewModel

private val CardBorderLight = Color(0xFFE8E5E0)
private val CardBorderDark = Color(0xFF2A2A28)
private val CardSurfaceLight = Color(0xFFFFFFFF)
private val CardSurfaceDark = Color(0xFF1E1E1C)
private val SearchBgLight = Color(0xFFEEEDE8)
private val SearchBgDark = Color(0xFF1E1E1C)
private val DocRowBgLight = Color(0xFFF5F4F0)
private val DocRowBgDark = Color(0xFF252523)
private val OpenButtonBgLight = Color(0xFFEEEDE8)
private val OpenButtonBgDark = Color(0xFF252523)
private val DividerLight = Color(0xFFE8E5E0)
private val DividerDark = Color(0xFF2A2A28)
private val ProcessingLight = Color(0xFF5B4FE8)
private val ProcessingDark = Color(0xFFA89EFF)
private val IndexedGreen = Color(0xFF17A865)

private val cardBorder @Composable get() = if (IsOrbitDarkTheme) CardBorderDark else CardBorderLight
private val cardSurface @Composable get() = if (IsOrbitDarkTheme) CardSurfaceDark else CardSurfaceLight
private val searchBg @Composable get() = if (IsOrbitDarkTheme) SearchBgDark else SearchBgLight
private val docRowBg @Composable get() = if (IsOrbitDarkTheme) DocRowBgDark else DocRowBgLight
private val openButtonBg @Composable get() = if (IsOrbitDarkTheme) OpenButtonBgDark else OpenButtonBgLight
private val dividerColor @Composable get() = if (IsOrbitDarkTheme) DividerDark else DividerLight
private val processingTint @Composable get() = if (IsOrbitDarkTheme) ProcessingDark else ProcessingLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacesScreen(
    viewModel: SpacesViewModel,
    onOpenSpace: (spaceId: String) -> Unit,
) {
    val spaces by viewModel.spaces.collectAsState()
    val semanticReady = remember(spaces) { viewModel.isSemanticModelReady() }
    val spaceStats = remember { mutableStateMapOf<String, Pair<Int, Long>>() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val totalDocuments = spaceStats.values.sumOf { it.first }

    LaunchedEffect(spaces) {
        val validIds = spaces.mapTo(mutableSetOf()) { it.id }
        spaceStats.keys.toList().forEach { id ->
            if (id !in validIds) spaceStats.remove(id)
        }
    }

    Scaffold(
        containerColor = SpaceDeep,
        topBar = {
            SpacesTopBar(
                spaceCount = spaces.size,
                documentCount = totalDocuments,
                onCreate = { showCreateDialog = true },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchBar(
                query = searchQuery,
                onChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (!semanticReady) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SpaceDust)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        "Semantic model not downloaded. Go to Settings › Model to download it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }

            if (spaces.isEmpty()) {
                SpacesEmptyState(
                    modifier = Modifier.fillMaxSize(),
                    onCreate = { showCreateDialog = true },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(spaces, key = { it.id }) { space ->
                        SpaceCard(
                            space = space,
                            viewModel = viewModel,
                            searchQuery = searchQuery,
                            onStatsChanged = { count, size -> spaceStats[space.id] = count to size },
                            onOpenSpace = { onOpenSpace(space.id) },
                        )
                    }
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
private fun SpacesTopBar(
    spaceCount: Int,
    documentCount: Int,
    onCreate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceDeep)
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Spaces",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp,
                ),
                color = TextPrimary,
            )
            Text(
                text = "$spaceCount knowledge base${if (spaceCount != 1) "s" else ""} · $documentCount document${if (documentCount != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextMuted,
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TextPrimary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCreate,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Create Space",
                tint = SpaceDeep,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(searchBg)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            cursorBrush = SolidColor(TextPrimary),
            singleLine = true,
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Search spaces or documents...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun SpaceCard(
    space: Space,
    viewModel: SpacesViewModel,
    searchQuery: String,
    onStatsChanged: (docCount: Int, totalSizeBytes: Long) -> Unit,
    onOpenSpace: () -> Unit,
) {
    val docs by remember(space.id) {
        viewModel.observeDocumentsInSpace(space.id)
    }.collectAsState(initial = emptyList())

    var expanded by remember { mutableStateOf(false) }

    val q = searchQuery.trim().lowercase()
    val matchesSearch = q.isEmpty() ||
        space.name.lowercase().contains(q) ||
        docs.any { it.name.lowercase().contains(q) }

    if (!matchesSearch) return

    val isActive = docs.isNotEmpty()

    val totalBytes = remember(docs) { docs.sumOf { it.sizeBytes } }
    val totalSizeStr = remember(totalBytes) {
        when {
            totalBytes >= 1_048_576 -> "%.1f MB".format(totalBytes / 1_048_576f)
            totalBytes >= 1_024 -> "%.1f KB".format(totalBytes / 1_024f)
            else -> "$totalBytes B"
        }
    }

    LaunchedEffect(docs) {
        onStatsChanged(docs.size, totalBytes)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardSurface)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenSpace,
                )
                .padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(VioletCore.copy(alpha = if (isActive) 1f else 0.4f)),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = space.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary.copy(alpha = if (isActive) 1f else 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${docs.size} doc${if (docs.size != 1) "s" else ""}" + if (docs.isNotEmpty()) " · $totalSizeStr" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(openButtonBg.copy(alpha = if (isActive) 1f else 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { expanded = !expanded },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Show documents",
                    tint = TextSecondary.copy(alpha = if (isActive) 1f else 0.5f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded && docs.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(dividerColor),
                )
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    docs.take(3).forEach { doc ->
                        InlineDocRow(doc = doc)
                    }
                    if (docs.size > 3) {
                        Text(
                            text = "+${docs.size - 3} more documents →",
                            style = MaterialTheme.typography.labelMedium,
                            color = processingTint,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(top = 4.dp, start = 2.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onOpenSpace,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineDocRow(doc: RagDocument) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(docRowBg)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = doc.name,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        when (doc.status) {
            RagStatus.DONE -> Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Indexed",
                tint = IndexedGreen,
                modifier = Modifier.size(14.dp),
            )

            RagStatus.PROCESSING, RagStatus.PENDING -> Icon(
                Icons.Default.HourglassEmpty,
                contentDescription = "Processing",
                tint = processingTint,
                modifier = Modifier.size(14.dp),
            )

            RagStatus.ERROR -> Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Destructive),
            )
        }
    }
}

@Composable
private fun SpacesEmptyState(
    modifier: Modifier = Modifier,
    onCreate: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SpaceDust),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(34.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "No spaces yet",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Create a space to organise your\ndocuments for context-aware chats",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(TextPrimary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCreate,
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Add, null, tint = SpaceDeep, modifier = Modifier.size(18.dp))
            Text(
                "Create Space",
                style = MaterialTheme.typography.titleMedium,
                color = SpaceDeep,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CreateSpaceDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceNebula,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(18.dp),
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
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SpaceDust)
                        .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        cursorBrush = SolidColor(TextPrimary),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (name.isEmpty()) {
                                    Text(
                                        "e.g. Research, Work, Books...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextMuted,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (name.isNotBlank()) TextPrimary else SpaceDust)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = name.isNotBlank(),
                        onClick = { if (name.isNotBlank()) onCreate(name) },
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Create",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (name.isNotBlank()) SpaceDeep else TextMuted,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
    )
}
