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

private val ToolsSurfaceLight = Color(0xFFF9F8F5)
private val ToolsCardLight = Color(0xFFFFFFFF)
private val ToolsCardDark = Color(0xFF1E1E1C)
private val ToolsInkLight = Color(0xFF0D0D0D)
private val ToolsSans = FontFamily.SansSerif
private val ToolsMono = FontFamily.Monospace

private val ToolsSurface: Color
    @Composable get() = if (IsOrbitDarkTheme) SpaceDeep else ToolsSurfaceLight
private val ToolsCard: Color
    @Composable get() = if (IsOrbitDarkTheme) ToolsCardDark else ToolsCardLight
private val ToolsInk: Color
    @Composable get() = if (IsOrbitDarkTheme) TextPrimary else ToolsInkLight

@Composable
fun ToolsSettingsScreen(
    toolSettingsStore: AutomationSettingsStore,
    onBack: () -> Unit,
) {
    var automationEnabled by remember { mutableStateOf(toolSettingsStore.isAutomationExecutionEnabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ToolsSurface),
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
                    tint = ToolsInk.copy(alpha = 0.40f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Settings",
                    color = ToolsInk.copy(alpha = 0.40f),
                    fontFamily = ToolsMono,
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
                    text = "Tools",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = ToolsInk,
                    fontFamily = ToolsSans,
                )
                Text(
                    text = "Tool availability and support",
                    color = ToolsInk.copy(alpha = 0.35f),
                    fontFamily = ToolsMono,
                    fontSize = 11.sp,
                )
            }
        }

        item { ToolsSectionHeader("AVAILABLE") }
        item {
            ToolsGroupedCard {
                ToolInfoRow(
                    icon = Icons.Default.MailOutline,
                    title = "Gmail",
                    description = "Available tool. Opens Gmail compose flow with manual handoff.",
                )
                ToolsHairlineDivider()
                ToolInfoRow(
                    icon = Icons.AutoMirrored.Filled.Message,
                    title = "WhatsApp",
                    description = "Available tool. Opens WhatsApp compose flow with manual handoff.",
                )
                ToolsHairlineDivider()
                ToolToggleRow(
                    icon = Icons.Default.Event,
                    title = "Reminder",
                    description = if (automationEnabled) {
                        "Automation ON. Reminders are scheduled directly in background notifications."
                    } else {
                        "Automation OFF. Orbit opens a reminder app flow instead of scheduling automatically."
                    },
                    checked = automationEnabled,
                    onCheckedChange = {
                        automationEnabled = it
                        toolSettingsStore.isAutomationExecutionEnabled = it
                    },
                )
            }
        }

        item { ToolsSectionHeader("NOTES") }
        item {
            ToolsGroupedCard {
                ToolInfoRow(
                    icon = Icons.Default.Build,
                    title = "Execution",
                    description = "Only tools that support safe background execution should use automation.",
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "OrbitAI · On-device LLM",
                modifier = Modifier.fillMaxWidth(),
                color = ToolsInk.copy(alpha = 0.20f),
                fontFamily = ToolsMono,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            )
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
