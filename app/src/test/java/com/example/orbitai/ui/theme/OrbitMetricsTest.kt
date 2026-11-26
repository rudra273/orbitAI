package com.example.orbitai.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitMetricsTest {

    @Test
    fun spacingFollowsFourDpGridAndMinimumTouchTarget() {
        val s = DefaultOrbitSpacing
        listOf(s.xxs, s.xs, s.sm, s.md, s.lg, s.xl, s.xxl, s.screenHorizontal, s.cardHorizontal, s.rowVertical, s.sectionGap, s.sectionHeaderGap).forEach { dp ->
            assertEquals("Expected $dp to lie on 4dp grid", 0f, dp.value % 4f, 0.001f)
        }
        assertTrue("Minimum touch target must be >= 48dp", s.minTouchTarget.value >= 48f)
    }

    @Test
    fun concentricShapesPreserveNestingGeometry() {
        val shapes = DefaultOrbitShapes
        assertTrue("Inner card radius must be smaller than outer card radius", shapes.cardInnerRadius < shapes.cardRadius)
        assertTrue("Sheet radius must be >= card radius", shapes.sheetRadius >= shapes.cardRadius)
    }

    @Test
    fun typographyEnforcesMinimumLegibleCaptionAndTabularHelper() {
        val type = createOrbitTypography(OrbitLightColors)
        assertTrue("Smallest caption must be >= 12sp", type.caption1.fontSize.value >= 12f)
        assertTrue("Section header must be >= 12sp", type.sectionHeader.fontSize.value >= 12f)
        assertEquals("tnum", type.body.tabular().fontFeatureSettings)
    }

    @Test
    fun reduceMotionCollapsesSpringsToInstantSnap() {
        val reduced = OrbitMotionPreferences(reduceMotion = true)
        val normal = OrbitMotionPreferences(reduceMotion = false)
        assertTrue(reduced.standardSpring<Float>() is androidx.compose.animation.core.SnapSpec)
        assertTrue(reduced.snappySpring<Float>() is androidx.compose.animation.core.SnapSpec)
        assertTrue(normal.standardSpring<Float>() is androidx.compose.animation.core.SpringSpec)
    }

    @Test
    fun highContrastStrengthensBordersAndSeparators() {
        val darkHc = OrbitDarkColors.withHighContrast()
        val lightHc = OrbitLightColors.withHighContrast()
        assertTrue(darkHc.separator != OrbitDarkColors.separator)
        assertTrue(lightHc.separator != OrbitLightColors.separator)
    }
}
