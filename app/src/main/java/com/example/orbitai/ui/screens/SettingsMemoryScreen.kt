package com.example.orbitai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.orbitai.data.MemoryFeatureStore
import com.example.orbitai.data.db.MemoryEntity
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.MemoryViewModel

@Composable
fun MemorySettingsScreen(
    memoryViewModel: MemoryViewModel,
    memoryStore: MemoryFeatureStore,
    onBack: () -> Unit,
) {
    var enabled by remember { mutableStateOf(memoryStore.isEnabled) }
    val memories by memoryViewModel.memories.collectAsState()

    SettingsSubScreen(
        title = "Memory",
        icon = Icons.Default.Psychology,
        accent = Color(0xFF34D399),
        onBack = onBack,
    ) {
        SettingsDescription("When enabled, OrbitAI saves and uses memories from your conversations.")

        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Use Memory",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (enabled) "Memory is active" else "Memory is off",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) Color(0xFF34D399) else TextMuted,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it; memoryStore.isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF34D399),
                        uncheckedTrackColor = GlassWhite8,
                    ),
                )
            }
        }

        if (memories.isNotEmpty()) {
            SettingsSectionLabel("Stored Memories")

            memories.forEach { memory ->
                MemoryItemCard(
                    memory = memory,
                    onDelete = { memoryViewModel.deleteMemory(memory.id) },
                )
            }
        } else if (enabled) {
            GlassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF34D399).copy(0.5f),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "No memories stored yet. Chat to build memories.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryItemCard(
    memory: MemoryEntity,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite4)
            .background(
                brush = Brush.linearGradient(listOf(GlassBorder, GlassBorder.copy(0.02f))),
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34D399).copy(0.7f)),
            )
            Text(
                text = memory.content,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete memory",
                    tint = TextMuted.copy(0.4f),
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}
