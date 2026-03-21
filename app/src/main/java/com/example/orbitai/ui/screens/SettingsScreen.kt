package com.example.orbitai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.R
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.DownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    downloadViewModel: DownloadViewModel,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { downloadViewModel.refreshStatus() }

    SettingsHub(
        isDarkTheme = isDarkTheme,
        onThemeChanged = onThemeChanged,
        onOpenSection = onNavigate,
        onBack = onBack,
    )
}

private data class SettingsCategory(
    val destination: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val iconResId: Int? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHub(
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    onOpenSection: (String) -> Unit,
    onBack: () -> Unit,
) {
    val categories = listOf(
        SettingsCategory(
            destination = "settings/model",
            icon = Icons.Default.DeveloperBoard,
            title = "Model",
            subtitle = "Download, configure on-device & cloud models",
            accentColor = VioletCore,
        ),
        SettingsCategory(
            destination = "settings/memory",
            icon = Icons.Default.Psychology,
            title = "Memory",
            subtitle = "View, edit & toggle stored memories",
            accentColor = Color(0xFF34D399),
        ),
        SettingsCategory(
            destination = "settings/tools",
            icon = Icons.Default.Build,
            title = "Tools",
            subtitle = "Available tools and automation support",
            accentColor = Color(0xFF22D3EE),
        ),
        SettingsCategory(
            destination = "settings/orbit_bubble",
            icon = Icons.Default.ChatBubble,
            title = "Orbit Bubble",
            subtitle = "Floating bubble toggle, model and behavior",
            accentColor = Color(0xFFF97316),
            iconResId = R.drawable.vector_logo,
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDeep),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to VioletGlowSoft.copy(alpha = 0.07f),
                            1.0f to Color.Transparent,
                        ),
                        radius = 600f,
                    ),
                ),
        )

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
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                            )
                            Text(
                                "OrbitAI",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = VioletBright,
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contrast,
                                contentDescription = null,
                                tint = VioletBright,
                                modifier = Modifier.size(18.dp),
                            )
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = onThemeChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VioletCore,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = GlassWhite20,
                                ),
                            )
                            Spacer(Modifier.width(6.dp))
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(categories.indices.toList()) { index ->
                    StaggeredFadeSlide(index = index) {
                        SettingsCategoryCard(
                            category = categories[index],
                            onClick = { onOpenSection(categories[index].destination) },
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "OrbitAI • On-device • MediaPipe LLM",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    category: SettingsCategory,
    onClick: () -> Unit,
) {
    val isDark = IsOrbitDarkTheme
    val cardShape = RoundedCornerShape(18.dp)
    val accent = category.accentColor

    // Light mode: tinted glass per accent
    val lightGlassTint = when {
        accent == VioletCore                       -> Color(0xFFF0ECFF)
        accent == Color(0xFF60A5FA)                -> Color(0xFFEBF2FF)
        accent == Color(0xFF34D399)                -> Color(0xFFE8FFF5)
        accent == Color(0xFFFBBF24)                -> Color(0xFFFFF8E7)
        accent == Color(0xFFF472B6)                -> Color(0xFFFFF0F7)
        else                                       -> Color(0xFFF5F5FF)
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
                                if (isDark) 24f else 16f,
                                0f, 4f,
                                (if (isDark) Color.Black else accent)
                                    .copy(alpha = if (isDark) 0.25f else 0.08f)
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
                                     .copy(alpha = if (isDark) 0.18f else 0.40f),
                        0.5f to accent.copy(alpha = if (isDark) 0.12f else 0.18f),
                        1.0f to (if (isDark) Color.White else accent)
                                     .copy(alpha = if (isDark) 0.05f else 0.08f),
                    ),
                    start = Offset.Zero,
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
                shape = cardShape,
            )
            .clickable(
                interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(
                        width = 0.5.dp,
                        color = accent.copy(alpha = if (isDark) 0.22f else 0.28f),
                        shape = RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (category.iconResId != null) {
                    Image(
                        painter = painterResource(category.iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = category.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
