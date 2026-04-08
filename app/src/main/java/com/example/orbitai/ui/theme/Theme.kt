package com.example.orbitai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PALETTE — Warm Minimal
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private data class OrbitPalette(
    // Backgrounds — layered depth (now warm, not space)
    val spaceVoid: Color,
    val spaceDeep: Color,
    val spaceNebula: Color,
    val spaceDust: Color,
    val spaceCloud: Color,
    val spaceMist: Color,
    // Neutral fills (subtle transparency layers)
    val glassWhite4: Color,
    val glassWhite8: Color,
    val glassWhite12: Color,
    val glassWhite20: Color,
    val glassBorder: Color,
    val glassBorderHi: Color,
    // Violet — semantic only (Spaces/RAG feature, not a brand color)
    val violetCore: Color,
    val violetBright: Color,
    val violetDim: Color,
    val violetGlow: Color,       // active space chip fill
    val violetGlowSoft: Color,
    val violetFrost: Color,      // active space chip border
    // Chat bubbles
    val userBubbleFill: Color,
    val userBubbleBorder: Color,
    val aiBubbleFill: Color,     // transparent — AI has no bubble
    val aiBubbleBorder: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textAccent: Color,
    // Semantic
    val destructive: Color,
    val destructiveSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    // On-device indicator
    val onDevicePillBg: Color,
    val onDevicePillText: Color,
)

private val DarkPalette = OrbitPalette(
    spaceVoid    = Color(0xFF141413),
    spaceDeep    = Color(0xFF141413),   // main background
    spaceNebula  = Color(0xFF1E1E1C),   // surface / cards
    spaceDust    = Color(0xFF252523),   // input fields, inactive chips
    spaceCloud   = Color(0xFF252523),
    spaceMist    = Color(0xFF323230),

    glassWhite4  = Color(0x0AFFFFFF),
    glassWhite8  = Color(0x14FFFFFF),
    glassWhite12 = Color(0x1EFFFFFF),
    glassWhite20 = Color(0x33FFFFFF),
    glassBorder  = Color(0x14FFFFFF),
    glassBorderHi= Color(0x26FFFFFF),

    // Violet — dark semantic values
    violetCore   = Color(0xFFA89EFF),
    violetBright = Color(0xFFA89EFF),
    violetDim    = Color(0xFF7B6FE0),
    violetGlow   = Color(0xFF252340),   // active chip bg
    violetGlowSoft=Color(0x1AA89EFF),
    violetFrost  = Color(0xFF3D3870),   // active chip border

    userBubbleFill   = Color(0xFFE8E6E1),
    userBubbleBorder  = Color(0xFFE8E6E1),
    aiBubbleFill     = Color.Transparent,
    aiBubbleBorder   = Color.Transparent,

    textPrimary  = Color(0xFFE8E6E1),
    textSecondary= Color(0xFF9A9A95),
    textMuted    = Color(0xFF6B6B66),
    textAccent   = Color(0xFFA89EFF),

    destructive  = Color(0xFFEF4444),
    destructiveSoft= Color(0x33EF4444),
    success      = Color(0xFF4CAF50),
    successSoft  = Color(0x224CAF50),
    warning      = Color(0xFFF59E0B),

    onDevicePillBg  = Color(0xFF1A2E1E),
    onDevicePillText= Color(0xFF4CAF50),
)

private val LightPalette = OrbitPalette(
    spaceVoid    = Color(0xFFF7F6F3),
    spaceDeep    = Color(0xFFF7F6F3),   // main background — warm off-white
    spaceNebula  = Color(0xFFFFFFFF),   // surface / cards
    spaceDust    = Color(0xFFEEEDE8),   // input fields, inactive chips
    spaceCloud   = Color(0xFFEEEDE8),
    spaceMist    = Color(0xFFE0DED8),

    glassWhite4  = Color(0x0A000000),
    glassWhite8  = Color(0x14000000),
    glassWhite12 = Color(0x1E000000),
    glassWhite20 = Color(0x33000000),
    glassBorder  = Color(0x14000000),
    glassBorderHi= Color(0x26000000),

    // Violet — light semantic values
    violetCore   = Color(0xFF5B4FE8),
    violetBright = Color(0xFF5B4FE8),
    violetDim    = Color(0xFF4A3FD0),
    violetGlow   = Color(0xFFEDE9FE),   // active chip bg
    violetGlowSoft=Color(0x1A5B4FE8),
    violetFrost  = Color(0xFFD4CFFC),   // active chip border

    userBubbleFill   = Color(0xFF1A1A1A),
    userBubbleBorder  = Color(0xFF1A1A1A),
    aiBubbleFill     = Color.Transparent,
    aiBubbleBorder   = Color.Transparent,

    textPrimary  = Color(0xFF1A1A1A),
    textSecondary= Color(0xFF8A8A85),
    textMuted    = Color(0xFF8A8A85),
    textAccent   = Color(0xFF5B4FE8),

    destructive  = Color(0xFFD93025),
    destructiveSoft= Color(0x22D93025),
    success      = Color(0xFF17A865),
    successSoft  = Color(0x2217A865),
    warning      = Color(0xFFC78300),

    onDevicePillBg  = Color(0xFFE8F5E9),
    onDevicePillText= Color(0xFF2E7D32),
)

private var currentPalette by mutableStateOf(DarkPalette)

fun setOrbitThemeMode(isDarkTheme: Boolean) {
    currentPalette = if (isDarkTheme) DarkPalette else LightPalette
}

/** true when the current Orbit palette is the dark variant */
val IsOrbitDarkTheme: Boolean get() = currentPalette == DarkPalette

// Backgrounds
val SpaceVoid: Color get() = currentPalette.spaceVoid
val SpaceDeep: Color get() = currentPalette.spaceDeep
val SpaceNebula: Color get() = currentPalette.spaceNebula
val SpaceDust: Color get() = currentPalette.spaceDust
val SpaceCloud: Color get() = currentPalette.spaceCloud
val SpaceMist: Color get() = currentPalette.spaceMist

// Neutral fills
val GlassWhite4: Color get() = currentPalette.glassWhite4
val GlassWhite8: Color get() = currentPalette.glassWhite8
val GlassWhite12: Color get() = currentPalette.glassWhite12
val GlassWhite20: Color get() = currentPalette.glassWhite20
val GlassBorder: Color get() = currentPalette.glassBorder
val GlassBorderHi: Color get() = currentPalette.glassBorderHi

// Violet — semantic only (Spaces/RAG)
val VioletCore: Color get() = currentPalette.violetCore
val VioletBright: Color get() = currentPalette.violetBright
val VioletDim: Color get() = currentPalette.violetDim
val VioletGlow: Color get() = currentPalette.violetGlow
val VioletGlowSoft: Color get() = currentPalette.violetGlowSoft
val VioletFrost: Color get() = currentPalette.violetFrost

// Chat bubbles
val UserBubbleFill: Color get() = currentPalette.userBubbleFill
val UserBubbleBorder: Color get() = currentPalette.userBubbleBorder
val AiBubbleFill: Color get() = currentPalette.aiBubbleFill
val AiBubbleBorder: Color get() = currentPalette.aiBubbleBorder

// Text
val TextPrimary: Color get() = currentPalette.textPrimary
val TextSecondary: Color get() = currentPalette.textSecondary
val TextMuted: Color get() = currentPalette.textMuted
val TextAccent: Color get() = currentPalette.textAccent

// Semantic
val Destructive: Color get() = currentPalette.destructive
val DestructiveSoft: Color get() = currentPalette.destructiveSoft
val Success: Color get() = currentPalette.success
val SuccessSoft: Color get() = currentPalette.successSoft
val Warning: Color get() = currentPalette.warning

// On-device indicator
val OnDevicePillBg: Color get() = currentPalette.onDevicePillBg
val OnDevicePillText: Color get() = currentPalette.onDevicePillText


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MATERIAL COLOR SCHEME
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun orbitColorScheme(isDarkTheme: Boolean) =
    if (isDarkTheme) {
        darkColorScheme(
            primary = VioletCore,
            onPrimary = Color(0xFF141413),
            primaryContainer = VioletDim,
            onPrimaryContainer = VioletBright,
            secondary = VioletBright,
            onSecondary = Color(0xFF141413),
            secondaryContainer = VioletGlow,
            onSecondaryContainer = VioletBright,
            tertiary = Color(0xFF60A5FA),
            onTertiary = Color(0xFF141413),
            background = SpaceDeep,
            onBackground = TextPrimary,
            surface = SpaceNebula,
            onSurface = TextPrimary,
            surfaceVariant = SpaceDust,
            onSurfaceVariant = TextSecondary,
            surfaceTint = VioletCore,
            outline = GlassBorder,
            outlineVariant = SpaceMist,
            error = Destructive,
            onError = Color.White,
            errorContainer = DestructiveSoft,
            onErrorContainer = Destructive,
            scrim = Color(0xCC141413),
            inverseSurface = TextPrimary,
            inverseOnSurface = SpaceDeep,
            inversePrimary = VioletDim,
        )
    } else {
        lightColorScheme(
            primary = VioletCore,
            onPrimary = Color.White,
            primaryContainer = VioletGlow,
            onPrimaryContainer = VioletDim,
            secondary = VioletBright,
            onSecondary = Color.White,
            secondaryContainer = VioletGlowSoft,
            onSecondaryContainer = VioletDim,
            tertiary = Color(0xFF3B82F6),
            onTertiary = Color.White,
            background = SpaceDeep,
            onBackground = TextPrimary,
            surface = SpaceNebula,
            onSurface = TextPrimary,
            surfaceVariant = SpaceDust,
            onSurfaceVariant = TextSecondary,
            surfaceTint = VioletCore,
            outline = GlassBorder,
            outlineVariant = SpaceMist,
            error = Destructive,
            onError = Color.White,
            errorContainer = DestructiveSoft,
            onErrorContainer = Destructive,
            scrim = Color(0x66F7F6F3),
            inverseSurface = SpaceDeep,
            inverseOnSurface = TextPrimary,
            inversePrimary = VioletBright,
        )
    }


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TYPOGRAPHY
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

// Replace with your bundled fonts once added to res/font/:
// val DisplayFont = FontFamily(
//     Font(R.font.dm_sans_regular, FontWeight.Normal),
//     Font(R.font.dm_sans_medium, FontWeight.Medium),
//     Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
//     Font(R.font.dm_sans_bold, FontWeight.Bold),
// )
val DisplayFont = FontFamily.Default

private fun orbitTypography() = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize   = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
        color = TextPrimary,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
        lineHeight = 30.sp,
        color = TextPrimary,
    ),

    headlineLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
        color = TextPrimary,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize   = 17.sp,
        lineHeight = 24.sp,
        color = TextPrimary,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize   = 15.sp,
        lineHeight = 22.sp,
        color = TextPrimary,
    ),

    titleLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
        color = TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = TextPrimary,
    ),
    titleSmall = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
        color = TextSecondary,
    ),

    bodyLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 15.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.1.sp,
        color = TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = TextSecondary,
    ),
    bodySmall = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        color = TextMuted,
    ),

    labelLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize   = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
        color = TextPrimary,
    ),
    labelMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = TextMuted,
    ),
    labelSmall = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        color = TextMuted,
    ),
)


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ROOT THEME COMPOSABLE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun OrbitAITheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    setOrbitThemeMode(isDarkTheme)
    MaterialTheme(
        colorScheme = orbitColorScheme(isDarkTheme),
        typography = orbitTypography(),
        content = content,
    )
}
