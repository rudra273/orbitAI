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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.db.Agent
import com.example.orbitai.data.db.ORBIT_AGENT_ID
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.AgentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentsScreen(viewModel: AgentsViewModel) {
    val agents by viewModel.agents.collectAsState()

    // null = closed, non-null = agent being edited (new agents use a blank shell)
    var editTarget by remember { mutableStateOf<Agent?>(null) }
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
                                    .background(AiAccent),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Agents",
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color      = TextPrimary,
                            )
                        }
                        Text(
                            "${agents.size} agent${if (agents.size != 1) "s" else ""}",
                            fontSize = 12.sp,
                            color    = TextMuted,
                            modifier = Modifier.padding(start = 18.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showCreateDialog = true },
                containerColor = AiAccent,
                contentColor   = Void,
                shape          = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Agent")
            }
        },
    ) { padding ->
        if (agents.isEmpty()) {
            AgentsEmptyState(
                modifier = Modifier.fillMaxSize().padding(padding),
                onCreate = { showCreateDialog = true },
            )
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(agents) { _, agent ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                    ) {
                        AgentCard(
                            agent    = agent,
                            onEdit   = { editTarget = agent },
                            onDelete = { viewModel.deleteAgent(agent.id) },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AgentEditDialog(
            title     = "New Agent",
            initName  = "",
            initPrompt = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, prompt ->
                viewModel.createAgent(name, prompt)
                showCreateDialog = false
            },
        )
    }

    editTarget?.let { agent ->
        AgentEditDialog(
            title      = if (agent.isDefault) "Edit Orbit" else "Edit Agent",
            initName   = agent.name,
            initPrompt = agent.systemPrompt,
            nameReadOnly = agent.isDefault,
            onDismiss  = { editTarget = null },
            onConfirm  = { name, prompt ->
                viewModel.updateAgent(agent.id, name, prompt)
                editTarget = null
            },
        )
    }
}

// ── Agent Card ────────────────────────────────────────────────────────────────

@Composable
private fun AgentCard(agent: Agent, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onEdit,
            ),
        shape = RoundedCornerShape(14.dp),
        color = Surface1,
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
                        if (agent.isDefault)
                            Brush.linearGradient(listOf(CyanCore.copy(0.2f), AiAccent.copy(0.1f)))
                        else
                            Brush.linearGradient(listOf(AiAccent.copy(0.15f), CyanCore.copy(0.05f)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (agent.isDefault) "✦" else agent.name.take(1).uppercase(),
                    fontSize   = if (agent.isDefault) 20.sp else 18.sp,
                    color      = if (agent.isDefault) CyanCore else AiAccent,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        agent.name,
                        color      = TextPrimary,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (agent.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyanCore.copy(alpha = 0.15f),
                        ) {
                            Text(
                                "default",
                                color    = CyanCore,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    agent.systemPrompt,
                    color    = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint     = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }

            if (!agent.isDefault) {
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
}

// ── Edit / Create Dialog ──────────────────────────────────────────────────────

@Composable
private fun AgentEditDialog(
    title: String,
    initName: String,
    initPrompt: String,
    nameReadOnly: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (name: String, prompt: String) -> Unit,
) {
    var name   by remember { mutableStateOf(initName) }
    var prompt by remember { mutableStateOf(initPrompt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Surface1,
        title = {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { if (!nameReadOnly) name = it },
                    label         = { Text("Name", color = TextMuted, fontSize = 12.sp) },
                    singleLine    = true,
                    readOnly      = nameReadOnly,
                    colors        = agentFieldColors(),
                    shape         = RoundedCornerShape(10.dp),
                    modifier      = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value         = prompt,
                    onValueChange = { prompt = it },
                    label         = { Text("System prompt", color = TextMuted, fontSize = 12.sp) },
                    minLines      = 4,
                    maxLines      = 8,
                    colors        = agentFieldColors(),
                    shape         = RoundedCornerShape(10.dp),
                    modifier      = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (name.isNotBlank() && prompt.isNotBlank()) onConfirm(name, prompt) },
                enabled  = name.isNotBlank() && prompt.isNotBlank(),
                colors   = ButtonDefaults.textButtonColors(contentColor = AiAccent),
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
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
private fun agentFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor   = Surface2,
    unfocusedContainerColor = Surface2,
    focusedBorderColor      = AiAccent.copy(0.6f),
    unfocusedBorderColor    = Outline,
    focusedTextColor        = TextPrimary,
    unfocusedTextColor      = TextPrimary,
    cursorColor             = AiAccent,
    focusedLabelColor       = AiAccent,
)

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun AgentsEmptyState(modifier: Modifier = Modifier, onCreate: () -> Unit) {
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
                Icons.Default.Android,
                contentDescription = null,
                tint     = AiAccent.copy(0.6f),
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("No agents yet", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create custom agents with different\nsystem prompts for any purpose",
            color     = TextMuted,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick        = onCreate,
            colors         = ButtonDefaults.buttonColors(containerColor = AiAccent, contentColor = Void),
            shape          = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Create Agent", fontWeight = FontWeight.SemiBold)
        }
    }
}
