package com.example.orbitai.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.orbitai.feature.reporting.ContentReportRepository
import kotlinx.coroutines.launch

@Composable
fun ContentReportDialog(responseText: String, source: String, onDismiss: () -> Unit) {
    val repository = remember { ContentReportRepository() }
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = { Text(if (sent) "Report sent" else "Report this AI response") },
        text = {
            Column {
                if (sent) {
                    Text("Thank you. Your report was submitted to the developer for review.")
                } else {
                    Text("Submit the selected AI response and your optional note to the developer for safety review. They may contain personal information. Submit only what you want to share.")
                    Spacer(androidx.compose.ui.Modifier.height(12.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(2000) },
                        label = { Text("Reason (optional)") },
                        enabled = !isSending,
                        maxLines = 4,
                    )
                    error?.let { Text(it) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !isSending, onClick = {
                if (sent) {
                    onDismiss()
                } else {
                    isSending = true
                    error = null
                    scope.launch {
                        repository.submit(responseText, note, source)
                            .onSuccess { sent = true }
                            .onFailure { error = it.message ?: "Report could not be sent. Please try again." }
                        isSending = false
                    }
                }
            }) { Text(if (sent) "Done" else if (isSending) "Sending…" else "Submit report") }
        },
        dismissButton = {
            if (!sent) TextButton(enabled = !isSending, onClick = onDismiss) { Text("Cancel") }
        },
    )
}
