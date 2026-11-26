package com.example.orbitai.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ORBIT THEME ACCESSOR
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

object OrbitTheme {
    val colors: OrbitColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOrbitColors.current

    val typography: OrbitTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalOrbitTypography.current

    val spacing: OrbitSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalOrbitSpacing.current

    val shapes: OrbitShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalOrbitShapes.current

    val motion: OrbitMotionPreferences
        @Composable
        @ReadOnlyComposable
        get() = LocalOrbitMotion.current
}

/** Chat-only appearance (text size and density), provided by [OrbitAITheme]. */
val LocalAppearance = androidx.compose.runtime.staticCompositionLocalOf {
    com.example.orbitai.core.common.Appearance()
}

// Legacy global bridge kept temporarily while screens migrate to OrbitTheme.colors (removed in Phase 6).
private var currentColors by mutableStateOf(OrbitDarkColors)

fun resolveOrbitColors(
    isDarkTheme: Boolean,
    pureBlack: Boolean = false,
    accent: ColorScheme? = null,
    highContrast: Boolean = false,
): OrbitColors {
    val base = when {
        !isDarkTheme -> OrbitLightColors
        pureBlack -> OrbitBlackColors
        else -> OrbitDarkColors
    }
    val withAccent = if (accent != null) base.withDynamicAccent(accent) else base
    return if (highContrast) withAccent.withHighContrast() else withAccent
}

fun setOrbitThemeMode(
    isDarkTheme: Boolean,
    pureBlack: Boolean = false,
    accent: ColorScheme? = null,
) {
    currentColors = resolveOrbitColors(isDarkTheme = isDarkTheme, pureBlack = pureBlack, accent = accent)
}

/** true when the current Orbit palette is a dark variant (dark or pure black) */
val IsOrbitDarkTheme: Boolean get() = currentColors.isDark

// Legacy color bridges mapped to the new Apple HIG semantic tokens
val SpaceVoid: Color get() = currentColors.background
val SpaceDeep: Color get() = currentColors.background
val SpaceNebula: Color get() = currentColors.surface
val SpaceDust: Color get() = currentColors.surfaceInput
val SpaceCloud: Color get() = currentColors.surfaceElevated
val SpaceMist: Color get() = currentColors.surfaceHigh

val GlassWhite4: Color get() = if (currentColors.isDark) Color(0x0AFFFFFF) else Color(0x0A000000)
val GlassWhite8: Color get() = if (currentColors.isDark) Color(0x14FFFFFF) else Color(0x14000000)
val GlassWhite12: Color get() = if (currentColors.isDark) Color(0x1EFFFFFF) else Color(0x1E000000)
val GlassWhite20: Color get() = if (currentColors.isDark) Color(0x33FFFFFF) else Color(0x33000000)
val GlassBorder: Color get() = currentColors.separatorSubtle
val GlassBorderHi: Color get() = currentColors.border

val VioletCore: Color get() = currentColors.accent
val VioletBright: Color get() = currentColors.accent
val VioletDim: Color get() = currentColors.onAccentContainer
val VioletGlow: Color get() = currentColors.accentContainer
val VioletGlowSoft: Color get() = currentColors.accent.copy(alpha = 0.12f)
val VioletFrost: Color get() = currentColors.accentBorder

val UserBubbleFill: Color get() = currentColors.userBubbleFill
val UserBubbleBorder: Color get() = currentColors.userBubbleFill
val AiBubbleFill: Color get() = Color.Transparent
val AiBubbleBorder: Color get() = Color.Transparent

val TextPrimary: Color get() = currentColors.labelPrimary
val TextSecondary: Color get() = currentColors.labelSecondary
val TextMuted: Color get() = currentColors.labelTertiary
val TextAccent: Color get() = currentColors.accent

val Destructive: Color get() = currentColors.destructive
val DestructiveSoft: Color get() = currentColors.destructiveContainer
val Success: Color get() = currentColors.success
val SuccessSoft: Color get() = currentColors.successContainer
val Warning: Color get() = currentColors.warning

val OnDevicePillBg: Color get() = currentColors.successContainer
val OnDevicePillText: Color get() = currentColors.onSuccessContainer


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// MATERIAL COLOR SCHEME
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private fun orbitColorScheme(colors: OrbitColors): ColorScheme =
    if (colors.isDark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.labelOnAccent,
            primaryContainer = colors.accentContainer,
            onPrimaryContainer = colors.onAccentContainer,
            secondary = colors.accent,
            onSecondary = colors.labelOnAccent,
            secondaryContainer = colors.selectedContainer,
            onSecondaryContainer = colors.onAccentContainer,
            tertiary = colors.info,
            onTertiary = colors.labelOnAccent,
            background = colors.background,
            onBackground = colors.labelPrimary,
            surface = colors.surface,
            onSurface = colors.labelPrimary,
            surfaceVariant = colors.surfaceInput,
            onSurfaceVariant = colors.labelSecondary,
            surfaceContainerLowest = colors.background,
            surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface,
            surfaceContainerHigh = colors.surfaceElevated,
            surfaceContainerHighest = colors.surfaceHigh,
            surfaceTint = colors.accent,
            outline = colors.border,
            outlineVariant = colors.separator,
            error = colors.destructive,
            onError = colors.labelOnAccent,
            errorContainer = colors.destructiveContainer,
            onErrorContainer = colors.onDestructiveContainer,
            scrim = colors.scrim,
            inverseSurface = colors.labelPrimary,
            inverseOnSurface = colors.background,
            inversePrimary = colors.onAccentContainer,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.labelOnAccent,
            primaryContainer = colors.accentContainer,
            onPrimaryContainer = colors.onAccentContainer,
            secondary = colors.accent,
            onSecondary = colors.labelOnAccent,
            secondaryContainer = colors.selectedContainer,
            onSecondaryContainer = colors.onAccentContainer,
            tertiary = colors.info,
            onTertiary = colors.labelOnAccent,
            background = colors.background,
            onBackground = colors.labelPrimary,
            surface = colors.surface,
            onSurface = colors.labelPrimary,
            surfaceVariant = colors.surfaceInput,
            onSurfaceVariant = colors.labelSecondary,
            surfaceContainerLowest = colors.surface,
            surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface,
            surfaceContainerHigh = colors.surfaceInput,
            surfaceContainerHighest = colors.surfaceHigh,
            surfaceTint = colors.accent,
            outline = colors.border,
            outlineVariant = colors.separator,
            error = colors.destructive,
            onError = Color.White,
            errorContainer = colors.destructiveContainer,
            onErrorContainer = colors.onDestructiveContainer,
            scrim = colors.scrim,
            inverseSurface = colors.inkPrimary,
            inverseOnSurface = colors.labelOnInk,
            inversePrimary = colors.accentContainer,
        )
    }


// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// TYPOGRAPHY BRIDGE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

val DisplayFont = OrbitUiFont

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ROOT THEME COMPOSABLE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun OrbitAITheme(
    appearance: com.example.orbitai.core.common.Appearance = com.example.orbitai.core.common.Appearance(),
    content: @Composable () -> Unit,
) {
    val isDarkTheme = appearance.isDark(androidx.compose.foundation.isSystemInDarkTheme())
    val context = androidx.compose.ui.platform.LocalContext.current
    val dynamic = if (appearance.dynamicColor) {
        if (isDarkTheme) androidx.compose.material3.dynamicDarkColorScheme(context)
        else androidx.compose.material3.dynamicLightColorScheme(context)
    } else null

    val motionPrefs = rememberSystemMotionPreferences()
    val colors = resolveOrbitColors(
        isDarkTheme = isDarkTheme,
        pureBlack = appearance.mode == com.example.orbitai.core.common.ThemeMode.BLACK,
        accent = dynamic,
        highContrast = motionPrefs.highContrast,
    )
    val typography = createOrbitTypography(colors)
    currentColors = colors

    CompositionLocalProvider(
        LocalOrbitColors provides colors,
        LocalOrbitTypography provides typography,
        LocalOrbitSpacing provides DefaultOrbitSpacing,
        LocalOrbitShapes provides DefaultOrbitShapes,
        LocalOrbitMotion provides motionPrefs,
        LocalAppearance provides appearance,
    ) {
        MaterialTheme(
            colorScheme = orbitColorScheme(colors),
            typography = typography.toMaterialTypography(),
            content = content,
        )
    }
}
