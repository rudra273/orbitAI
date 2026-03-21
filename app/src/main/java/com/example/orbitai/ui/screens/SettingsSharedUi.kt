package com.example.orbitai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.ModelFormat
import com.example.orbitai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSubScreen(
    title: String,
    icon: ImageVector,
    accent: Color = VioletCore,
    onBack: () -> Unit,
    iconPainter: Painter? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextSecondary,
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.12f))
                                    .border(
                                        width = 0.5.dp,
                                        color = accent.copy(alpha = if (IsOrbitDarkTheme) 0.22f else 0.25f),
                                        shape = RoundedCornerShape(10.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (iconPainter != null) {
                                    Image(
                                        painter = iconPainter,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                    )
                                } else {
                                    Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
                                }
                            }
                            Text(
                                title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(top = 4.dp),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 40.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    accent: Color = VioletCore,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = IsOrbitDarkTheme
    val cardShape = RoundedCornerShape(18.dp)

    val lightGlassTint = when {
        accent == VioletCore        -> Color(0xFFF0ECFF)
        accent == Color(0xFF60A5FA) -> Color(0xFFEBF2FF)
        accent == Color(0xFF34D399) -> Color(0xFFE8FFF5)
        accent == Color(0xFFFBBF24) -> Color(0xFFFFF8E7)
        accent == Color(0xFFF472B6) -> Color(0xFFFFF0F7)
        else                        -> Color(0xFFF5F5FF)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                if (isDark) 20f else 14f,
                                0f, 4f,
                                (if (isDark) Color.Black else accent)
                                    .copy(alpha = if (isDark) 0.22f else 0.07f)
                                    .toArgb(),
                            )
                        }
                    }
                    canvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        18.dp.toPx(), 18.dp.toPx(), paint,
                    )
                }
            }
            .clip(cardShape)
            .background(
                if (isDark) Color.White.copy(alpha = 0.05f)
                else lightGlassTint.copy(alpha = 0.82f)
            )
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f  to Color.White.copy(alpha = if (isDark) 0.07f else 0.50f),
                        0.25f to Color.White.copy(alpha = if (isDark) 0.02f else 0.10f),
                        0.5f  to Color.Transparent,
                    ),
                )
            )
            .border(
                width = if (isDark) 1.dp else 1.5.dp,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to (if (isDark) Color.White else accent)
                                     .copy(alpha = if (isDark) 0.18f else 0.35f),
                        0.5f to accent.copy(alpha = if (isDark) 0.10f else 0.15f),
                        1.0f to (if (isDark) Color.White else accent)
                                     .copy(alpha = if (isDark) 0.05f else 0.06f),
                    ),
                    start = Offset.Zero,
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
                shape = cardShape,
            )
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted,
    )
}

@Composable
fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
        ),
        color = VioletBright.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
fun OrbitDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GlassBorder.copy(alpha = 0.5f)),
    )
}

@Composable
fun OrbitSlider(
    label: String,
    value: Float,
    valueStr: String,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    hint: String = "",
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                if (hint.isNotEmpty()) {
                    Text(hint, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    valueStr,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = GlassWhite8,
            ),
        )
    }
}

@Composable
fun OrbitPrimaryButton(
    label: String,
    enabled: Boolean,
    accent: Color = VioletCore,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) {
                    Brush.linearGradient(listOf(accent.copy(0.9f), accent.copy(0.7f)))
                } else {
                    Brush.linearGradient(listOf(GlassWhite8, GlassWhite8))
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) Color.White else TextMuted,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun OrbitDestructiveButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DestructiveSoft)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = Destructive,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

fun ModelFormat.badgeLabel(): String = when (this) {
    ModelFormat.TASK -> "Task"
    ModelFormat.LITERTLM -> "LiteRT-LM"
}

fun inferModelFormat(fileName: String): ModelFormat = when {
    fileName.lowercase().endsWith(".litertlm") -> ModelFormat.LITERTLM
    else -> ModelFormat.TASK
}

fun normalizeModelFileName(fileName: String): String {
    val trimmed = fileName.trim()
    return when {
        trimmed.isEmpty() -> trimmed
        trimmed.lowercase().endsWith(".task") ||
            trimmed.lowercase().endsWith(".litertlm") -> trimmed

        else -> "$trimmed.task"
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
