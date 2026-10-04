package com.example.orbitai.feature.bubble

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.orbitai.MainActivity
import com.example.orbitai.core.common.ThemeSettingsStore
import com.example.orbitai.feature.bubble.OrbitBubbleService.Companion.ACTION_USE_TEXT
import com.example.orbitai.feature.bubble.OrbitBubbleService.Companion.EXTRA_SHARED_TEXT
import com.example.orbitai.feature.chat.ChatSettingsStore
import com.example.orbitai.feature.textactions.TextAction
import com.example.orbitai.feature.textactions.TextActionPrompts
import com.example.orbitai.feature.textactions.TextActionRunner
import com.example.orbitai.ui.theme.OrbitAITheme
import kotlinx.coroutines.CancellationException

/**
 * Orbit's entry in the text-selection menu (and for shared text): proofread, rewrite, translate,
 * summarize, explain or reply with the on-device model. When the selected text is editable,
 * Replace puts the result back into the other app.
 */
class ProcessTextActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            else -> null
        }
        if (sharedText.isNullOrBlank()) {
            finish()
            return
        }
        // Only a PROCESS_TEXT selection in an editable field can take replacement text.
        val canReplace = intent.action == Intent.ACTION_PROCESS_TEXT &&
            !intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

        setContent {
            OrbitAITheme(appearance = ThemeSettingsStore(this).appearance) {
                TextActionSheet(
                    text = sharedText,
                    canReplace = canReplace,
                    onReplace = { result ->
                        setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, result))
                        finish()
                    },
                    onAskOrbit = { askOrbit(sharedText); finish() },
                    onReminder = { openSharedTextInApp("/remind $sharedText"); finish() },
                    onGetModel = {
                        Toast.makeText(this, "Download a model in Settings > Models to use text actions.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }

    private fun askOrbit(sharedText: String) {
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!OrbitBubbleService.canDrawOverlays(this) || !audioGranted) {
            openSharedTextInApp(sharedText)
        } else {
            try {
                val serviceIntent = Intent(this, OrbitBubbleService::class.java).apply {
                    action = ACTION_USE_TEXT
                    putExtra(EXTRA_SHARED_TEXT, sharedText)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
            } catch (_: IllegalStateException) {
                openSharedTextInApp(sharedText)
            } catch (_: SecurityException) {
                openSharedTextInApp(sharedText)
            }
        }
    }

    private fun openSharedTextInApp(text: String) {
        Toast.makeText(this, "Shared text opened in Orbit. Add your instruction before sending.", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_SHARED_TEXT, text)
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextActionSheet(
    text: String,
    canReplace: Boolean,
    onReplace: (String) -> Unit,
    onAskOrbit: () -> Unit,
    onReminder: () -> Unit,
    onGetModel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val runner = remember { TextActionRunner(context.applicationContext) }
    var language by remember { mutableStateOf(ChatSettingsStore(context).translateLanguage) }
    var pickingLanguage by remember { mutableStateOf(false) }
    val model = remember { runner.localModel() }
    var action by remember { mutableStateOf<TextAction?>(null) }
    var output by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(action) {
        val chosen = action ?: return@LaunchedEffect
        val local = model ?: return@LaunchedEffect
        output = ""
        done = false
        error = null
        try {
            runner.run(local, chosen, text, language).collect { output += it }
            output = TextActionPrompts.cleanResult(output)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "Couldn't finish: ${e.message ?: e.javaClass.simpleName}"
        }
        done = true
    }

    if (pickingLanguage) {
        var typed by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickingLanguage = false },
            title = { Text(stringResource(R.string.translate_to)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.e_g_tamil_french_japanese)) },
                )
            },
            confirmButton = {
                TextButton(enabled = typed.isNotBlank(), onClick = {
                    language = typed.trim()
                    pickingLanguage = false
                    action = TextAction.TRANSLATE
                }) { Text(stringResource(R.string.translate)) }
            },
            dismissButton = { TextButton(onClick = { pickingLanguage = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = text.replace(Regex("\\s+"), " "),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            HorizontalDivider()
            val chosen = action
            if (chosen == null) {
                if (model == null) {
                    Text(stringResource(R.string.text_actions_run_on_an), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onGetModel) { Text(stringResource(R.string.download_a_model)) }
                } else {
                    TextAction.entries.forEach { option ->
                        val label = if (option == TextAction.TRANSLATE) "Translate to $language" else option.label
                        ActionRow(label) { action = option }
                        if (option == TextAction.TRANSLATE) ActionRow("Translate to another language…") { pickingLanguage = true }
                    }
                }
                ActionRow("Ask Orbit", onAskOrbit)
                ActionRow("Create reminder", onReminder)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (chosen == TextAction.TRANSLATE) "Translate to $language" else chosen.label,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (!done) CircularProgressIndicator(Modifier.padding(4.dp), strokeWidth = 2.dp)
                }
                SelectionContainer {
                    Text(
                        text = error ?: output.ifEmpty { if (done) "No result." else "Working on it…" },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    )
                }
                if (done) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canReplace && error == null && output.isNotBlank()) Button(onClick = { onReplace(output) }) { Text(stringResource(R.string.replace)) }
                        OutlinedButton(enabled = output.isNotBlank(), onClick = {
                            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Orbit", output))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }) { Text(stringResource(R.string.copy)) }
                        OutlinedButton(enabled = output.isNotBlank(), onClick = {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, output), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }) { Text(stringResource(R.string.share)) }
                    }
                    TextButton(onClick = { action = null }) { Text(stringResource(R.string.other_actions)) }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}
