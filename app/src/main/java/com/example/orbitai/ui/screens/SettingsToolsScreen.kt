package com.example.orbitai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.orbitai.data.ToolSettingsStore
import com.example.orbitai.ui.theme.GlassWhite20
import com.example.orbitai.ui.theme.TextMuted
import com.example.orbitai.ui.theme.TextPrimary

@Composable
fun ToolsSettingsScreen(
    toolSettingsStore: ToolSettingsStore,
    onBack: () -> Unit,
) {
    var automationEnabled by remember { mutableStateOf(toolSettingsStore.isAutomationExecutionEnabled) }

    SettingsSubScreen(
        title = "Tools",
        icon = Icons.Default.Build,
        accent = Color(0xFFF59E0B),
        onBack = onBack,
    ) {
        SettingsDescription(
            "Control whether supported tools run automatically in the background. " +
                "Right now, this applies to reminders only."
        )

        GlassCard(accent = Color(0xFFF59E0B)) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                RowSetting(
                    title = "Automation execution",
                    subtitle = if (automationEnabled) {
                        "Supported tools execute automatically. Reminders are scheduled as background notifications."
                    } else {
                        "Tools open their target app UI instead of running automatically."
                    },
                    trailing = {
                        Switch(
                            checked = automationEnabled,
                            onCheckedChange = {
                                automationEnabled = it
                                toolSettingsStore.isAutomationExecutionEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF59E0B),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = GlassWhite20,
                            ),
                        )
                    },
                )

                OrbitDivider()

                RowSetting(
                    title = "Supported automatic tool",
                    subtitle = "Reminders: create a background notification at the scheduled time.",
                )
            }
        }
    }
}

@Composable
private fun RowSetting(
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}
