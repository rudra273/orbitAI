package com.example.orbitai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Purposeful, subtle haptic feedback helper for OrbitAI interactions.
 * Keeps tactile cues consistent across tabs, switches, destructive confirmations,
 * long-press menus, and message actions.
 */
@Stable
class OrbitHaptics(private val haptic: HapticFeedback) {
    fun selection() = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun toggle() = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun confirm() = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    fun longPress() = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberOrbitHaptics(): OrbitHaptics {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { OrbitHaptics(haptic) }
}
