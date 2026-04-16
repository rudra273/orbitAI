package com.example.orbitai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.feature.memory.MemoryFeatureStore
import com.example.orbitai.core.database.MemoryEntity
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.TextPrimary
import com.example.orbitai.viewmodel.MemoryViewModel

private val MemorySurfaceLight = Color(0xFFF9F8F5)
private val MemoryCardLight = Color(0xFFFFFFFF)
private val MemoryCardDark = Color(0xFF1E1E1C)
private val MemoryInkLight = Color(0xFF0D0D0D)
private val MemoryGreen = Color(0xFF17A865)
private val MemoryRed = Color(0xFFD94F4F)
private val MemorySans = FontFamily.SansSerif
private val MemoryMono = FontFamily.Monospace

private val MemorySurface: Color
    @Composable get() = if (IsOrbitDarkTheme) SpaceDeep else MemorySurfaceLight
private val MemoryCard: Color
    @Composable get() = if (IsOrbitDarkTheme) MemoryCardDark else MemoryCardLight
private val MemoryInk: Color
    @Composable get() = if (IsOrbitDarkTheme) TextPrimary else MemoryInkLight

@Composable
fun MemorySettingsScreen(
    memoryViewModel: MemoryViewModel,
    memoryStore: MemoryFeatureStore,
    onBack: () -> Unit,
) {
    var enabled by remember { mutableStateOf(memoryStore.isEnabled) }
    val memories by memoryViewModel.memories.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MemorySurface),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MemoryInk.copy(alpha = 0.40f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Settings",
                    color = MemoryInk.copy(alpha = 0.40f),
                    fontFamily = MemoryMono,
                    fontSize = 13.sp,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 8.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Memory",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = MemoryInk,
                    fontFamily = MemorySans,
                )
                Text(
                    text = "Stored memories and controls",
                    color = MemoryInk.copy(alpha = 0.35f),
                    fontFamily = MemoryMono,
                    fontSize = 11.sp,
                )
            }
        }

        item { MemorySectionHeader("MEMORY") }
        item {
            MemoryGroupedCard {
                MemoryToggleRow(
                    title = "Use memory",
                    description = if (enabled) {
                        "OrbitAI saves and uses memories from conversations."
                    } else {
                        "Memory is off for new conversations."
                    },
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        memoryStore.isEnabled = it
                    },
                )
            }
        }

        item { MemorySectionHeader("STORED") }
        if (memories.isNotEmpty()) {
            item {
                MemoryGroupedCard {
                    memories.forEachIndexed { index, memory ->
                        MemoryStoredRow(
                            memory = memory,
                            onDelete = { memoryViewModel.deleteMemory(memory.id) },
                        )
                        if (index != memories.lastIndex) {
                            MemoryHairlineDivider()
                        }
                    }
                }
            }
        } else {
            item {
                MemoryGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MemoryGreen.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MemoryGreen,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            text = if (enabled) {
                                "No memories stored yet. Chat to build memories."
                            } else {
                                "Turn memory on to begin storing helpful details."
                            },
                            color = MemoryInk.copy(alpha = 0.55f),
                            fontFamily = MemorySans,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "OrbitAI · On-device LLM",
                modifier = Modifier.fillMaxWidth(),
                color = MemoryInk.copy(alpha = 0.20f),
                fontFamily = MemoryMono,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MemoryGroupedCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MemoryCard)
            .border(
                width = 1.dp,
                color = MemoryInk.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 10.dp),
        content = content,
    )
}

@Composable
private fun MemorySectionHeader(text: String) {
    Text(
        text = text,
        color = MemoryInk.copy(alpha = 0.30f),
        fontFamily = MemoryMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun MemoryHairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MemoryInk.copy(alpha = 0.08f)),
    )
}

@Composable
private fun MemoryToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MemoryInk.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MemoryInk.copy(alpha = 0.72f),
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    color = MemoryInk,
                    fontFamily = MemorySans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
                Text(
                    text = description,
                    color = MemoryInk.copy(alpha = 0.38f),
                    fontFamily = MemorySans,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        MemoryToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun MemoryStoredRow(
    memory: MemoryEntity,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MemoryGreen),
        )
        Text(
            text = memory.content,
            modifier = Modifier.weight(1f),
            color = MemoryInk.copy(alpha = 0.82f),
            fontFamily = MemorySans,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete memory",
                tint = MemoryRed.copy(alpha = 0.82f),
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun MemoryToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val trackColor = if (checked) MemoryInk else MemoryInk.copy(alpha = 0.12f)
    val thumbAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    val thumbColor = if (checked && IsOrbitDarkTheme) SpaceDeep else Color.White

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) }
            .padding(horizontal = 3.dp),
        contentAlignment = thumbAlignment,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(thumbColor),
        )
    }
}
