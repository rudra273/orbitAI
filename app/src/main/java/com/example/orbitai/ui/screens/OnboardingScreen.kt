package com.example.orbitai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.orbitai.R
import com.example.orbitai.core.common.TokenStore
import com.example.orbitai.core.model.AVAILABLE_MODELS
import com.example.orbitai.core.model.DownloadStatus
import com.example.orbitai.core.model.MODEL_DOWNLOAD_SPECS
import com.example.orbitai.core.model.formatModelSize
import com.example.orbitai.viewmodel.DownloadViewModel

/**
 * Shown after the welcome screen (and from Settings > About): one tap to download the model that
 * suits this phone, which keeps downloading in the background, and a short tour of what Orbit does.
 */
@Composable
fun OnboardingScreen(
    downloadViewModel: DownloadViewModel,
    tokenStore: TokenStore,
    onOpenModels: () -> Unit,
    onDone: () -> Unit,
) {
    BackHandler(onBack = onDone)
    val progress by downloadViewModel.progress.collectAsState()
    val model = downloadViewModel.recommendedModelId?.let { id -> AVAILABLE_MODELS.find { it.id == id } }
    val spec = model?.let { MODEL_DOWNLOAD_SPECS[it.id] }
    val state = model?.let { progress[it.id] }
    val installed = model != null && (state?.status == DownloadStatus.COMPLETED || downloadViewModel.downloader.isDownloaded(model))
    val downloading = state?.status == DownloadStatus.DOWNLOADING || state?.status == DownloadStatus.PAUSED || state?.status == DownloadStatus.VERIFYING
    val needsToken = spec?.requiresAuth == true && !tokenStore.hasToken()

    Scaffold { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.onboarding_intro), style = MaterialTheme.typography.bodyMedium)
            }
            item { Text(stringResource(R.string.onboarding_pick_model), style = MaterialTheme.typography.titleMedium) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (model == null) {
                            Text(stringResource(R.string.onboarding_no_recommendation), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(stringResource(R.string.onboarding_recommended_for_phone), style = MaterialTheme.typography.labelSmall)
                            Text(model.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(model.description, style = MaterialTheme.typography.bodySmall)
                            spec?.totalSizeBytes?.let {
                                Text(stringResource(R.string.onboarding_download_size, formatModelSize(it)), style = MaterialTheme.typography.bodySmall)
                            }
                            when {
                                installed -> Text(stringResource(R.string.onboarding_ready), style = MaterialTheme.typography.titleSmall)
                                downloading -> {
                                    LinearProgressIndicator(progress = { state?.progress ?: 0f }, modifier = Modifier.fillMaxWidth())
                                    Text(
                                        stringResource(R.string.onboarding_downloading, state?.progressPercent ?: 0),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(stringResource(R.string.onboarding_background_note), style = MaterialTheme.typography.bodySmall)
                                }
                                needsToken -> {
                                    Text(stringResource(R.string.onboarding_needs_token), style = MaterialTheme.typography.bodySmall)
                                    Button(onClick = onOpenModels, modifier = Modifier.fillMaxWidth()) {
                                        Text(stringResource(R.string.onboarding_add_token))
                                    }
                                }
                                else -> Button(onClick = { downloadViewModel.startDownload(model) }, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.onboarding_download))
                                }
                            }
                            if (state?.status == DownloadStatus.FAILED) {
                                Text(state.error ?: stringResource(R.string.onboarding_failed), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        OutlinedButton(onClick = onOpenModels, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.onboarding_other_model))
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.onboarding_tour), style = MaterialTheme.typography.titleMedium) }
            val tour = listOf(
                R.string.tour_spaces_title to R.string.tour_spaces_body,
                R.string.tour_modes_title to R.string.tour_modes_body,
                R.string.tour_tools_title to R.string.tour_tools_body,
                R.string.tour_bubble_title to R.string.tour_bubble_body,
                R.string.tour_text_title to R.string.tour_text_body,
            )
            tour.forEach { (title, body) ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_done)) }
                TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_skip)) }
                Text(stringResource(R.string.onboarding_reopen), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
