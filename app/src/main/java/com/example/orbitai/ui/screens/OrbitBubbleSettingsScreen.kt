package com.example.orbitai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.orbitai.data.ToolSettingsStore
import com.example.orbitai.tools.bubble.OrbitBubbleService
import com.example.orbitai.ui.theme.GlassWhite20
import com.example.orbitai.ui.theme.TextMuted
import com.example.orbitai.ui.theme.TextPrimary
import com.example.orbitai.ui.theme.VioletBright
import com.example.orbitai.ui.theme.VioletCore
import com.example.orbitai.ui.screens.OrbitSlider

@Composable
fun OrbitBubbleSettingsScreen(
    toolSettingsStore: ToolSettingsStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var bubbleEnabled by remember { mutableStateOf(toolSettingsStore.isFloatingBubbleEnabled) }
    var bubbleSizeDp by remember { mutableIntStateOf(toolSettingsStore.bubbleSizeDp) }
    var responseHeightDp by remember { mutableIntStateOf(toolSettingsStore.bubbleResponseHeightDp) }
    var bubbleIdleAlphaPercent by remember { mutableIntStateOf(toolSettingsStore.bubbleIdleAlphaPercent) }
    var bubbleStyle by remember { mutableStateOf(toolSettingsStore.bubbleStyle) }
    var resultsInOverlay by remember { mutableStateOf(toolSettingsStore.bubbleResultsInOverlay) }
    var overlayGranted by remember { mutableStateOf(OrbitBubbleService.canDrawOverlays(context)) }
    var audioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var pendingBubbleEnable by remember { mutableStateOf(false) }

    fun updateSize(newSize: Int) {
        bubbleSizeDp = newSize
        toolSettingsStore.bubbleSizeDp = newSize
        if (bubbleEnabled) OrbitBubbleService.start(context)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        audioGranted = granted
        if (granted && pendingBubbleEnable && overlayGranted) {
            bubbleEnabled = true
            toolSettingsStore.isFloatingBubbleEnabled = true
            OrbitBubbleService.start(context)
        } else if (!granted) {
            bubbleEnabled = false
            toolSettingsStore.isFloatingBubbleEnabled = false
            OrbitBubbleService.stop(context)
        }
        pendingBubbleEnable = false
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        overlayGranted = OrbitBubbleService.canDrawOverlays(context)
        if (!overlayGranted) {
            pendingBubbleEnable = false
            bubbleEnabled = false
            toolSettingsStore.isFloatingBubbleEnabled = false
            OrbitBubbleService.stop(context)
        } else if (pendingBubbleEnable) {
            if (audioGranted) {
                bubbleEnabled = true
                toolSettingsStore.isFloatingBubbleEnabled = true
                OrbitBubbleService.start(context)
                pendingBubbleEnable = false
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(Unit) {
        overlayGranted = OrbitBubbleService.canDrawOverlays(context)
        audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        bubbleEnabled = toolSettingsStore.isFloatingBubbleEnabled && overlayGranted && audioGranted
        bubbleSizeDp = toolSettingsStore.bubbleSizeDp
        responseHeightDp = toolSettingsStore.bubbleResponseHeightDp
        bubbleIdleAlphaPercent = toolSettingsStore.bubbleIdleAlphaPercent
        bubbleStyle = toolSettingsStore.bubbleStyle
        resultsInOverlay = toolSettingsStore.bubbleResultsInOverlay
        if (bubbleEnabled) OrbitBubbleService.start(context)
    }

    fun updateBubble(enabled: Boolean) {
        if (!enabled) {
            pendingBubbleEnable = false
            bubbleEnabled = false
            toolSettingsStore.isFloatingBubbleEnabled = false
            OrbitBubbleService.stop(context)
            return
        }

        pendingBubbleEnable = true
        if (!overlayGranted) {
            overlayPermissionLauncher.launch(OrbitBubbleService.overlayPermissionIntent(context))
            return
        }
        if (!audioGranted) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        bubbleEnabled = true
        toolSettingsStore.isFloatingBubbleEnabled = true
        OrbitBubbleService.start(context)
        pendingBubbleEnable = false
    }

    SettingsSubScreen(
        title = "Orbit Bubble",
        icon = Icons.Default.ChatBubble,
        accent = Color(0xFFF59E0B),
        onBack = onBack,
    ) {
        SettingsDescription("Manage floating bubble visibility and behavior over other apps.")

        GlassCard(accent = Color(0xFFF59E0B)) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                RowSetting(
                    title = "Enable bubble",
                    subtitle = if (bubbleEnabled) {
                        "Bubble is active over other apps. Tap to speak and long press to dismiss."
                    } else {
                        "Turn on to show a draggable floating bubble over other apps."
                    },
                    trailing = {
                        Switch(
                            checked = bubbleEnabled,
                            onCheckedChange = ::updateBubble,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF59E0B),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = GlassWhite20,
                            ),
                        )
                    },
                )

                OrbitDivider()

                RowSetting(
                    title = "Response mode",
                    subtitle = if (resultsInOverlay) {
                        "Response appears directly over the current app."
                    } else {
                        "Response opens inside Orbit chat app."
                    },
                    trailing = {
                        Switch(
                            checked = resultsInOverlay,
                            onCheckedChange = {
                                resultsInOverlay = it
                                toolSettingsStore.bubbleResultsInOverlay = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF59E0B),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = GlassWhite20,
                            ),
                        )
                    },
                )

                OrbitDivider()

                RowSetting(
                    title = "Bubble style",
                    subtitle = "Round bubble or right-side slide tab.",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SizeChip("Round", bubbleStyle == "round") {
                                bubbleStyle = "round"
                                toolSettingsStore.bubbleStyle = "round"
                                if (bubbleEnabled) OrbitBubbleService.start(context)
                            }
                            SizeChip("Slide", bubbleStyle == "slide") {
                                bubbleStyle = "slide"
                                toolSettingsStore.bubbleStyle = "slide"
                                if (bubbleEnabled) OrbitBubbleService.start(context)
                            }
                        }
                    },
                )

                OrbitDivider()

                RowSetting(
                    title = "Bubble size",
                    subtitle = "Choose small, medium, or large size.",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SizeChip("S", bubbleSizeDp == OrbitBubbleService.SIZE_SMALL_DP) { updateSize(OrbitBubbleService.SIZE_SMALL_DP) }
                            SizeChip("M", bubbleSizeDp == OrbitBubbleService.SIZE_MEDIUM_DP) { updateSize(OrbitBubbleService.SIZE_MEDIUM_DP) }
                            SizeChip("L", bubbleSizeDp == OrbitBubbleService.SIZE_LARGE_DP) { updateSize(OrbitBubbleService.SIZE_LARGE_DP) }
                        }
                    },
                )

                OrbitDivider()

                OrbitSlider(
                    label = "Response window height",
                    value = responseHeightDp.toFloat(),
                    valueStr = "${responseHeightDp} dp",
                    range = 70f..360f,
                    steps = 10,
                    accent = Color(0xFFF59E0B),
                    hint = "Adjust overlay output panel height.",
                    onChange = { value ->
                        responseHeightDp = value.toInt()
                        toolSettingsStore.bubbleResponseHeightDp = responseHeightDp
                    },
                )

                OrbitDivider()

                OrbitSlider(
                    label = "Idle transparency",
                    value = bubbleIdleAlphaPercent.toFloat(),
                    valueStr = "${bubbleIdleAlphaPercent}%",
                    range = 20f..100f,
                    steps = 15,
                    accent = Color(0xFFF59E0B),
                    hint = "How visible the bubble stays when not interacting.",
                    onChange = { value ->
                        bubbleIdleAlphaPercent = value.toInt()
                        toolSettingsStore.bubbleIdleAlphaPercent = bubbleIdleAlphaPercent
                        if (bubbleEnabled) OrbitBubbleService.start(context)
                    },
                )

                OrbitDivider()

                RowSetting(
                    title = "Permissions",
                    subtitle = buildString {
                        append("Overlay: ")
                        append(if (overlayGranted) "granted" else "required")
                        append(" • Microphone: ")
                        append(if (audioGranted) "granted" else "required")
                    },
                )
            }
        }
    }
}

@Composable
private fun SizeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) VioletCore.copy(alpha = 0.20f) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) VioletBright else GlassWhite20,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) VioletBright else TextMuted,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun RowSetting(
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}
