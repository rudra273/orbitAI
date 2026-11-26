package com.example.orbitai.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.orbitai.ui.theme.OrbitTheme
import kotlinx.coroutines.delay

/**
 * Subtle staggered entrance using HIG spring motion.
 * Automatically collapses to an immediate, non-animated render when Reduce Motion is enabled.
 */
@Composable
fun StaggeredFadeSlide(
    index: Int,
    content: @Composable () -> Unit,
) {
    val motion = OrbitTheme.motion
    if (motion.reduceMotion) {
        content()
        return
    }

    val delayMs = (index * 40).coerceAtMost(240)
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(index) {
        delay(delayMs.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = motion.standardSpring(),
        label = "stagger_alpha",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 18f,
        animationSpec = motion.standardSpring(),
        label = "stagger_y",
    )

    Box(
        modifier = Modifier.graphicsLayer(
            alpha = alpha,
            translationY = offsetY,
        ),
    ) {
        content()
    }
}
