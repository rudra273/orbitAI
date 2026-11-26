package com.example.orbitai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.orbitai.R
import com.example.orbitai.core.common.Appearance
import com.example.orbitai.core.common.ThemeMode
import com.example.orbitai.ui.components.OrbitGroupedCard
import com.example.orbitai.ui.components.OrbitInsetDivider
import com.example.orbitai.ui.components.OrbitListRow
import com.example.orbitai.ui.components.OrbitSectionHeader
import com.example.orbitai.ui.components.OrbitSubScreenTopBar
import com.example.orbitai.ui.components.OrbitSwitch
import com.example.orbitai.ui.theme.OrbitTheme
import com.example.orbitai.ui.theme.tabular

/** Settings > Appearance: theme, dynamic colour, chat text size and message density. */
@Composable
fun AppearanceScreen(appearance: Appearance, onChange: (Appearance) -> Unit, onBack: () -> Unit) {
    var scale by remember(appearance.chatTextScale) { mutableFloatStateOf(appearance.chatTextScale) }
    val colors = OrbitTheme.colors
    val spacing = OrbitTheme.spacing
    val typography = OrbitTheme.typography
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = colors.background,
        topBar = {
            OrbitSubScreenTopBar(
                title = stringResource(R.string.appearance),
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = spacing.screenHorizontal,
                vertical = spacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            item { OrbitSectionHeader(stringResource(R.string.theme)) }
            item {
                OrbitGroupedCard {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        val selected = appearance.mode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .selectable(selected = selected, role = Role.RadioButton) {
                                    onChange(appearance.copy(mode = mode))
                                }
                                .padding(horizontal = spacing.cardHorizontal, vertical = spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.accent,
                                    unselectedColor = colors.labelTertiary,
                                ),
                            )
                            Spacer(Modifier.width(spacing.sm))
                            Text(
                                text = mode.label,
                                style = typography.callout,
                                color = colors.labelPrimary,
                            )
                        }
                        if (index != ThemeMode.entries.lastIndex) {
                            OrbitInsetDivider(startIndent = 52.dp)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(spacing.xs)) }
            item {
                OrbitGroupedCard {
                    OrbitListRow(
                        title = stringResource(R.string.dynamic_colour),
                        subtitle = stringResource(R.string.use_accent_colours_from_your),
                        showChevron = false,
                        trailing = {
                            OrbitSwitch(
                                checked = appearance.dynamicColor,
                                onCheckedChange = { onChange(appearance.copy(dynamicColor = it)) },
                            )
                        },
                        onClick = { onChange(appearance.copy(dynamicColor = !appearance.dynamicColor)) },
                    )
                    OrbitInsetDivider(startIndent = spacing.cardHorizontal)
                    OrbitListRow(
                        title = stringResource(R.string.compact_messages),
                        subtitle = stringResource(R.string.less_space_between_chat_messages),
                        showChevron = false,
                        trailing = {
                            OrbitSwitch(
                                checked = appearance.compactMessages,
                                onCheckedChange = { onChange(appearance.copy(compactMessages = it)) },
                            )
                        },
                        onClick = { onChange(appearance.copy(compactMessages = !appearance.compactMessages)) },
                    )
                }
            }

            item { Spacer(Modifier.height(spacing.xs)) }
            item {
                OrbitGroupedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.cardHorizontal),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Chat text size",
                                style = typography.callout,
                                color = colors.labelPrimary,
                            )
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                style = typography.footnote.tabular(),
                                color = colors.labelSecondary,
                            )
                        }
                        Slider(
                            value = scale,
                            onValueChange = { scale = (it * 20).toInt() / 20f },
                            onValueChangeFinished = { onChange(appearance.copy(chatTextScale = scale)) },
                            valueRange = 0.9f..1.3f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.surfaceInput,
                            ),
                        )
                        Text(
                            text = "Messages will look like this.",
                            style = typography.body.copy(fontSize = typography.body.fontSize * scale),
                            color = colors.labelPrimary,
                        )
                    }
                }
            }
        }
    }
}
