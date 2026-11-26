package com.example.orbitai.ui.screens

import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.feature.automation.AutomationSettingsStore
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.TextPrimary

private val ToolsSans = com.example.orbitai.ui.theme.OrbitUiFont
private val ToolsMono = com.example.orbitai.ui.theme.OrbitMonoFont

private val ToolsSurface: Color
    @Composable get() = com.example.orbitai.ui.theme.OrbitTheme.colors.background
private val ToolsCard: Color
    @Composable get() = com.example.orbitai.ui.theme.OrbitTheme.colors.surface
private val ToolsInk: Color
    @Composable get() = com.example.orbitai.ui.theme.OrbitTheme.colors.labelPrimary

@Composable
fun ToolsSettingsScreen(
    toolSettingsStore: AutomationSettingsStore,
    onBack: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenReplies: () -> Unit,
    onOpenDigest: () -> Unit,
) {
    val colors = com.example.orbitai.ui.theme.OrbitTheme.colors
    androidx.compose.material3.Scaffold(
        containerColor = colors.background,
        topBar = {
            com.example.orbitai.ui.components.OrbitSubScreenTopBar(
                title = stringResource(R.string.tools),
                subtitle = stringResource(R.string.tool_availability_and_support),
                onBack = onBack,
            )
        },
    ) { padding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { com.example.orbitai.ui.components.OrbitSectionHeader(stringResource(R.string.section_tools)) }
        item {
            Text(
                text = stringResource(R.string.orbit_asks_before_every_action),
                style = com.example.orbitai.ui.theme.OrbitTheme.typography.footnote,
                color = colors.labelSecondary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        item {
            com.example.orbitai.ui.components.OrbitGroupedCard {
                com.example.orbitai.feature.automation.OrbitTool.entries.forEachIndexed { index, tool ->
                    var enabled by remember { mutableStateOf(toolSettingsStore.isEnabled(tool)) }
                    com.example.orbitai.ui.components.OrbitListRow(
                        icon = Icons.Default.Build,
                        title = tool.title + (tool.command?.let { "  ·  /$it" } ?: ""),
                        subtitle = tool.description,
                        showChevron = false,
                        trailing = {
                            com.example.orbitai.ui.components.OrbitSwitch(
                                checked = enabled,
                                onCheckedChange = {
                                    enabled = it
                                    toolSettingsStore.setEnabled(tool, it)
                                },
                            )
                        },
                        onClick = {
                            enabled = !enabled
                            toolSettingsStore.setEnabled(tool, enabled)
                        },
                    )
                    if (index != com.example.orbitai.feature.automation.OrbitTool.entries.lastIndex) {
                        com.example.orbitai.ui.components.OrbitInsetDivider()
                    }
                }
            }
        }

        item {
            com.example.orbitai.ui.components.OrbitGroupedCard {
                com.example.orbitai.ui.components.OrbitListRow(
                    icon = Icons.Default.Event,
                    title = stringResource(R.string.saved_reminders),
                    onClick = onOpenReminders,
                )
                com.example.orbitai.ui.components.OrbitInsetDivider()
                com.example.orbitai.ui.components.OrbitListRow(
                    icon = Icons.AutoMirrored.Filled.Message,
                    title = stringResource(R.string.message_replies),
                    onClick = onOpenReplies,
                )
                com.example.orbitai.ui.components.OrbitInsetDivider()
                com.example.orbitai.ui.components.OrbitListRow(
                    icon = Icons.Default.MailOutline,
                    title = stringResource(R.string.digest_open),
                    onClick = onOpenDigest,
                )
            }
        }
        item { com.example.orbitai.ui.components.OrbitSectionHeader(stringResource(R.string.section_notes)) }
        item {
            com.example.orbitai.ui.components.OrbitGroupedCard {
                com.example.orbitai.ui.components.OrbitListRow(
                    icon = Icons.Default.Build,
                    title = stringResource(R.string.execution),
                    subtitle = stringResource(R.string.only_tools_that_support_safe),
                    showChevron = false,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.orbitai_on_device_llm),
                modifier = Modifier.fillMaxWidth(),
                style = com.example.orbitai.ui.theme.OrbitTheme.typography.caption1,
                color = colors.labelTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
    }
}

@Composable
private fun ToolsGroupedCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ToolsCard)
            .border(
                width = 1.dp,
                color = ToolsInk.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 10.dp),
        content = content,
    )
}

@Composable
private fun ToolsSectionHeader(text: String) {
    Text(
        text = text,
        color = ToolsInk.copy(alpha = 0.30f),
        fontFamily = ToolsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun ToolsHairlineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(ToolsInk.copy(alpha = 0.08f)),
    )
}

@Composable
private fun ToolInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(ToolsInk.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ToolsInk.copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = ToolsInk,
                fontFamily = ToolsSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Text(
                text = description,
                color = ToolsInk.copy(alpha = 0.38f),
                fontFamily = ToolsSans,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun ToolToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
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
                    .background(ToolsInk.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ToolsInk.copy(alpha = 0.72f),
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    color = ToolsInk,
                    fontFamily = ToolsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
                Text(
                    text = description,
                    color = ToolsInk.copy(alpha = 0.38f),
                    fontFamily = ToolsSans,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        ToolsToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ToolsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val trackColor = if (checked) ToolsInk else ToolsInk.copy(alpha = 0.12f)
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
