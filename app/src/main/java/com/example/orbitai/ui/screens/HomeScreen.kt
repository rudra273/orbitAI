package com.example.orbitai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.feature.chat.ChatSummary
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.SpaceNebula
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.viewmodel.ChatViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

private val InkLight = Color(0xFF0D0D0D)
private val InkDark = Color(0xFFF0EFE9)
private val Violet = Color(0xFF5B4FE8)
private val Green = Color(0xFF17A865)
private val DeleteRed = Color(0xFFD94F4F)
private val PopupDark = Color(0xFF1E1E1C)
private val SearchBgLight = Color(0xFFF9F8F5) // Or similar to SpaceDeep if needed
private val SearchBgDark = Color(0xFF1E1E1C)

private val surfaceColor @Composable get() = SpaceDeep
private val inkColor @Composable get() = if (IsOrbitDarkTheme) InkDark else InkLight
private val dividerColor @Composable get() = inkColor.copy(alpha = if (IsOrbitDarkTheme) 0.12f else 0.06f)
private val popupSurface @Composable get() = if (IsOrbitDarkTheme) PopupDark else SpaceNebula
private val tagBackground @Composable get() = Violet.copy(alpha = if (IsOrbitDarkTheme) 0.18f else 0.10f)

private val Sans = FontFamily.SansSerif
private val Mono = FontFamily.Monospace

private data class ChatGroup(val title: String, val chats: List<ChatSummary>)
private sealed interface ChatListItem {
    data class Header(val label: String) : ChatListItem
    data class Row(val chat: ChatSummary) : ChatListItem
    data class Hit(val hit: com.example.orbitai.feature.chat.MessageSearchHit) : ChatListItem
}

@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onOpenChat: (String) -> Unit,
    onDownloadModel: () -> Unit,
    onOpenMessage: (chatId: String, messageId: String) -> Unit = { chatId, _ -> onOpenChat(chatId) },
) {
    val chats by viewModel.chatSummaries.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var renaming by remember { mutableStateOf<ChatSummary?>(null) }
    var messageHits by remember { mutableStateOf(emptyList<com.example.orbitai.feature.chat.MessageSearchHit>()) }

    val filteredChats = remember(chats, searchQuery, showArchived) {
        val lowerq = searchQuery.lowercase()
        chats.filter { it.isArchived == showArchived }
            .filter { displayTitle(it).lowercase().contains(lowerq) || previewText(it).lowercase().contains(lowerq) }
    }

    // Full-text search inside messages, after a short pause in typing.
    androidx.compose.runtime.LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.length < 2) {
            messageHits = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(250)
        messageHits = viewModel.searchMessages(query)
    }

    val groupedItems = remember(filteredChats, messageHits) {
        buildListItems(filteredChats.sortedByDescending { chatTimestamp(it) }) +
            if (messageHits.isEmpty()) emptyList()
            else listOf(ChatListItem.Header("IN MESSAGES")) + messageHits.map { ChatListItem.Hit(it) }
    }
    val selectionMode = selected.isNotEmpty()
    androidx.activity.compose.BackHandler(enabled = selectionMode || showArchived) {
        if (selectionMode) selected = emptySet() else showArchived = false
    }

    renaming?.let { chat ->
        var name by remember(chat.id) { mutableStateOf(displayTitle(chat)) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renaming = null },
            containerColor = popupSurface,
            title = { Text("Rename chat", color = inkColor, fontFamily = Sans) },
            text = {
                androidx.compose.material3.OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
            },
            confirmButton = {
                androidx.compose.material3.TextButton(enabled = name.isNotBlank(), onClick = {
                    viewModel.renameChat(chat.id, name)
                    renaming = null
                }) { Text("Save") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }

    Scaffold(
        containerColor = surfaceColor,
        topBar = {
          if (selectionMode) {
            SelectionBar(
                count = selected.size,
                archivedView = showArchived,
                onClose = { selected = emptySet() },
                onArchive = { viewModel.setArchived(selected, !showArchived); selected = emptySet() },
                onDelete = { viewModel.deleteChats(selected); selected = emptySet() },
            )
          } else {
            ChatHistoryTopBar(
                title = if (showArchived) "Archived" else "Chats",
                archivedView = showArchived,
                onToggleArchived = { showArchived = !showArchived },
                chatCount = filteredChats.size,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                searchActive = searchActive,
                onSearchToggle = { 
                    searchActive = !searchActive
                    if (!searchActive) searchQuery = "" 
                },
                onClearAll = { showClearConfirm = true }
            )
          }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(inkColor.copy(alpha = 0.12f))
                    .clickable(onClick = { viewModel.openTemporaryChat(onOpenChat) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.VisibilityOff,
                    contentDescription = "New temporary chat",
                    tint = inkColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(inkColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.openNewChat(onOpenChat) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New chat",
                        tint = surfaceColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            }
        },
    ) { padding ->
        if (groupedItems.isEmpty()) {
            EmptyChats(
                hasModels = availableModels.isNotEmpty(),
                onDownloadModel = onDownloadModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 24.dp),
            ) {
                itemsIndexed(groupedItems, key = { _, it ->
                    when (it) {
                        is ChatListItem.Header -> "h_${it.label}"
                        is ChatListItem.Row -> it.chat.id
                        is ChatListItem.Hit -> "m_${it.hit.messageId}"
                    }
                }) { index, item ->
                    when (item) {
                        is ChatListItem.Header -> GroupHeader(
                            label = item.label,
                            isFirst = index == 0,
                        )
                        is ChatListItem.Row -> {
                            val id = item.chat.id
                            ChatRow(
                                chat = item.chat,
                                selectionMode = selectionMode,
                                isSelected = id in selected,
                                onOpen = {
                                    if (selectionMode) selected = if (id in selected) selected - id else selected + id
                                    else onOpenChat(id)
                                },
                                onDelete = { viewModel.deleteChat(id) },
                                onRename = { renaming = item.chat },
                                onTogglePin = { viewModel.setPinned(listOf(id), !item.chat.isPinned) },
                                onToggleArchive = { viewModel.setArchived(listOf(id), !item.chat.isArchived) },
                                onSelect = { selected = selected + id },
                            )
                        }
                        is ChatListItem.Hit -> MessageHitRow(item.hit) { onOpenMessage(item.hit.chatId, item.hit.messageId) }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = popupSurface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    "Clear All Chats",
                    color = inkColor,
                    fontFamily = Sans,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    "Delete all conversations except archived ones? This action cannot be undone.",
                    color = inkColor.copy(alpha = 0.7f),
                    fontFamily = Sans,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeleteRed.copy(alpha = 0.15f))
                        .clickable(onClick = {
                            viewModel.deleteChats(chats.filter { !it.isArchived }.map { it.id })
                            showClearConfirm = false
                        })
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Delete All",
                        color = DeleteRed,
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showClearConfirm = false }) {
                    Text(
                        "Cancel",
                        color = inkColor,
                        fontFamily = Sans,
                        fontSize = 13.sp,
                    )
                }
            }
        )
    }
}

@Composable
private fun ChatHistoryTopBar(
    title: String,
    archivedView: Boolean,
    onToggleArchived: () -> Unit,
    chatCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    searchActive: Boolean,
    onSearchToggle: () -> Unit,
    onClearAll: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = if (searchActive) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (searchActive) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = inkColor,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSearchToggle
                    )
            )
            Spacer(Modifier.size(12.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(inkColor.copy(alpha = 0.05f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    color = inkColor,
                    fontFamily = Sans
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(inkColor),
                singleLine = true,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search chats...", 
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = inkColor.copy(alpha = 0.4f),
                                fontFamily = Sans
                            )
                        }
                        inner()
                    }
                }
            )
        } else {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.displaySmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = inkColor,
                )
                Text(
                    text = "$chatCount conversation${if (chatCount == 1) "" else "s"}",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = inkColor.copy(alpha = 0.5f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSearchToggle
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = inkColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { menuExpanded = true }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = inkColor,
                        modifier = Modifier.size(20.dp),
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundedCornerShape(8.dp),
                        containerColor = popupSurface,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, inkColor.copy(alpha = 0.10f)),
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (archivedView) "All chats" else "Archived chats", color = inkColor, fontFamily = Sans, fontSize = 14.sp) },
                            onClick = {
                                menuExpanded = false
                                onToggleArchived()
                            },
                        )
                        if (!archivedView) DropdownMenuItem(
                            text = { 
                                Text(
                                    "Clear all chats", 
                                    color = DeleteRed, 
                                    fontFamily = Sans, 
                                    fontSize = 14.sp 
                                ) 
                            },
                            onClick = {
                                menuExpanded = false
                                onClearAll()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(label: String, isFirst: Boolean = false) {
    Text(
        text = label,
        color = inkColor.copy(alpha = 0.35f),
        fontFamily = Mono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = if (isFirst) 4.dp else 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ChatRow(
    chat: ChatSummary,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    onSelect: () -> Unit,
) {
    var menuExpanded by remember(chat.id) { mutableStateOf(false) }

    Column {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpen,
                        onLongClick = { if (selectionMode) onOpen() else menuExpanded = true },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    androidx.compose.material3.Checkbox(checked = isSelected, onCheckedChange = { onOpen() })
                    Spacer(Modifier.size(4.dp))
                }
                if (chat.isPinned) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = inkColor.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.size(6.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle(chat),
                        color = inkColor,
                        fontFamily = Sans,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = previewText(chat),
                        color = inkColor.copy(alpha = 0.5f),
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isOnDeviceModel(chat.modelId)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Green),
                            )
                            Spacer(Modifier.size(6.dp))
                        }

                        Text(
                            text = "${formatChatTimestamp(chatTimestamp(chat))} · ${displayModelName(chat.modelId)}",
                            color = inkColor.copy(alpha = 0.4f),
                            fontFamily = Mono,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )

                        val spaceTag = extractSpaceTag(chat)
                        if (spaceTag != null) {
                            Text(
                                text = spaceTag,
                                color = Violet,
                                fontFamily = Mono,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tagBackground)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = RoundedCornerShape(8.dp),
                containerColor = popupSurface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, inkColor.copy(alpha = 0.10f)),
            ) {
                listOfNotNull(
                    "Rename" to onRename,
                    if (chat.isArchived) null else (if (chat.isPinned) "Unpin" else "Pin") to onTogglePin,
                    (if (chat.isArchived) "Unarchive" else "Archive") to onToggleArchive,
                    "Select" to onSelect,
                ).forEach { (label, action) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                color = inkColor,
                                fontFamily = Sans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            action()
                        },
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete",
                            color = DeleteRed,
                            fontFamily = Sans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor),
        )
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    archivedView: Boolean,
    onClose: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = popupSurface,
            title = { Text("Delete $count chat${if (count == 1) "" else "s"}?", color = inkColor, fontFamily = Sans) },
            text = { Text("This can't be undone.", color = inkColor.copy(alpha = 0.7f), fontFamily = Sans) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete", color = DeleteRed) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Cancel selection", tint = inkColor)
        }
        Text("$count selected", color = inkColor, fontFamily = Sans, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        androidx.compose.material3.TextButton(onClick = onArchive) { Text(if (archivedView) "Unarchive" else "Archive", color = inkColor) }
        androidx.compose.material3.TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = DeleteRed) }
    }
}

/** A search match inside a message: chat title plus the excerpt with matches in bold. */
@Composable
private fun MessageHitRow(hit: com.example.orbitai.feature.chat.MessageSearchHit, onOpen: () -> Unit) {
    val excerpt = remember(hit.snippet) {
        androidx.compose.ui.text.buildAnnotatedString {
            Regex("""\[([^\]]*)]""").let { marker ->
                var last = 0
                marker.findAll(hit.snippet).forEach { m ->
                    append(hit.snippet.substring(last, m.range.first))
                    withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
                    last = m.range.last + 1
                }
                append(hit.snippet.substring(last))
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = hit.chatTitle.takeUnless { it.isBlank() || it.equals("New Chat", true) } ?: "Untitled chat",
            color = inkColor,
            fontFamily = Sans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = excerpt,
            color = inkColor.copy(alpha = 0.6f),
            fontFamily = Sans,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = formatChatTimestamp(hit.timestampMs),
            color = inkColor.copy(alpha = 0.4f),
            fontFamily = Mono,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun EmptyChats(
    hasModels: Boolean,
    onDownloadModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No conversations yet",
            color = inkColor.copy(alpha = 0.4f),
            fontFamily = Sans,
            fontSize = 18.sp,
        )
        Text(
            text = if (hasModels) "Start a new chat to begin" else "Download a model to start using OrbitAI",
            color = inkColor.copy(alpha = 0.3f),
            fontFamily = Mono,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (!hasModels) {
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(inkColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDownloadModel,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = surfaceColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Download a model to use",
                        color = surfaceColor,
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun buildListItems(allChats: List<ChatSummary>): List<ChatListItem> {
    if (allChats.isEmpty()) return emptyList()
    val chats = allChats.filter { !it.isPinned }

    val groups = listOf(
        ChatGroup("PINNED", allChats.filter { it.isPinned }),
        ChatGroup("TODAY", chats.filter { ageInDays(chatTimestamp(it)) == 0L }),
        ChatGroup("YESTERDAY", chats.filter { ageInDays(chatTimestamp(it)) == 1L }),
        ChatGroup("LAST 7 DAYS", chats.filter { ageInDays(chatTimestamp(it)) in 2L..7L }),
        ChatGroup("OLDER", chats.filter { ageInDays(chatTimestamp(it)) > 7L }),
    ).filter { it.chats.isNotEmpty() }

    return buildList {
        groups.forEach { group ->
            add(ChatListItem.Header(group.title))
            group.chats.forEach { add(ChatListItem.Row(it)) }
        }
    }
}

private fun chatTimestamp(chat: ChatSummary): Long =
    chat.lastMessageAt ?: chat.createdAt

private fun ageInDays(timestamp: Long): Long {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return ChronoUnit.DAYS.between(date, today)
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val fullDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun formatChatTimestamp(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(timestamp).atZone(zone)
    val days = ageInDays(timestamp)
    return when {
        days <= 0L -> time.format(timeFormatter)
        days == 1L -> "Yesterday"
        time.year == LocalDate.now(zone).year -> time.format(dateFormatter)
        else -> time.format(fullDateFormatter)
    }
}

private fun displayTitle(chat: ChatSummary): String {
    val title = chat.title.trim()
    if (title.isBlank() || title.equals("New Chat", ignoreCase = true)) return "Untitled chat"
    return title
}

private fun previewText(chat: ChatSummary): String {
    val preview = chat.lastMessagePreview?.trim().orEmpty()
    return if (preview.isBlank()) "No messages yet" else preview
}

private fun displayModelName(modelId: String): String {
    if (modelId.isBlank()) return "unknown-model"
    return modelId.lowercase().replace("_", "-")
}

private fun isOnDeviceModel(modelId: String): Boolean =
    modelId.isNotBlank() && !modelId.contains("gemini", ignoreCase = true)

private fun extractSpaceTag(chat: ChatSummary): String? {
    val title = chat.title
    val start = title.lastIndexOf('[')
    val end = title.lastIndexOf(']')
    if (start >= 0 && end > start + 1 && end == title.lastIndex) {
        return title.substring(start + 1, end).trim().takeIf { it.isNotBlank() }
    }
    return null
}
