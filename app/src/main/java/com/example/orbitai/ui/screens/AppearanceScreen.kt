package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.core.common.Appearance
import com.example.orbitai.core.common.ThemeMode

/** Settings > Appearance: theme, dynamic colour, chat text size and message density. */
@Composable
fun AppearanceScreen(appearance: Appearance, onChange: (Appearance) -> Unit, onBack: () -> Unit) {
    var scale by remember(appearance.chatTextScale) { mutableFloatStateOf(appearance.chatTextScale) }
    BackHandler(onBack = onBack)

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(stringResource(R.string.appearance), style = MaterialTheme.typography.headlineMedium)
            }
            item { Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall) }
            ThemeMode.entries.forEach { mode ->
                item(key = mode.name) {
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = appearance.mode == mode, role = Role.RadioButton) {
                            onChange(appearance.copy(mode = mode))
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = appearance.mode == mode, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(mode.label)
                    }
                }
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.dynamic_colour),
                    description = stringResource(R.string.use_accent_colours_from_your),
                    checked = appearance.dynamicColor,
                ) { onChange(appearance.copy(dynamicColor = it)) }
            }
            item {
                Text("Chat text size: ${(scale * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = scale,
                    onValueChange = { scale = (it * 20).toInt() / 20f },
                    onValueChangeFinished = { onChange(appearance.copy(chatTextScale = scale)) },
                    valueRange = 0.9f..1.3f,
                    steps = 7,
                )
                Text(
                    "Messages will look like this.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * scale),
                )
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.compact_messages),
                    description = stringResource(R.string.less_space_between_chat_messages),
                    checked = appearance.compactMessages,
                ) { onChange(appearance.copy(compactMessages = it)) }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, lineHeight = 16.sp)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
