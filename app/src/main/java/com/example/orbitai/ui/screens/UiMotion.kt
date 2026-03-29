package com.example.orbitai.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

@Composable
fun StaggeredFadeSlide(
    index: Int,
    content: @Composable () -> Unit,
) {
    val delayMs = (index * 50).coerceAtMost(400)
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(index) {
        delay(delayMs.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "stagger_alpha",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
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

