package com.example.orbitai.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun BubbleDisclosureDialog(onContinue: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Enable Orbit Bubble?") },
        text = {
            Text("Orbit displays a floating assistant over other apps and uses the microphone when you tap to speak. It prefers on-device speech recognition; your device's speech provider may process audio online otherwise. Screen capture asks for Android approval each time and is analyzed by a local model. You can stop the bubble from its notification or in Settings.")
        },
        confirmButton = { TextButton(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
