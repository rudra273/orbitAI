package com.example.orbitai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.orbitai.R

/**
 * Anthropic-inspired editorial typography for OrbitAI:
 * - [OrbitEditorialFont] (Source Serif 4): screen titles, headlines, empty states, and AI headings.
 * - [OrbitUiFont] (Inter): UI chrome, controls, lists, body text, and chat messages.
 * - [OrbitMonoFont]: code blocks, slash commands, model specs, and tabular figures.
 *
 * All fonts are bundled offline in `res/font/`.
 */
@OptIn(ExperimentalTextApi::class)
val OrbitEditorialFont = FontFamily(
    Font(
        resId = R.font.source_serif_4_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.source_serif_4_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.source_serif_4_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.source_serif_4_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

@OptIn(ExperimentalTextApi::class)
val OrbitUiFont = FontFamily(
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.inter_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

val OrbitMonoFont = FontFamily.Monospace

/** Enables tabular (fixed-width) numerals so changing counters, sizes, and timers never jitter. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/**
 * Apple HIG semantic type hierarchy with Anthropic-inspired editorial serif titles
 * and clean Inter UI copy. Minimum readable size is 12.sp to preserve legibility
 * across Dynamic Type scales.
 */
@Immutable
data class OrbitTypography(
    val largeTitle: TextStyle,
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val subheadline: TextStyle,
    val footnote: TextStyle,
    val caption1: TextStyle,
    val sectionHeader: TextStyle,
    val code: TextStyle,
)

fun createOrbitTypography(colors: OrbitColors): OrbitTypography = OrbitTypography(
    largeTitle = TextStyle(
        fontFamily = OrbitEditorialFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        color = colors.labelPrimary,
    ),
    title1 = TextStyle(
        fontFamily = OrbitEditorialFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp,
        color = colors.labelPrimary,
    ),
    title2 = TextStyle(
        fontFamily = OrbitEditorialFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.25).sp,
        color = colors.labelPrimary,
    ),
    title3 = TextStyle(
        fontFamily = OrbitUiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.15).sp,
        color = colors.labelPrimary,
    ),
    headline = TextStyle(
        fontFamily = OrbitUiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp,
        color = colors.labelPrimary,
    ),
    body = TextStyle(
        fontFamily = OrbitUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp,
        color = colors.labelPrimary,
    ),
    callout = TextStyle(
        fontFamily = OrbitUiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        color = colors.labelPrimary,
    ),
    subheadline = TextStyle(
        fontFamily = OrbitUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        color = colors.labelSecondary,
    ),
    footnote = TextStyle(
        fontFamily = OrbitUiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.05.sp,
        color = colors.labelSecondary,
    ),
    caption1 = TextStyle(
        fontFamily = OrbitUiFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
        color = colors.labelTertiary,
    ),
    sectionHeader = TextStyle(
        fontFamily = OrbitUiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
        color = colors.labelSecondary,
    ),
    code = TextStyle(
        fontFamily = OrbitMonoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        fontFeatureSettings = "tnum",
        color = colors.labelPrimary,
    ),
)

/** Bridges [OrbitTypography] onto Material 3's [Typography] slots. */
fun OrbitTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = largeTitle,
    displayMedium = title1,
    displaySmall = title2,
    headlineLarge = title2,
    headlineMedium = title3,
    headlineSmall = headline,
    titleLarge = title3,
    titleMedium = headline,
    titleSmall = callout,
    bodyLarge = body,
    bodyMedium = subheadline,
    bodySmall = footnote,
    labelLarge = callout,
    labelMedium = caption1,
    labelSmall = caption1,
)

val LocalOrbitTypography = staticCompositionLocalOf { createOrbitTypography(OrbitDarkColors) }
