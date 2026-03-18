package com.example.orbitai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// PALETTE — Deep Space
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private data class OrbitPalette(
    val spaceVoid: Color,
    val spaceDeep: Color,
    val spaceNebula: Color,
    val spaceDust: Color,
    val spaceCloud: Color,
    val spaceMist: Color,
    val glassWhite4: Color,
    val glassWhite8: Color,
    val glassWhite12: Color,
    val glassWhite20: Color,
    val glassBorder: Color,
    val glassBorderHi: Color,
    val violetCore: Color,
    val violetBright: Color,
    val violetDim: Color,
    val violetGlow: Color,
    val violetGlowSoft: Color,
    val violetFrost: Color,
    val userBubbleFill: Color,
    val userBubbleBorder: Color,
    val aiBubbleFill: Color,
    val aiBubbleBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textAccent: Color,
    val destructive: Color,
    val destructiveSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
)

private val DarkPalette = OrbitPalette(
    spaceVoid = Color(0xFF07080F),
    spaceDeep = Color(0xFF0B0D16),
    spaceNebula = Color(0xFF0F1120),
    spaceDust = Color(0xFF141728),
    spaceCloud = Color(0xFF1A1E30),
    spaceMist = Color(0xFF1F2336),
    glassWhite4 = Color(0x0AFFFFFF),
    glassWhite8 = Color(0x14FFFFFF),
    glassWhite12 = Color(0x1EFFFFFF),
    glassWhite20 = Color(0x33FFFFFF),
    glassBorder = Color(0x1AFFFFFF),
    glassBorderHi = Color(0x33FFFFFF),
    violetCore = Color(0xFF8B5CF6),
    violetBright = Color(0xFFA78BFA),
    violetDim = Color(0xFF6D28D9),
    violetGlow = Color(0x338B5CF6),
    violetGlowSoft = Color(0x1A8B5CF6),
    violetFrost = Color(0x268B5CF6),
    userBubbleFill = Color(0x2D7C3AED),
    userBubbleBorder = Color(0x4D8B5CF6),
    aiBubbleFill = Color(0x1AFFFFFF),
    aiBubbleBorder = Color(0x1AFFFFFF),
    textPrimary = Color(0xFFF0F2FF),
    textSecondary = Color(0xFFB8BCCC),
    textMuted = Color(0xFF6B7080),
    textAccent = Color(0xFFA78BFA),
    destructive = Color(0xFFEF4444),
    destructiveSoft = Color(0x33EF4444),
    success = Color(0xFF10B981),
    successSoft = Color(0x2210B981),
    warning = Color(0xFFF59E0B),
)

private val LightPalette = OrbitPalette(
    spaceVoid = Color(0xFFF5F7FF),
    spaceDeep = Color(0xFFF7F8FC),
    spaceNebula = Color(0xFFFFFFFF),
    spaceDust = Color(0xFFEDEFF7),
    spaceCloud = Color(0xFFE6E9F3),
    spaceMist = Color(0xFFD5DAE8),
    glassWhite4 = Color(0x08FFFFFF),
    glassWhite8 = Color(0x14FFFFFF),
    glassWhite12 = Color(0x1FFFFFFF),
    glassWhite20 = Color(0x33FFFFFF),
    glassBorder = Color(0x1A0E1324),
    glassBorderHi = Color(0x33111C36),
    violetCore = Color(0xFF6D49D8),
    violetBright = Color(0xFF825AF2),
    violetDim = Color(0xFF5434B8),
    violetGlow = Color(0x336D49D8),
    violetGlowSoft = Color(0x1A6D49D8),
    violetFrost = Color(0x266D49D8),
    userBubbleFill = Color(0x266D49D8),
    userBubbleBorder = Color(0x406D49D8),
    aiBubbleFill = Color(0x0A0E1324),
    aiBubbleBorder = Color(0x120E1324),
    textPrimary = Color(0xFF161A27),
    textSecondary = Color(0xFF4A556D),
    textMuted = Color(0xFF7D869B),
    textAccent = Color(0xFF6D49D8),
    destructive = Color(0xFFD93025),
    destructiveSoft = Color(0x22D93025),
    success = Color(0xFF0A8F64),
    successSoft = Color(0x220A8F64),
    warning = Color(0xFFC78300),
)

private var currentPalette by mutableStateOf(DarkPalette)

fun setOrbitThemeMode(isDarkTheme: Boolean) {
    currentPalette = if (isDarkTheme) DarkPalette else LightPalette
}

/** true when the current Orbit palette is the dark variant */
val IsOrbitDarkTheme: Boolean get() = currentPalette == DarkPalette

// Backgrounds — layered depth
val SpaceVoid: Color get() = currentPalette.spaceVoid
val SpaceDeep: Color get() = currentPalette.spaceDeep
val SpaceNebula: Color get() = currentPalette.spaceNebula
val SpaceDust: Color get() = currentPalette.spaceDust
val SpaceCloud: Color get() = currentPalette.spaceCloud
val SpaceMist: Color get() = currentPalette.spaceMist

// Glass layers
val GlassWhite4: Color get() = currentPalette.glassWhite4
val GlassWhite8: Color get() = currentPalette.glassWhite8
val GlassWhite12: Color get() = currentPalette.glassWhite12
val GlassWhite20: Color get() = currentPalette.glassWhite20
val GlassBorder: Color get() = currentPalette.glassBorder
val GlassBorderHi: Color get() = currentPalette.glassBorderHi

// Accent family
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


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MATERIAL COLOR SCHEME
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun orbitColorScheme(isDarkTheme: Boolean) =
    if (isDarkTheme) {
        darkColorScheme(
            primary = VioletCore,
            onPrimary = Color.White,
            primaryContainer = VioletDim,
            onPrimaryContainer = VioletBright,
            secondary = VioletBright,
            onSecondary = SpaceDeep,
            secondaryContainer = VioletGlow,
            onSecondaryContainer = VioletBright,
            tertiary = Color(0xFF60A5FA),
            onTertiary = SpaceDeep,
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
            scrim = Color(0xCC07080F),
            inverseSurface = TextPrimary,
            inverseOnSurface = SpaceDeep,
            inversePrimary = VioletDim,
        )
    } else {
        lightColorScheme(
            primary = VioletCore,
            onPrimary = Color.White,
            primaryContainer = VioletFrost,
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
            scrim = Color(0x6607080F),
            inverseSurface = SpaceDeep,
            inverseOnSurface = TextPrimary,
            inversePrimary = VioletBright,
        )
    }


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// GLASS SYSTEM — design tokens for glassmorphism surfaces
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * All glassmorphism surface tokens in one place.
 * Access via LocalGlassTheme.current
 */
data class GlassTheme(

    // Surface fills
    val surfaceSubtle: Color  = GlassWhite4,    // barely-there cards
    val surfaceCard: Color    = GlassWhite8,    // standard card
    val surfaceRaised: Color  = GlassWhite12,   // nav bar, bottom sheet header
    val surfaceActive: Color  = GlassWhite20,   // selected / active row

    // Borders
    val borderSubtle: Color   = GlassBorder,    // default card stroke
    val borderFocus: Color    = GlassBorderHi,  // focused input stroke
    val borderViolet: Color   = UserBubbleBorder, // accented border

    // Glow halos — apply as outer shadow / box-shadow equivalent via drawBehind
    val glowViolet: Color     = VioletGlow,
    val glowVioletSoft: Color = VioletGlowSoft,

    // Corner radii (stored here so all glass surfaces stay consistent)
    val radiusSmall: Dp   = 10.dp,
    val radiusMedium: Dp  = 16.dp,
    val radiusLarge: Dp   = 22.dp,
    val radiusXL: Dp      = 28.dp,
    val radiusFull: Dp    = 999.dp,

    // Elevation — used to pick which glass fill to use
    val elevationNone: Dp   = 0.dp,
    val elevationLow: Dp    = 1.dp,
    val elevationMedium: Dp = 4.dp,
    val elevationHigh: Dp   = 8.dp,
)

val LocalGlassTheme = staticCompositionLocalOf { GlassTheme() }


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SPACING SYSTEM
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

data class Spacing(
    val xxs: Dp  = 2.dp,
    val xs: Dp   = 4.dp,
    val sm: Dp   = 8.dp,
    val md: Dp   = 12.dp,
    val lg: Dp   = 16.dp,
    val xl: Dp   = 20.dp,
    val xxl: Dp  = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

// Convenience extension — OrbitAI.spacing.lg
object OrbitAI {
    val spacing: Spacing @Composable get() = LocalSpacing.current
    val glass: GlassTheme @Composable get() = LocalGlassTheme.current
}


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// GRADIENTS — reusable brushes
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object OrbitGradients {

    /** Faint radial violet glow — use as screen background overlay */
    val ambientGlow = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to Color(0x1A8B5CF6),   // 10% violet at center
            0.6f to Color(0x0A6D28D9),   // fading out
            1.0f to Color(0x00000000),
        )
    )

    /** Violet → transparent — bottom fade for nav bar scrim */
    val navScrim = Brush.verticalGradient(
        colors = listOf(
            Color(0x00070810),
            Color(0xE6070810),
        )
    )

    /** User bubble — subtle violet gradient */
    val userBubble = Brush.linearGradient(
        colors = listOf(
            Color(0x3D8B5CF6),   // 24% violet
            Color(0x2D7C3AED),   // 18% deeper violet
        )
    )

    /** FAB / primary button — vivid violet */
    val primaryButton = Brush.linearGradient(
        colors = listOf(
            VioletBright,
            VioletCore,
        )
    )

    /** Subtle shimmer for skeleton loaders */
    val shimmer = Brush.horizontalGradient(
        colors = listOf(
            GlassWhite4,
            GlassWhite12,
            GlassWhite4,
        )
    )

    /** Active tab indicator */
    val tabIndicator = Brush.horizontalGradient(
        colors = listOf(
            Color(0x00A78BFA),
            VioletCore,
            Color(0x00A78BFA),
        )
    )
}


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TYPOGRAPHY
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * Typography scale.
 *
 * Font family: use your bundled fonts from res/font/.
 * Replace `Font(R.font.your_font)` with your actual font resources.
 *
 * Recommended pairing for this aesthetic:
 *   Display / Headings → "DM Sans" or "Plus Jakarta Sans" (geometric, modern)
 *   Body / UI           → "Inter" or "Figtree" (clean, legible at small sizes)
 *
 * To add fonts: File → New → Android Resource Directory → font
 * Then download TTF/OTF and drag into res/font/
 */

// Uncomment and replace with your actual font resources once added to res/font/:
// val DisplayFont = FontFamily(
//     Font(R.font.dm_sans_regular, FontWeight.Normal),
//     Font(R.font.dm_sans_medium, FontWeight.Medium),
//     Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
//     Font(R.font.dm_sans_bold, FontWeight.Bold),
// )

// Fallback to system default until fonts are bundled:
val DisplayFont = FontFamily.Default

private fun orbitTypography() = Typography(
    // Large screen titles — app bars, hero text
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

    // Section headers, card titles
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

    // UI labels — nav items, button text, row titles
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

    // Body text — chat messages, descriptions
    bodyLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 15.sp,
        lineHeight = 24.sp,
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

    // Overlines, badges, timestamps
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

    CompositionLocalProvider(
        LocalGlassTheme provides GlassTheme(),
        LocalSpacing    provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = orbitColorScheme(isDarkTheme),
            typography  = orbitTypography(),
            content     = content,
        )
    }
}