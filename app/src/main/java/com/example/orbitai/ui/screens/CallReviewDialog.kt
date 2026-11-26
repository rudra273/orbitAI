package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.orbitai.viewmodel.CallReviewRequest
import com.example.orbitai.feature.automation.executor.CallContact

@Composable
fun CallReviewDialog(request: CallReviewRequest, error: String?, onDismiss: () -> Unit, onCall: (CallContact, Boolean) -> Unit) {
    var selected by remember(request) { mutableStateOf(request.selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.call_contact)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(if (request.selected != null) "Confirm the matching contact and number." else "Several contacts match. Choose the number to call.")
                request.contacts.forEach { contact ->
                    Row {
                        RadioButton(selected = selected == contact, onClick = { selected = contact })
                        TextButton(onClick = { selected = contact }) { Text("${contact.name}\n${contact.number}") }
                    }
                }
                error?.let { Text(it) }
            }
        },
        confirmButton = { TextButton(enabled = selected != null, onClick = { selected?.let { onCall(it, false) } }) { Text(stringResource(R.string.call)) } },
        dismissButton = {
            Row {
                TextButton(enabled = selected != null, onClick = { selected?.let { onCall(it, true) } }) { Text(stringResource(R.string.open_dialer)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
