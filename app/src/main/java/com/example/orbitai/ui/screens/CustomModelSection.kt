package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.orbitai.core.model.CustomModelImporter
import com.example.orbitai.core.model.CustomModelRequest
import com.example.orbitai.core.model.CustomModelSpec
import com.example.orbitai.core.model.CustomModels
import com.example.orbitai.core.model.PromptStyle
import com.example.orbitai.core.model.formatModelSize
import kotlinx.coroutines.launch

/** Settings > Models: import a `.litertlm` / `.task` file and manage imported models (f021). */
@Composable
fun CustomModelSection() {
    val context = LocalContext.current
    val importer = remember { CustomModelImporter(context.applicationContext) }
    val models by CustomModels.models.collectAsState()
    val scope = rememberCoroutineScope()

    var picked by remember { mutableStateOf<Uri?>(null) }
    var pickedName by remember { mutableStateOf("") }
    var pickedSize by remember { mutableLongStateOf(-1L) }
    var name by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(PromptStyle.GEMMA) }
    var vision by remember { mutableStateOf(false) }
    var contextText by remember { mutableStateOf("4096") }
    var importing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<CustomModelSpec?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val (fileName, size) = importer.describe(uri)
            picked = uri
            pickedName = fileName
            pickedSize = size
            name = fileName.substringBeforeLast('.')
            error = if (CustomModelSpec.formatOf(fileName) == null) "Pick a .litertlm or .task model file." else null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.your_models), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp))
        models.forEach { spec ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(spec.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Custom · ${spec.promptStyle.name.lowercase()} · ${spec.contextTokens} tokens" + if (spec.supportsVision) " · vision" else "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { deleting = spec }) { Text(stringResource(R.string.delete)) }
                }
            }
        }
        OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.import_model_file))
        }
        Text(
            "Use a .litertlm or .task file you already have. It is copied into the app and test-loaded once.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp),
        )
    }

    if (picked != null) {
        AlertDialog(
            onDismissRequest = { if (!importing) picked = null },
            title = { Text(stringResource(R.string.import_model)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        pickedName + if (pickedSize > 0) " · ${formatModelSize(pickedSize)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true, enabled = !importing)
                    Text(stringResource(R.string.prompt_style), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PromptStyle.entries.forEach { option ->
                            FilterChip(
                                selected = style == option,
                                onClick = { if (!importing) style = option },
                                label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                    OutlinedTextField(
                        contextText,
                        { contextText = it.filter(Char::isDigit).take(6) },
                        label = { Text(stringResource(R.string.context_size_tokens)) },
                        singleLine = true,
                        enabled = !importing,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.understands_images), Modifier.weight(1f))
                        Switch(vision, { vision = it }, enabled = !importing)
                    }
                    if (importing) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text(if (progress >= 1f) "Checking that it loads…" else "Copying…", style = MaterialTheme.typography.bodySmall)
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !importing && CustomModelSpec.formatOf(pickedName) != null,
                    onClick = {
                        val uri = picked ?: return@TextButton
                        importing = true
                        error = null
                        progress = 0f
                        scope.launch {
                            val result = importer.import(
                                CustomModelRequest(uri, name, style, vision, contextText.toIntOrNull()?.coerceIn(512, 131072) ?: 4096),
                            ) { copied, total -> if (total > 0) progress = (copied.toFloat() / total).coerceIn(0f, 1f) }
                            importing = false
                            result.onSuccess { picked = null }.onFailure { error = it.message }
                        }
                    },
                ) { Text(stringResource(R.string.action_import)) }
            },
            dismissButton = { TextButton(enabled = !importing, onClick = { picked = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    deleting?.let { spec ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${spec.displayName}?") },
            text = { Text(stringResource(R.string.the_imported_copy_is_removed)) },
            confirmButton = { TextButton(onClick = { importer.delete(spec); deleting = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
