package com.example.orbitai.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Accessibility-aware motion & contrast preferences read from Android system settings:
 * - [reduceMotion]: true when `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`
 *   (Android "Remove animations" / Reduce Motion).
 * - [highContrast]: true when system high-contrast text is enabled.
 */
@Immutable
data class OrbitMotionPreferences(
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false,
) {
    /** Smooth, non-bouncy HIG spring; collapses to instant [snap] when [reduceMotion] is active. */
    fun <T> standardSpring(): AnimationSpec<T> =
        if (reduceMotion) snap()
        else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )

    /** Snappy spring for tactile micro-interactions; collapses to [snap] when [reduceMotion] is active. */
    fun <T> snappySpring(): AnimationSpec<T> =
        if (reduceMotion) snap()
        else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        )
}

fun readSystemMotionPreferences(context: Context): OrbitMotionPreferences {
    val resolver = context.contentResolver
    val animatorScale = runCatching {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    val highTextContrast = runCatching {
        Settings.Secure.getInt(resolver, "high_text_contrast_enabled", 0) == 1
    }.getOrDefault(false)
    return OrbitMotionPreferences(
        reduceMotion = animatorScale == 0f,
        highContrast = highTextContrast,
    )
}

@Composable
fun rememberSystemMotionPreferences(): OrbitMotionPreferences {
    val context = LocalContext.current
    return remember(context) { readSystemMotionPreferences(context) }
}

val LocalOrbitMotion = staticCompositionLocalOf { OrbitMotionPreferences() }
