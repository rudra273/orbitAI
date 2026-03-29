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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import com.example.orbitai.data.Chat
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.viewmodel.ChatViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

private val SurfaceLight = Color(0xFFF9F8F5)
private val SurfaceDark = SpaceDeep
private val InkLight = Color(0xFF0D0D0D)
private val InkDark = Color(0xFFF0EFE9)
private val Violet = Color(0xFF5B4FE8)
private val Green = Color(0xFF17A865)
private val DeleteRed = Color(0xFFD94F4F)
private val PopupLight = Color(0xFFFFFFFF)
private val PopupDark = Color(0xFF1A1A17)

private val surfaceColor @Composable get() = if (IsOrbitDarkTheme) SurfaceDark else SurfaceLight
private val inkColor @Composable get() = if (IsOrbitDarkTheme) InkDark else InkLight
private val dividerColor @Composable get() = inkColor.copy(alpha = 0.06f)
private val popupSurface @Composable get() = if (IsOrbitDarkTheme) PopupDark else PopupLight

private val Sans = FontFamily.SansSerif
private val Mono = FontFamily.Monospace

private data class ChatGroup(val title: String, val chats: List<Chat>)
private sealed interface ChatListItem {
    data class Header(val label: String) : ChatListItem
    data class Row(val chat: Chat) : ChatListItem
}

@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onOpenChat: (String) -> Unit,
) {
    val chats by viewModel.chats.collectAsState()
    val groupedItems = remember(chats) {
        buildListItems(chats.sortedByDescending { chatTimestamp(it) })
    }

    Scaffold(
        containerColor = surfaceColor,
        topBar = { ChatHistoryTopBar(chatCount = chats.size) },
        floatingActionButton = {
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
                            onClick = {
                                val id = viewModel.createNewChat()
                                onOpenChat(id)
                            },
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
        },
    ) { padding ->
        if (groupedItems.isEmpty()) {
            EmptyChats(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            ) {
                items(groupedItems, key = {
                    when (it) {
                        is ChatListItem.Header -> "h_${it.label}"
                        is ChatListItem.Row -> it.chat.id
                    }
                }) { item ->
                    when (item) {
                        is ChatListItem.Header -> GroupHeader(item.label)
                        is ChatListItem.Row -> {
                            ChatRow(
                                chat = item.chat,
                                onOpen = { onOpenChat(item.chat.id) },
                                onDelete = { viewModel.deleteChat(item.chat.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatHistoryTopBar(chatCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "OrbitAI",
                color = inkColor,
                fontFamily = Mono,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = inkColor,
                    modifier = Modifier.size(20.dp),
                )
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = inkColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Chats",
            color = inkColor,
            fontFamily = Sans,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "$chatCount conversation${if (chatCount == 1) "" else "s"}",
            color = inkColor.copy(alpha = 0.35f),
            fontFamily = Mono,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun GroupHeader(label: String) {
    Text(
        text = label,
        color = inkColor.copy(alpha = 0.35f),
        fontFamily = Mono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ChatRow(
    chat: Chat,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember(chat.id) { mutableStateOf(false) }

    Column {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpen,
                        onLongClick = { menuExpanded = true },
                    )
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                                    .background(Violet.copy(alpha = 0.10f))
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
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Rename",
                            color = inkColor,
                            fontFamily = Sans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    },
                    onClick = { menuExpanded = false },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Pin",
                            color = inkColor,
                            fontFamily = Sans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    },
                    onClick = { menuExpanded = false },
                )
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
private fun EmptyChats(modifier: Modifier = Modifier) {
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
            text = "Start a new chat to begin",
            color = inkColor.copy(alpha = 0.3f),
            fontFamily = Mono,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private fun buildListItems(chats: List<Chat>): List<ChatListItem> {
    if (chats.isEmpty()) return emptyList()

    val groups = listOf(
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

private fun chatTimestamp(chat: Chat): Long =
    chat.messages.lastOrNull()?.timestampMs ?: chat.createdAt

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

private fun displayTitle(chat: Chat): String {
    val title = chat.title.trim()
    if (title.isBlank() || title.equals("New Chat", ignoreCase = true)) return "Untitled chat"
    return title
}

private fun previewText(chat: Chat): String {
    val preview = chat.messages.lastOrNull()?.content?.trim().orEmpty()
    return if (preview.isBlank()) "No messages yet" else preview
}

private fun displayModelName(modelId: String): String {
    if (modelId.isBlank()) return "unknown-model"
    return modelId.lowercase().replace("_", "-")
}

private fun isOnDeviceModel(modelId: String): Boolean =
    modelId.isNotBlank() && !modelId.contains("gemini", ignoreCase = true)

private fun extractSpaceTag(chat: Chat): String? {
    val title = chat.title
    val start = title.lastIndexOf('[')
    val end = title.lastIndexOf(']')
    if (start >= 0 && end > start + 1 && end == title.lastIndex) {
        return title.substring(start + 1, end).trim().takeIf { it.isNotBlank() }
    }
    return null
}
