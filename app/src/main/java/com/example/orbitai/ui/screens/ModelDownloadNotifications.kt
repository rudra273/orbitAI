package com.example.orbitai.ui.screens

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.orbitai.R
import com.example.orbitai.core.model.AVAILABLE_MODELS
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.ModelDownloadWorker
import com.example.orbitai.viewmodel.DownloadViewModel

/** Requests permission at the point where the user starts a background model download. */
@Composable
internal fun rememberModelDownloadAction(downloadViewModel: DownloadViewModel): (LlmModel) -> Unit {
    val context = LocalContext.current
    val notifications = context.getSystemService(NotificationManager::class.java)
    var pendingModelId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNotificationSettings by rememberSaveable { mutableStateOf(false) }

    fun isDownloadChannelBlocked(): Boolean =
        notifications.getNotificationChannel(ModelDownloadWorker.CHANNEL_ID)?.importance ==
            NotificationManager.IMPORTANCE_NONE

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val model = AVAILABLE_MODELS.find { it.id == pendingModelId }
        pendingModelId = null
        if (model != null) {
            downloadViewModel.startDownload(model)
            showNotificationSettings = !granted || !notifications.areNotificationsEnabled() || isDownloadChannelBlocked()
        }
    }

    if (showNotificationSettings) {
        AlertDialog(
            onDismissRequest = { showNotificationSettings = false },
            title = { Text(stringResource(R.string.download_notifications_unavailable)) },
            text = { Text(stringResource(R.string.download_notifications_settings_explanation)) },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationSettings = false
                    val settings = if (notifications.areNotificationsEnabled() && isDownloadChannelBlocked()) {
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, ModelDownloadWorker.CHANNEL_ID)
                    } else {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    }
                    context.startActivity(settings.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text(stringResource(R.string.download_notifications_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationSettings = false }) {
                    Text(stringResource(R.string.download_notifications_not_now))
                }
            },
        )
    }

    return { model ->
        if (pendingModelId == null) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                downloadViewModel.startDownload(model)
                showNotificationSettings = !notifications.areNotificationsEnabled() || isDownloadChannelBlocked()
            } else {
                pendingModelId = model.id
                permissionLauncher.launch(permission)
            }
        }
    }
}
