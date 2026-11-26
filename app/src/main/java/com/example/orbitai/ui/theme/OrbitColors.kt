package com.example.orbitai.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for OrbitAI following Apple HIG layered neutral surfaces
 * (Light grouped `#F2F2F7` / `#FFFFFF`, Charcoal Dark `#141416` -> `#1C1C1E` -> `#2C2C2E` -> `#3A3A3C`,
 * and Pure Black OLED `#000000` -> `#1C1C1E` -> `#2C2C2E`) paired with OrbitAI's semantic accents.
 *
 * Every text/background pair is verified to meet WCAG 2.2 AA (>= 4.5:1) and non-text UI elements
 * (>= 3.0:1) by `ContrastAuditTest`.
 */
@Immutable
data class OrbitColors(
    val isDark: Boolean,
    // Layered backgrounds & surfaces
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceInput: Color,
    val surfaceHigh: Color,
    val scrim: Color,

    // Separators & borders
    val separator: Color,
    val separatorSubtle: Color,
    val border: Color,
    val borderFocused: Color,

    // Labels (text hierarchy)
    val labelPrimary: Color,
    val labelSecondary: Color,
    val labelTertiary: Color,
    val labelDisabled: Color,
    val labelOnAccent: Color,
    val labelOnInk: Color,

    // Brand & interactive states
    val inkPrimary: Color,
    val accent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val accentBorder: Color,
    val pressedOverlay: Color,
    val selectedContainer: Color,

    // Semantic status
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val destructive: Color,
    val destructiveContainer: Color,
    val onDestructiveContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,

    // Chat bubbles
    val userBubbleFill: Color,
    val userBubbleText: Color,

    // Switch / toggle states
    val switchTrackOn: Color,
    val switchTrackOff: Color,
    val switchThumbOn: Color,
    val switchThumbOff: Color,
) {
    /** Strengthens borders and separators for high-contrast / reduce-transparency contexts. */
    fun withHighContrast(): OrbitColors = if (isDark) {
        copy(
            separator = Color(0xFF545458),
            separatorSubtle = Color(0xFF48484A),
            border = Color(0xFF636366),
            scrim = Color(0xCC000000),
        )
    } else {
        copy(
            separator = Color(0xFF8E8E93),
            separatorSubtle = Color(0xFFAEAEB2),
            border = Color(0xFF636366),
            scrim = Color(0x80000000),
        )
    }

    /** Applies Material You wallpaper accent colors when the user enables Dynamic Color. */
    fun withDynamicAccent(scheme: ColorScheme): OrbitColors = copy(
        accent = scheme.primary,
        labelOnAccent = scheme.onPrimary,
        accentContainer = scheme.primaryContainer,
        onAccentContainer = scheme.onPrimaryContainer,
        accentBorder = scheme.outline,
        borderFocused = scheme.primary,
        selectedContainer = scheme.secondaryContainer,
        switchTrackOn = scheme.primary,
        switchThumbOn = scheme.onPrimary,
    )
}

/** Apple HIG Light grouped neutral palette + OrbitAI accents. */
val OrbitLightColors = OrbitColors(
    isDark = false,
    background = Color(0xFFF2F2F7),       // Apple systemGroupedBackground
    surface = Color(0xFFFFFFFF),          // Apple secondarySystemGroupedBackground
    surfaceElevated = Color(0xFFFFFFFF),  // Sheets, menus, dialogs
    surfaceInput = Color(0xFFE5E5EA),     // Apple systemGray5
    surfaceHigh = Color(0xFFD1D1D6),      // Apple systemGray4
    scrim = Color(0x66000000),

    separator = Color(0xFFC6C6C8),        // Apple opaque separator
    separatorSubtle = Color(0x243C3C43),
    border = Color(0x333C3C43),
    borderFocused = Color(0xFF5346E0),

    labelPrimary = Color(0xFF000000),
    labelSecondary = Color(0xFF48484D),   // >= 7.2:1 on all light surfaces
    labelTertiary = Color(0xFF5C5C60),    // >= 5.3:1 on all light surfaces
    labelDisabled = Color(0xFF8E8E93),
    labelOnAccent = Color(0xFFFFFFFF),
    labelOnInk = Color(0xFFFFFFFF),

    inkPrimary = Color(0xFF1C1C1E),
    accent = Color(0xFF5346E0),           // OrbitAI semantic Violet (>= 4.5:1 on all light surfaces)
    accentContainer = Color(0xFFECEAFF),
    onAccentContainer = Color(0xFF3E33B8),
    accentBorder = Color(0xFFC5BFFF),
    pressedOverlay = Color(0x14000000),
    selectedContainer = Color(0xFFECEAFF),

    success = Color(0xFF156E31),
    successContainer = Color(0xFFE6F6EC),
    onSuccessContainer = Color(0xFF145E2B),
    warning = Color(0xFF9C4F00),
    warningContainer = Color(0xFFFFF3E0),
    onWarningContainer = Color(0xFF7A3D00),
    destructive = Color(0xFFC4181F),
    destructiveContainer = Color(0xFFFDECEC),
    onDestructiveContainer = Color(0xFF9E151B),
    info = Color(0xFF005EC4),
    infoContainer = Color(0xFFE7F2FF),
    onInfoContainer = Color(0xFF004594),

    userBubbleFill = Color(0xFF1C1C1E),
    userBubbleText = Color(0xFFFFFFFF),

    switchTrackOn = Color(0xFF5346E0),
    switchTrackOff = Color(0xFF8E8E93),   // >= 3.3:1 against #FFFFFF
    switchThumbOn = Color(0xFFFFFFFF),
    switchThumbOff = Color(0xFFFFFFFF),
)

/** Apple HIG Charcoal Dark layered neutral palette + OrbitAI accents. */
val OrbitDarkColors = OrbitColors(
    isDark = true,
    background = Color(0xFF141416),       // Charcoal base
    surface = Color(0xFF1C1C1E),          // Apple systemGray6 (dark)
    surfaceElevated = Color(0xFF2C2C2E),  // Apple systemGray5 (dark)
    surfaceInput = Color(0xFF2C2C2E),     // Apple systemGray5 (dark)
    surfaceHigh = Color(0xFF3A3A3C),      // Apple systemGray4 (dark)
    scrim = Color(0xB3000000),

    separator = Color(0xFF38383A),        // Apple dark opaque separator
    separatorSubtle = Color(0x2EFFFFFF),
    border = Color(0x3DFFFFFF),
    borderFocused = Color(0xFFA89EFF),

    labelPrimary = Color(0xFFF5F5F7),
    labelSecondary = Color(0xFFAEAEB2),   // >= 6.3:1 on all dark layers
    labelTertiary = Color(0xFF98989D),    // >= 4.8:1 on all dark layers
    labelDisabled = Color(0xFF636366),
    labelOnAccent = Color(0xFF141416),
    labelOnInk = Color(0xFF141416),

    inkPrimary = Color(0xFFF5F5F7),
    accent = Color(0xFFA89EFF),           // OrbitAI semantic Violet (dark)
    accentContainer = Color(0xFF28244A),
    onAccentContainer = Color(0xFFD1CCFF),
    accentBorder = Color(0xFF4A428A),
    pressedOverlay = Color(0x1AFFFFFF),
    selectedContainer = Color(0xFF28244A),

    success = Color(0xFF34C759),
    successContainer = Color(0xFF163320),
    onSuccessContainer = Color(0xFF7BE897),
    warning = Color(0xFFFFD60A),
    warningContainer = Color(0xFF3A2D0C),
    onWarningContainer = Color(0xFFFFE45E),
    destructive = Color(0xFFFF6961),      // Apple dark high-contrast red (>= 4.6:1 on #2C2C2E)
    destructiveContainer = Color(0xFF3D191B),
    onDestructiveContainer = Color(0xFFFF9E99),
    info = Color(0xFF64D2FF),
    infoContainer = Color(0xFF142E3F),
    onInfoContainer = Color(0xFFA6E5FF),

    userBubbleFill = Color(0xFFF5F5F7),
    userBubbleText = Color(0xFF141416),

    switchTrackOn = Color(0xFFA89EFF),
    switchTrackOff = Color(0xFF6E6E73),   // >= 3.4:1 against #1C1C1E surface
    switchThumbOn = Color(0xFF141416),
    switchThumbOff = Color(0xFFF5F5F7),
)

/** Apple HIG Pure Black OLED dark mode: true-black base with standard dark gray elevated layers. */
val OrbitBlackColors = OrbitDarkColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceElevated = Color(0xFF2C2C2E),
    surfaceInput = Color(0xFF2C2C2E),
    surfaceHigh = Color(0xFF3A3A3C),
)

val LocalOrbitColors = staticCompositionLocalOf { OrbitDarkColors }
