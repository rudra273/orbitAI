package com.example.orbitai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.orbitai.data.InferenceSettings
import com.example.orbitai.data.InferenceSettingsStore

@Composable
fun InferenceSettingsScreen(
    inferenceStore: InferenceSettingsStore,
    onBack: () -> Unit,
) {
    val saved = remember { inferenceStore.get() }
    var temperature by remember { mutableFloatStateOf(saved.temperature) }
    var topK by remember { mutableIntStateOf(saved.topK) }
    var topP by remember { mutableFloatStateOf(saved.topP) }
    var maxDecodedTokens by remember { mutableIntStateOf(saved.maxDecodedTokens) }
    var isDirty by remember { mutableStateOf(false) }
    var saved2 by remember { mutableStateOf(false) }

    SettingsSubScreen(
        title = "Inference",
        icon = Icons.Default.Tune,
        accent = Color(0xFF60A5FA),
        onBack = onBack,
    ) {
        SettingsDescription("Controls how the AI generates responses. Changes take effect on the next message.")

        GlassCard(accent = Color(0xFF60A5FA)) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                OrbitSlider(
                    label = "Temperature",
                    value = temperature,
                    valueStr = "%.2f".format(temperature),
                    range = 0.1f..2.0f,
                    accent = Color(0xFF60A5FA),
                    hint = "Higher = more creative, lower = more focused",
                    onChange = { temperature = it; isDirty = true; saved2 = false },
                )
                OrbitDivider()
                OrbitSlider(
                    label = "Top-K",
                    value = topK.toFloat(),
                    valueStr = topK.toString(),
                    range = 1f..100f,
                    steps = 98,
                    accent = Color(0xFF60A5FA),
                    hint = "Tokens considered at each step",
                    onChange = { topK = it.toInt(); isDirty = true; saved2 = false },
                )
                OrbitDivider()
                OrbitSlider(
                    label = "Top-P",
                    value = topP,
                    valueStr = "%.2f".format(topP),
                    range = 0.1f..1.0f,
                    accent = Color(0xFF60A5FA),
                    hint = "Nucleus sampling threshold",
                    onChange = { topP = it; isDirty = true; saved2 = false },
                )
                OrbitDivider()
                OrbitSlider(
                    label = "Max output tokens",
                    value = maxDecodedTokens.toFloat(),
                    valueStr = maxDecodedTokens.toString(),
                    range = 128f..2048f,
                    steps = 14,
                    accent = Color(0xFF60A5FA),
                    hint = "Maximum length of each response",
                    onChange = {
                        maxDecodedTokens = (it / 128).toInt() * 128
                        isDirty = true
                        saved2 = false
                    },
                )
            }
        }

        OrbitPrimaryButton(
            label = if (saved2) "Applied" else "Apply Settings",
            enabled = isDirty,
            accent = Color(0xFF60A5FA),
            onClick = {
                inferenceStore.save(InferenceSettings(temperature, topK, topP, maxDecodedTokens))
                isDirty = false
                saved2 = true
            },
        )
    }
}
