package com.example.orbitai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.R
import com.example.orbitai.core.engine.BackendPreference
import com.example.orbitai.core.engine.BackendRun
import com.example.orbitai.core.engine.HardwareBackend
import com.example.orbitai.core.engine.RunOutcome
import com.example.orbitai.core.engine.SpeedTestResults
import com.example.orbitai.core.model.AVAILABLE_MODELS
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.ui.theme.OrbitMonoFont
import com.example.orbitai.ui.theme.OrbitTheme
import com.example.orbitai.ui.theme.OrbitUiFont
import com.example.orbitai.viewmodel.DownloadViewModel
import java.util.Locale

/** "Run on: Auto · GPU fastest · Test speed" line under an installed model. */
@Composable
internal fun ModelBackendControls(model: LlmModel, viewModel: DownloadViewModel) {
    val colors = OrbitTheme.colors
    viewModel.backendVersion.collectAsState().value // re-read choice and results when they change
    val supported = viewModel.supportedBackends(model)
    if (supported.isEmpty()) return
    if (supported.size == 1) {
        Text(stringResource(R.string.runs_on_cpu_only), color = colors.labelSecondary, fontFamily = OrbitMonoFont, fontSize = 11.sp)
        return
    }
    val preference = viewModel.backendPreference(model)
    val results = viewModel.speedResults(model)
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${stringResource(R.string.run_on)}: ${preferenceLabel(preference)} ▾",
            color = colors.accent,
            fontFamily = OrbitMonoFont,
            fontSize = 11.sp,
            modifier = Modifier
                .heightIn(min = 32.dp)
                .clickable(role = Role.Button) { showPicker = true }
                .padding(vertical = 8.dp),
        )
        Text("·", color = colors.labelTertiary, fontFamily = OrbitMonoFont, fontSize = 11.sp)
        Text(
            text = results?.winner?.let { stringResource(R.string.backend_fastest, it.label) } ?: stringResource(R.string.backend_not_tested),
            color = colors.labelSecondary,
            fontFamily = OrbitMonoFont,
            fontSize = 11.sp,
        )
        Text("·", color = colors.labelTertiary, fontFamily = OrbitMonoFont, fontSize = 11.sp)
        Text(
            text = stringResource(if (results == null) R.string.test_speed else R.string.test_again),
            color = colors.accent,
            fontFamily = OrbitMonoFont,
            fontSize = 11.sp,
            modifier = Modifier
                .heightIn(min = 32.dp)
                .clickable(role = Role.Button) { viewModel.openSpeedTest(model) }
                .padding(vertical = 8.dp),
        )
    }

    if (showPicker) {
        BackendPickerDialog(
            supported = supported,
            selected = preference,
            results = results,
            onSelect = { viewModel.setBackendPreference(model, it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun preferenceLabel(preference: BackendPreference): String =
    preference.backend?.label ?: stringResource(R.string.backend_auto)

@Composable
private fun BackendPickerDialog(
    supported: List<HardwareBackend>,
    selected: BackendPreference,
    results: SpeedTestResults?,
    onSelect: (BackendPreference) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OrbitTheme.colors
    val options = listOf(BackendPreference.AUTO) + supported.sortedBy { it.ordinal }.map { BackendPreference.of(it) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.run_on), fontFamily = OrbitUiFont, fontSize = 18.sp, color = colors.labelPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    val backend = option.backend
                    val title = if (backend == null) stringResource(R.string.backend_auto_recommended) else backend.label
                    val description = when {
                        backend == null -> results?.winner?.let { stringResource(R.string.backend_auto_uses, it.label) + " " }.orEmpty() +
                            stringResource(R.string.backend_auto_desc)
                        else -> stringResource(R.string.backend_strict_desc, backend.label)
                    }
                    val run = backend?.let { b -> results?.runs?.firstOrNull { it.backend == b } }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, role = Role.RadioButton) { onSelect(option) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        RadioButton(selected = option == selected, onClick = null, modifier = Modifier.padding(end = 8.dp))
                        Column {
                            Text(title, color = colors.labelPrimary, fontFamily = OrbitUiFont, fontSize = 14.sp)
                            Text(description, color = colors.labelSecondary, fontFamily = OrbitUiFont, fontSize = 12.sp)
                            if (run != null) RunSummary(run)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), fontFamily = OrbitUiFont, color = colors.labelPrimary) }
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(14.dp),
    )
}

/** Speed-test dialog for the model the user tapped: intro, progress, then results. */
@Composable
internal fun SpeedTestDialog(viewModel: DownloadViewModel) {
    val state by viewModel.speedTestState.collectAsState()
    val current = state ?: return
    val model = AVAILABLE_MODELS.firstOrNull { it.id == current.modelId } ?: return
    val colors = OrbitTheme.colors
    val running = current.started && current.results == null && current.error == null

    AlertDialog(
        onDismissRequest = { viewModel.dismissSpeedTest() },
        title = { Text(stringResource(R.string.speed_test_title), fontFamily = OrbitUiFont, fontSize = 18.sp, color = colors.labelPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    !current.started -> Text(
                        stringResource(R.string.speed_test_intro, model.displayName),
                        color = colors.labelSecondary, fontFamily = OrbitUiFont, fontSize = 14.sp,
                    )
                    running -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            current.running?.let { stringResource(R.string.speed_test_running, it.label) }
                                ?: stringResource(R.string.speed_test_preparing),
                            color = colors.labelPrimary, fontFamily = OrbitUiFont, fontSize = 14.sp,
                        )
                    }
                    current.error != null -> Text(
                        stringResource(R.string.speed_test_failed, current.error),
                        color = colors.destructive, fontFamily = OrbitUiFont, fontSize = 14.sp,
                    )
                    else -> SpeedTestResultList(current.results!!)
                }
            }
        },
        confirmButton = {
            when {
                !current.started -> TextButton(onClick = { viewModel.startSpeedTest(model) }) {
                    Text(stringResource(R.string.speed_test_start), fontFamily = OrbitUiFont, color = colors.accent)
                }
                !running -> TextButton(onClick = { viewModel.dismissSpeedTest() }) {
                    Text(stringResource(R.string.close), fontFamily = OrbitUiFont, color = colors.labelPrimary)
                }
            }
        },
        dismissButton = when {
            !current.started -> {
                { TextButton(onClick = { viewModel.dismissSpeedTest() }) { Text(stringResource(R.string.cancel), fontFamily = OrbitUiFont, color = colors.labelPrimary) } }
            }
            running -> {
                { TextButton(onClick = { viewModel.cancelSpeedTest() }) { Text(stringResource(R.string.cancel), fontFamily = OrbitUiFont, color = colors.destructive) } }
            }
            else -> null
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun SpeedTestResultList(results: SpeedTestResults) {
    val colors = OrbitTheme.colors
    val winner = results.winner
    if (winner == null) {
        Text(stringResource(R.string.speed_test_none_worked), color = colors.destructive, fontFamily = OrbitUiFont, fontSize = 14.sp)
    }
    results.runs.sortedBy { it.backend.ordinal }.forEach { run ->
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(run.backend.label, color = colors.labelPrimary, fontFamily = OrbitUiFont, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                if (run.backend == winner) {
                    Text("★ ${stringResource(R.string.recommended)}", color = colors.success, fontFamily = OrbitMonoFont, fontSize = 11.sp)
                }
            }
            RunSummary(run)
            if (results.noGainOverCpu(run.backend)) {
                Text(stringResource(R.string.speed_test_no_gain), color = colors.warning, fontFamily = OrbitUiFont, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RunSummary(run: BackendRun) {
    val colors = OrbitTheme.colors
    val text = when (run.outcome) {
        RunOutcome.OK -> stringResource(
            R.string.speed_test_line,
            oneDecimal(run.loadMs / 1000.0),
            oneDecimal(run.firstTokenMs / 1000.0),
            oneDecimal(run.tokensPerSecond),
        )
        RunOutcome.FAILED -> stringResource(R.string.outcome_failed)
        RunOutcome.BROKEN_OUTPUT -> stringResource(R.string.outcome_broken)
        RunOutcome.TIMED_OUT -> stringResource(R.string.outcome_timed_out)
        RunOutcome.CRASHED_BEFORE -> stringResource(R.string.outcome_crashed)
    }
    Text(
        text,
        color = if (run.ok) colors.labelSecondary else colors.destructive,
        fontFamily = OrbitMonoFont,
        fontSize = 11.sp,
    )
}

private fun oneDecimal(value: Double) = String.format(Locale.getDefault(), "%.1f", value)
