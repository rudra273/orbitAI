package com.example.orbitai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacesScreen(
    viewModel: SpacesViewModel,
    onOpenSpace: (spaceId: String) -> Unit,
) {
    val spaces by viewModel.spaces.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Void,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyanCore),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Spaces",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                        Text(
                            "${spaces.size} space${if (spaces.size != 1) "s" else ""}",
                            fontSize = 12.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 18.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = CyanCore,
                contentColor   = Void,
                shape          = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Space")
            }
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(spaces) { _, space ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                    ) {
                        SpaceCard(
                            space     = space,
                            onClick   = { onOpenSpace(space.id) },
                            onDelete  = { viewModel.deleteSpace(space.id) },
                        )
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

@Composable
private fun SpaceCard(space: Space, onClick: () -> Unit, onDelete: () -> Unit) {
    val dateStr = remember(space.createdAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(space.createdAt))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(14.dp),
        color = Surface1,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(CyanCore.copy(0.15f), CyanCore.copy(0.05f)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint     = CyanCore,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    space.name,
                    color      = TextPrimary,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Created $dateStr",
                    color    = TextMuted,
                    fontSize = 12.sp,
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete space",
                    tint     = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint     = TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CreateSpaceDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Surface1,
        title = {
            Text("New Space", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                placeholder   = { Text("e.g. Research, Work, Books…", color = TextMuted) },
                singleLine    = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = Surface2,
                    unfocusedContainerColor = Surface2,
                    focusedBorderColor      = CyanCore.copy(0.6f),
                    unfocusedBorderColor    = Outline,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary,
                    cursorColor             = CyanCore,
                ),
                shape = RoundedCornerShape(10.dp),
            )
        },
        confirmButton = {
            TextButton(
                onClick  = { if (name.isNotBlank()) onCreate(name) },
                enabled  = name.isNotBlank(),
                colors   = ButtonDefaults.textButtonColors(contentColor = CyanCore),
            ) {
                Text("Create", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors  = ButtonDefaults.textButtonColors(contentColor = TextMuted),
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SpacesEmptyState(modifier: Modifier = Modifier, onCreate: () -> Unit) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(CyanCore.copy(0.12f), Void))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint     = CyanCore.copy(0.6f),
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("No spaces yet", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a space to organise your\ndocuments for context-aware chats",
            color     = TextMuted,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick        = onCreate,
            colors         = ButtonDefaults.buttonColors(containerColor = CyanCore, contentColor = Void),
            shape          = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Create Space", fontWeight = FontWeight.SemiBold)
        }
    }
}
