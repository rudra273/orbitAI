package com.example.orbitai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.orbitai.R
import com.example.orbitai.ui.theme.OrbitTheme

/**
 * Apple HIG inset-grouped card container: crisp [OrbitColors.surface] fill on [OrbitColors.background],
 * 16dp concentric corner radius, and a subtle hairline border in dark mode for clear layer separation.
 */
@Composable
fun OrbitGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OrbitTheme.colors
    val shapes = OrbitTheme.shapes
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.card,
        color = colors.surface,
        border = if (colors.isDark) BorderStroke(0.5.dp, colors.separator) else null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/** Hairline separator inside an [OrbitGroupedCard], inset to align with row text start. */
@Composable
fun OrbitInsetDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 64.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(0.5.dp)
            .background(OrbitTheme.colors.separator),
    )
}

/** Accessible uppercase section header aligned to grouped card content. */
@Composable
fun OrbitSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = OrbitTheme.typography.sectionHeader,
        color = OrbitTheme.colors.labelSecondary,
        modifier = modifier
            .padding(
                start = OrbitTheme.spacing.md,
                end = OrbitTheme.spacing.md,
                top = OrbitTheme.spacing.sm,
                bottom = OrbitTheme.spacing.xxs,
            )
            .semantics { heading() },
    )
}

/**
 * Standard HIG list row for inset-grouped cards:
 * - Minimum 56dp height (48dp+ touch target)
 * - 16dp horizontal / 12dp vertical padding
 * - Concentric 36dp icon badge (`10.dp` radius)
 * - Standard ripple press state
 */
@Composable
fun OrbitListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = OrbitTheme.colors.labelPrimary,
    iconContainerColor: Color = OrbitTheme.colors.surfaceInput,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = OrbitTheme.colors
    val spacing = OrbitTheme.spacing
    val shapes = OrbitTheme.shapes
    val typography = OrbitTheme.typography

    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
        .padding(horizontal = spacing.cardHorizontal, vertical = spacing.rowVertical)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(shapes.cardInner)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) iconTint else colors.labelDisabled,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = typography.callout,
                color = if (enabled) colors.labelPrimary else colors.labelDisabled,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = typography.footnote,
                    color = if (enabled) colors.labelSecondary else colors.labelDisabled,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        when {
            trailing != null -> trailing()
            showChevron && onClick != null -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.labelTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Accessible, WCAG-verified toggle switch with 48dp minimum touch target,
 * spring thumb motion, and clearly distinguishable ON and OFF states in both themes.
 */
@Composable
fun OrbitSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = OrbitTheme.colors
    val motion = OrbitTheme.motion
    val haptics = com.example.orbitai.ui.theme.rememberOrbitHaptics()
    val stateLabel = if (checked) "On" else "Off"
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.surfaceHigh
            checked -> colors.switchTrackOn
            else -> colors.switchTrackOff
        },
        animationSpec = motion.standardSpring(),
        label = "orbit_switch_track",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) colors.switchThumbOn else colors.switchThumbOff,
        animationSpec = motion.standardSpring(),
        label = "orbit_switch_thumb",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = motion.snappySpring(),
        label = "orbit_switch_offset",
    )

    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .semantics {
                stateDescription = stateLabel
            }
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = {
                    haptics.toggle()
                    onCheckedChange(it)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(CircleShape)
                .background(trackColor)
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    }
}

/** Accessible 48dp icon button with standard ripple feedback and optional tinted container. */
@Composable
fun OrbitIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = OrbitTheme.colors.labelPrimary,
    containerColor: Color = Color.Transparent,
    enabled: Boolean = true,
) {
    val colors = OrbitTheme.colors
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else colors.labelDisabled,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

enum class OrbitButtonStyle { Primary, Secondary, Destructive }

/** Primary / Secondary / Destructive action button with 48dp min height and loading state. */
@Composable
fun OrbitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: OrbitButtonStyle = OrbitButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = OrbitTheme.colors
    val shapes = OrbitTheme.shapes
    val container = when (style) {
        OrbitButtonStyle.Primary -> colors.inkPrimary
        OrbitButtonStyle.Secondary -> colors.surfaceInput
        OrbitButtonStyle.Destructive -> colors.destructiveContainer
    }
    val contentColor = when (style) {
        OrbitButtonStyle.Primary -> colors.labelOnInk
        OrbitButtonStyle.Secondary -> colors.labelPrimary
        OrbitButtonStyle.Destructive -> colors.onDestructiveContainer
    }

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = 48.dp),
        shape = shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = contentColor,
            disabledContainerColor = colors.surfaceHigh,
            disabledContentColor = colors.labelDisabled,
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
            Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = OrbitTheme.typography.callout,
            color = if (enabled) contentColor else colors.labelDisabled,
        )
    }
}

/** Unified 48dp search field with clear button and WCAG-compliant placeholder. */
@Composable
fun OrbitSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = OrbitTheme.colors
    val shapes = OrbitTheme.shapes
    val typography = OrbitTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shapes.input)
            .background(colors.surfaceInput)
            .border(0.5.dp, colors.separator, shapes.input)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = colors.labelSecondary,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
            textStyle = typography.body.copy(color = colors.labelPrimary),
            cursorBrush = SolidColor(colors.accent),
            singleLine = true,
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = typography.body,
                            color = colors.labelTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            OrbitIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.clear_search),
                onClick = { onQueryChange("") },
                tint = colors.labelSecondary,
            )
        }
    }
}

/** Pill badge that pairs an optional icon with high-contrast label text (never color alone). */
@Composable
fun OrbitStatusBadge(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = OrbitTheme.colors.surfaceInput,
    contentColor: Color = OrbitTheme.colors.labelSecondary,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = label,
            style = OrbitTheme.typography.caption1,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Clean editorial empty state with readable WCAG contrast and optional primary CTA. */
@Composable
fun OrbitEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = OrbitTheme.colors
    val typography = OrbitTheme.typography
    val spacing = OrbitTheme.spacing

    Column(
        modifier = modifier.padding(horizontal = spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(OrbitTheme.shapes.card)
                    .background(colors.surfaceInput),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.labelSecondary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(spacing.md))
        }
        Text(
            text = title,
            style = typography.title2,
            color = colors.labelPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.xs))
        Text(
            text = subtitle,
            style = typography.subheadline,
            color = colors.labelSecondary,
            textAlign = TextAlign.Center,
        )
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Spacer(Modifier.height(spacing.lg))
            OrbitButton(
                text = actionLabel,
                onClick = onAction,
                icon = actionIcon,
            )
        }
    }
}

/** Structured skeleton bar for async loading states. */
@Composable
fun OrbitSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(OrbitTheme.shapes.chip)
            .background(OrbitTheme.colors.surfaceInput),
    )
}
