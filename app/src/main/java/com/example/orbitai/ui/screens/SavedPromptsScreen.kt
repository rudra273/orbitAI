package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.orbitai.appContainer
import com.example.orbitai.feature.prompts.CommandPalette
import com.example.orbitai.feature.prompts.SavedPrompt

/** Create, edit, reorder and delete prompts shown in the chat "/" menu. */
@Composable
fun SavedPromptsScreen(onBack: () -> Unit) {
    val store = LocalContext.current.appContainer.savedPrompts
    val prompts by store.prompts.collectAsState()
    var editing by remember { mutableStateOf<SavedPrompt?>(null) }
    BackHandler(onBack = onBack)

    editing?.let { prompt ->
        SavedPromptDialog(
            initial = prompt,
            onDismiss = { editing = null },
            onSave = { store.save(it); editing = null },
        )
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(stringResource(R.string.saved_prompts), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Type / in a chat to use them. Put ${CommandPalette.PLACEHOLDER} where you want to type, e.g. \"Translate ${CommandPalette.PLACEHOLDER} to Hindi\".",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                OutlinedButton(onClick = { editing = SavedPrompt(title = "", text = "") }) { Text(stringResource(R.string.new_prompt)) }
            }
            itemsIndexed(prompts, key = { _, p -> p.id }) { index, prompt ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(prompt.title, style = MaterialTheme.typography.titleSmall)
                            Text(prompt.text, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { store.move(prompt.id, -1) }, enabled = index > 0) {
                            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                        }
                        IconButton(onClick = { store.move(prompt.id, 1) }, enabled = index < prompts.lastIndex) {
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                        }
                        IconButton(onClick = { editing = prompt }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit ${prompt.title}") }
                        IconButton(onClick = { store.delete(prompt.id) }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete ${prompt.title}") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPromptDialog(initial: SavedPrompt, onDismiss: () -> Unit, onSave: (SavedPrompt) -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var text by remember { mutableStateOf(initial.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.title.isBlank()) "New prompt" else "Edit prompt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true)
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.prompt)) }, maxLines = 6)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(initial.copy(title = title.trim(), text = text.trim())) },
                enabled = title.isNotBlank() && text.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
