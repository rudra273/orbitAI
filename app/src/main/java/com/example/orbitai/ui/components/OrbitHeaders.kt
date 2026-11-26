package com.example.orbitai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.orbitai.R
import com.example.orbitai.ui.theme.OrbitTheme
import com.example.orbitai.ui.theme.tabular

/**
 * Unified top-level header shared across all 4 primary tabs (Chats, Spaces, Modes, Settings).
 * Guarantees identical height, 16dp horizontal margin, editorial serif title baseline,
 * and 48dp action targets on every top-level screen.
 */
@Composable
fun OrbitTopLevelHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = OrbitTheme.colors
    val spacing = OrbitTheme.spacing
    val typography = OrbitTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(
                    start = spacing.screenHorizontal,
                    end = spacing.xs,
                    top = 6.dp,
                    bottom = 6.dp,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = spacing.xs),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = typography.title2,
                    color = colors.labelPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = typography.footnote.tabular(),
                        color = colors.labelSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                content = actions,
            )
        }
    }
}

/**
 * Unified pinned top bar for all pushed detail and Settings sub-screens.
 * Provides a 48dp RTL-mirrored back button, crisp Inter SemiBold title, optional actions,
 * and a subtle bottom hairline separator.
 */
@Composable
fun OrbitSubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = OrbitTheme.colors
    val spacing = OrbitTheme.spacing
    val typography = OrbitTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = spacing.xxs, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrbitIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.xxs),
            ) {
                Text(
                    text = title,
                    style = typography.headline,
                    color = colors.labelPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = typography.caption1,
                        color = colors.labelSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                content = actions,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.separator),
        )
    }
}
