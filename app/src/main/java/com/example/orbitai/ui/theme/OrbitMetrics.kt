package com.example.orbitai.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Consistent 4dp spacing grid and layout rhythm for OrbitAI.
 * Enforces aligned margins across all screens and minimum 48dp interactive touch targets.
 */
@Immutable
data class OrbitSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    /** Universal horizontal margin on every top-level and detail screen. */
    val screenHorizontal: Dp = 16.dp,
    /** Horizontal padding inside inset-grouped cards and rows. */
    val cardHorizontal: Dp = 16.dp,
    /** Vertical padding inside standard list rows. */
    val rowVertical: Dp = 12.dp,
    /** Vertical rhythm between distinct sections. */
    val sectionGap: Dp = 24.dp,
    /** Gap between a section header and its grouped card. */
    val sectionHeaderGap: Dp = 8.dp,
    /** Minimum accessible touch target dimension (HIG 44pt / Android 48dp). */
    val minTouchTarget: Dp = 48.dp,
)

/**
 * Concentric corner radii: inner elements use `outerRadius - insetPadding`
 * (e.g. `cardRadius = 16.dp` with `6.dp` inset -> `cardInnerRadius = 10.dp`).
 */
@Immutable
data class OrbitShapes(
    val cardRadius: Dp = 16.dp,
    val cardInnerRadius: Dp = 10.dp,
    val inputRadius: Dp = 12.dp,
    val buttonRadius: Dp = 12.dp,
    val chipRadius: Dp = 10.dp,
    val sheetRadius: Dp = 24.dp,
) {
    val card get() = RoundedCornerShape(cardRadius)
    val cardInner get() = RoundedCornerShape(cardInnerRadius)
    val input get() = RoundedCornerShape(inputRadius)
    val button get() = RoundedCornerShape(buttonRadius)
    val chip get() = RoundedCornerShape(chipRadius)
    val sheet get() = RoundedCornerShape(topStart = sheetRadius, topEnd = sheetRadius)
    val pill get() = CircleShape
}

val DefaultOrbitSpacing = OrbitSpacing()
val DefaultOrbitShapes = OrbitShapes()

val LocalOrbitSpacing = staticCompositionLocalOf { DefaultOrbitSpacing }
val LocalOrbitShapes = staticCompositionLocalOf { DefaultOrbitShapes }
