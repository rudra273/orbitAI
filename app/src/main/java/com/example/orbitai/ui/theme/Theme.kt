package com.example.orbitai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ───────────────────────────────────────────────────────────────────
val Void        = Color(0xFF080C14)   // deepest bg
val Surface0    = Color(0xFF0D1117)   // card bg
val Surface1    = Color(0xFF161B24)   // elevated surface
val Surface2    = Color(0xFF1E2430)   // input bg
val Outline     = Color(0xFF2A3344)
val CyanCore    = Color(0xFF00D4FF)   // primary accent
val CyanDim     = Color(0xFF0099BB)
val TextPrimary = Color(0xFFE8EDF5)
val TextMuted   = Color(0xFF6B7A92)
val UserBubble  = Color(0xFF162136)
val AiBubble    = Color(0xFF0F1A12)
val AiAccent    = Color(0xFF00FF88)   // assistant name accent

private val DarkScheme = darkColorScheme(
    primary          = CyanCore,
    onPrimary        = Void,
    primaryContainer = CyanDim,
    secondary        = AiAccent,
    background       = Void,
    surface          = Surface0,
    surfaceVariant   = Surface1,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextMuted,
    outline          = Outline,
    error            = Color(0xFFFF5252),
)

@Composable
fun OrbitAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content
    )
}