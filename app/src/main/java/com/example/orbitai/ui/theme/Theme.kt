package com.example.orbitai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

// Backgrounds — layered depth, never pure black
val SpaceVoid       = Color(0xFF07080F)   // deepest bg — almost black, slight blue cast
val SpaceDeep       = Color(0xFF0B0D16)   // screen background
val SpaceNebula     = Color(0xFF0F1120)   // cards, surfaces
val SpaceDust       = Color(0xFF141728)   // elevated surface (nav bar, input bar)
val SpaceCloud      = Color(0xFF1A1E30)   // hover / pressed states
val SpaceMist       = Color(0xFF1F2336)   // subtle dividers / borders

// Glass layers — alpha-based, layered over deep backgrounds
val GlassWhite4     = Color(0x0AFFFFFF)   // 4% white — faintest tint
val GlassWhite8     = Color(0x14FFFFFF)   // 8% — subtle card
val GlassWhite12    = Color(0x1EFFFFFF)   // 12% — card hover
val GlassWhite20    = Color(0x33FFFFFF)   // 20% — active / selected
val GlassBorder     = Color(0x1AFFFFFF)   // 10% white border
val GlassBorderHi   = Color(0x33FFFFFF)   // 20% white border — focused

// Violet / Purple accent family
val VioletCore      = Color(0xFF8B5CF6)   // primary accent — vibrant violet
val VioletBright    = Color(0xFFA78BFA)   // lighter violet — hover, text on dark
val VioletDim       = Color(0xFF6D28D9)   // pressed / deeper
val VioletGlow      = Color(0x338B5CF6)   // 20% — glow halos, active bg
val VioletGlowSoft  = Color(0x1A8B5CF6)   // 10% — very soft ambient glow
val VioletFrost     = Color(0x268B5CF6)   // frosted violet glass fill

// User bubble — violet tinted glass
val UserBubbleFill  = Color(0x2D7C3AED)   // ~18% violet
val UserBubbleBorder= Color(0x4D8B5CF6)   // 30% violet border

// Assistant bubble — dark glass
val AiBubbleFill    = Color(0x1AFFFFFF)   // 10% white
val AiBubbleBorder  = Color(0x1AFFFFFF)   // 10% white border

// Text
val TextPrimary     = Color(0xFFF0F2FF)   // near-white, slight blue cast
val TextSecondary   = Color(0xFFB8BCCC)   // muted — secondary labels
val TextMuted       = Color(0xFF6B7080)   // very muted — placeholder, disabled
val TextAccent      = VioletBright        // links, active labels

// Semantic
val Destructive     = Color(0xFFEF4444)
val DestructiveSoft = Color(0x33EF4444)
val Success         = Color(0xFF10B981)
val SuccessSoft     = Color(0x2210B981)
val Warning         = Color(0xFFF59E0B)


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MATERIAL COLOR SCHEME
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private val DarkColorScheme = darkColorScheme(
    primary            = VioletCore,
    onPrimary          = Color.White,
    primaryContainer   = VioletDim,
    onPrimaryContainer = VioletBright,

    secondary          = VioletBright,
    onSecondary        = SpaceDeep,
    secondaryContainer = VioletGlow,
    onSecondaryContainer = VioletBright,

    tertiary           = Color(0xFF60A5FA),   // cool blue — used sparingly
    onTertiary         = SpaceDeep,

    background         = SpaceDeep,
    onBackground       = TextPrimary,

    surface            = SpaceNebula,
    onSurface          = TextPrimary,
    surfaceVariant     = SpaceDust,
    onSurfaceVariant   = TextSecondary,

    surfaceTint        = VioletCore,

    outline            = GlassBorder,
    outlineVariant     = SpaceMist,

    error              = Destructive,
    onError            = Color.White,
    errorContainer     = DestructiveSoft,
    onErrorContainer   = Destructive,

    scrim              = Color(0xCC07080F),
    inverseSurface     = TextPrimary,
    inverseOnSurface   = SpaceDeep,
    inversePrimary     = VioletDim,
)


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

private val OrbitTypography = Typography(
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
fun OrbitAITheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalGlassTheme provides GlassTheme(),
        LocalSpacing    provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography  = OrbitTypography,
            content     = content,
        )
    }
}