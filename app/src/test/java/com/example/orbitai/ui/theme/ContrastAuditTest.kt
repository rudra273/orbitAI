package com.example.orbitai.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Automated WCAG 2.2 AA contrast gate for every semantic color pair across
 * Light, Charcoal Dark, and Pure Black OLED palettes.
 *
 * - Normal text: >= 4.5:1
 * - Large text / icons / non-text UI components: >= 3.0:1
 */
class ContrastAuditTest {

    private fun linearize(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun relativeLuminance(color: Color): Double {
        return 0.2126 * linearize(color.red) +
            0.7152 * linearize(color.green) +
            0.0722 * linearize(color.blue)
    }

    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertContrast(
        themeName: String,
        pairName: String,
        fg: Color,
        bg: Color,
        minRatio: Double,
    ) {
        val actual = contrastRatio(fg, bg)
        assertTrue(
            "[$themeName] $pairName contrast is ${"%.2f".format(actual)}:1, expected >= $minRatio:1",
            actual >= minRatio,
        )
    }

    private fun auditPalette(name: String, c: OrbitColors) {
        val primarySurfaces = listOf(
            "background" to c.background,
            "surface" to c.surface,
            "surfaceElevated" to c.surfaceElevated,
            "surfaceInput" to c.surfaceInput,
        )

        // All primary/secondary/tertiary labels must pass WCAG AA (>= 4.5:1) on every surface layer.
        for ((surfaceName, bg) in primarySurfaces) {
            assertContrast(name, "labelPrimary on $surfaceName", c.labelPrimary, bg, 4.5)
            assertContrast(name, "labelSecondary on $surfaceName", c.labelSecondary, bg, 4.5)
            assertContrast(name, "labelTertiary on $surfaceName", c.labelTertiary, bg, 4.5)
            assertContrast(name, "accent on $surfaceName", c.accent, bg, 4.5)
            assertContrast(name, "success on $surfaceName", c.success, bg, 4.5)
            assertContrast(name, "warning on $surfaceName", c.warning, bg, 4.5)
            assertContrast(name, "destructive on $surfaceName", c.destructive, bg, 4.5)
            assertContrast(name, "info on $surfaceName", c.info, bg, 4.5)
        }

        // Foreground-on-container pairs must pass WCAG AA (>= 4.5:1).
        assertContrast(name, "labelOnAccent on accent", c.labelOnAccent, c.accent, 4.5)
        assertContrast(name, "labelOnInk on inkPrimary", c.labelOnInk, c.inkPrimary, 4.5)
        assertContrast(name, "onAccentContainer on accentContainer", c.onAccentContainer, c.accentContainer, 4.5)
        assertContrast(name, "onSuccessContainer on successContainer", c.onSuccessContainer, c.successContainer, 4.5)
        assertContrast(name, "onWarningContainer on warningContainer", c.onWarningContainer, c.warningContainer, 4.5)
        assertContrast(name, "onDestructiveContainer on destructiveContainer", c.onDestructiveContainer, c.destructiveContainer, 4.5)
        assertContrast(name, "onInfoContainer on infoContainer", c.onInfoContainer, c.infoContainer, 4.5)
        assertContrast(name, "userBubbleText on userBubbleFill", c.userBubbleText, c.userBubbleFill, 4.5)

        // Non-text UI controls (switches) must pass WCAG 2.2 Non-Text Contrast (>= 3.0:1).
        assertContrast(name, "switchTrackOn on surface", c.switchTrackOn, c.surface, 3.0)
        assertContrast(name, "switchTrackOff on surface", c.switchTrackOff, c.surface, 3.0)
        assertContrast(name, "switchThumbOn on switchTrackOn", c.switchThumbOn, c.switchTrackOn, 3.0)
        assertContrast(name, "switchThumbOff on switchTrackOff", c.switchThumbOff, c.switchTrackOff, 3.0)
    }

    @Test
    fun lightPaletteMeetsWcagAA() {
        auditPalette("Light", OrbitLightColors)
    }

    @Test
    fun darkCharcoalPaletteMeetsWcagAA() {
        auditPalette("Dark", OrbitDarkColors)
    }

    @Test
    fun pureBlackPaletteMeetsWcagAA() {
        auditPalette("PureBlack", OrbitBlackColors)
    }

    @Test
    fun darkModeUsesLighterElevatedLayers() {
        val bgLum = relativeLuminance(OrbitDarkColors.background)
        val surfaceLum = relativeLuminance(OrbitDarkColors.surface)
        val elevatedLum = relativeLuminance(OrbitDarkColors.surfaceElevated)
        val highLum = relativeLuminance(OrbitDarkColors.surfaceHigh)
        assertTrue("Dark surface must be lighter than background", surfaceLum > bgLum)
        assertTrue("Dark surfaceElevated must be lighter than surface", elevatedLum > surfaceLum)
        assertTrue("Dark surfaceHigh must be lighter than surfaceElevated", highLum > elevatedLum)
    }
}
