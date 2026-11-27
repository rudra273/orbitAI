package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.orbitai.feature.reporting.ContentReportRepository
import kotlinx.coroutines.launch
import java.util.UUID

private const val MAX_REPORT_RESPONSE_LENGTH = 16_000

@Composable
fun ContentReportDialog(responseText: String, source: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { ContentReportRepository(context) }
    val excerpt = remember(responseText) {
        responseText.replace("\u0000", "").take(MAX_REPORT_RESPONSE_LENGTH).dropLastWhile(Char::isHighSurrogate)
    }
    val submissionId = rememberSaveable(responseText, source) { UUID.randomUUID().toString() }
    val scope = rememberCoroutineScope()
    var note by rememberSaveable { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var receipt by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val sent = receipt != null

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = { Text(if (sent) "Report sent" else "Report this AI response") },
        text = {
            Column {
                if (sent) {
                    Text(stringResource(R.string.thank_you_your_report_was))
                    Spacer(Modifier.height(8.dp))
                    SelectionContainer { Text("Report ID: $receipt") }
                } else {
                    Text(stringResource(R.string.submit_the_selected_ai_response))
                    if (responseText.length > MAX_REPORT_RESPONSE_LENGTH) {
                        Text("This report includes up to the first 16,000 characters of the reply.")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = excerpt,
                        modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.replace("\u0000", "").take(2000).dropLastWhile(Char::isHighSurrogate) },
                        label = { Text(stringResource(R.string.reason_optional)) },
                        enabled = !isSending,
                        maxLines = 4,
                    )
                    error?.let { Text(it) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !isSending && (sent || excerpt.isNotBlank()), onClick = {
                if (sent) {
                    onDismiss()
                } else {
                    isSending = true
                    error = null
                    scope.launch {
                        repository.submit(excerpt, note, source, submissionId)
                            .onSuccess { receipt = it }
                            .onFailure { error = it.message ?: "Report could not be sent. Please try again." }
                        isSending = false
                    }
                }
            }) { Text(if (sent) "Done" else if (isSending) "Sending…" else "Submit report") }
        },
        dismissButton = {
            if (!sent) TextButton(enabled = !isSending, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
