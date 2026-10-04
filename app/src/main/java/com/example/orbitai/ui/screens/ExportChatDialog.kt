package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.orbitai.feature.chat.Chat
import com.example.orbitai.feature.chat.export.ChatExport
import com.example.orbitai.feature.chat.export.ChatExporter
import com.example.orbitai.feature.chat.export.ExportFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Export a chat as Markdown, plain text or PDF, then share it or save it to the device. */
@Composable
fun ExportChatDialog(chat: Chat, modelName: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exporter = remember { ChatExporter(context.applicationContext) }
    var format by remember { mutableStateOf(ExportFormat.MARKDOWN) }
    var includeImages by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    val hasImages = remember(chat) { chat.messages.any { it.imageUris.isNotEmpty() } }

    fun run(work: suspend () -> Unit) {
        busy = true
        scope.launch {
            try {
                work()
                onDismiss()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                busy = false
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(format.mimeType)) { uri ->
        if (uri != null) run {
            exporter.saveTo(uri, chat, modelName, format, includeImages)
            Toast.makeText(context, "Chat saved", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.export_chat)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ExportFormat.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = format == option, enabled = !busy, role = Role.RadioButton) { format = option },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = format == option, onClick = null, enabled = !busy)
                        Spacer(Modifier.width(8.dp))
                        Text("${option.label} (.${option.extension})")
                    }
                }
                if (format == ExportFormat.PDF && hasImages) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = includeImages, enabled = !busy, role = Role.Checkbox) { includeImages = it },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = includeImages, onCheckedChange = null, enabled = !busy)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.include_images))
                    }
                }
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.exporting))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                run { context.startActivity(exporter.shareIntent(chat, modelName, format, includeImages)) }
            }) { Text(stringResource(R.string.share)) }
        },
        dismissButton = {
            Row {
                TextButton(enabled = !busy, onClick = { saveLauncher.launch(ChatExport.fileName(chat, format)) }) { Text(stringResource(R.string.save)) }
                TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
