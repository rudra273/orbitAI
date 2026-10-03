package com.example.orbitai.core.model

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.orbitai.R
import com.example.orbitai.core.common.DownloadSettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * Downloads one catalog model in the background as a foreground `dataSync` job, so downloads
 * continue when the app is closed. Interrupted transfers are retried by WorkManager and resume
 * from the partial file kept by [ModelDownloader].
 */
class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        val model = AVAILABLE_MODELS.find { it.id == modelId }
            ?: return Result.failure(workDataOf(KEY_ERROR to "Unknown model $modelId"))
        val notificationId = NOTIFICATION_ID_BASE + modelId.hashCode()

        ensureChannel()
        setForeground(foregroundInfo(model, notificationId, 0L, 0L, DownloadStatus.DOWNLOADING))

        var last: DownloadProgress? = null
        var lastReportAt = 0L
        ModelDownloader(applicationContext).download(model).collect { progress ->
            last = progress
            val now = System.currentTimeMillis()
            if (progress.status != DownloadStatus.DOWNLOADING || now - lastReportAt >= REPORT_INTERVAL_MS) {
                lastReportAt = now
                setProgress(progress.toWorkData())
                notificationManager.notify(
                    notificationId,
                    buildNotification(model, progress.bytesDownloaded, progress.totalBytes, progress.status),
                )
            }
        }

        val final = last
        return when {
            final?.status == DownloadStatus.COMPLETED -> Result.success()
            final?.canResume == true && runAttemptCount < MAX_ATTEMPTS -> Result.retry()
            else -> Result.failure(workDataOf(KEY_ERROR to (final?.error ?: "Download failed")))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val model = AVAILABLE_MODELS.find { it.id == inputData.getString(KEY_MODEL_ID) }
        ensureChannel()
        return ForegroundInfo(
            NOTIFICATION_ID_BASE + (model?.id?.hashCode() ?: 0),
            buildNotification(model, 0L, 0L, DownloadStatus.DOWNLOADING),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun foregroundInfo(model: LlmModel, id: Int, done: Long, total: Long, status: DownloadStatus) =
        ForegroundInfo(id, buildNotification(model, done, total, status), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

    private fun buildNotification(model: LlmModel?, done: Long, total: Long, status: DownloadStatus) =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Downloading ${model?.displayName ?: "model"}")
            .setContentText(
                when {
                    status == DownloadStatus.VERIFYING -> "Verifying download…"
                    total > 0 -> "${formatModelSize(done)} of ${formatModelSize(total)}"
                    else -> "Starting…"
                }
            )
            .setProgress(100, if (total > 0) ((done * 100) / total).toInt() else 0, total <= 0 || status == DownloadStatus.VERIFYING)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Pause", WorkManager.getInstance(applicationContext).createCancelPendingIntent(id))
            .build()

    private fun ensureChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Model downloads", NotificationManager.IMPORTANCE_LOW)
        )
    }

    companion object {
        private const val CHANNEL_ID = "orbitai_model_downloads"
        private const val NOTIFICATION_ID_BASE = 40_000
        private const val REPORT_INTERVAL_MS = 500L
        private const val MAX_ATTEMPTS = 5
        private const val KEY_MODEL_ID = "model_id"
        private const val KEY_BYTES = "bytes"
        private const val KEY_TOTAL = "total"
        private const val KEY_STATUS = "status"
        private const val KEY_ERROR = "error"

        fun workName(modelId: String) = "model-download-$modelId"

        fun enqueue(context: Context, model: LlmModel) {
            val wifiOnly = DownloadSettingsStore(context).largeModelsOnWifiOnly &&
                (MODEL_DOWNLOAD_SPECS[model.id]?.totalSizeBytes ?: 0L) > DownloadSettingsStore.LARGE_MODEL_BYTES
            val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setInputData(workDataOf(KEY_MODEL_ID to model.id))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(workName(model.id), ExistingWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context, modelId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(modelId))
        }

        /** Maps the model's unique work to [DownloadProgress]; null when no work has been scheduled. */
        fun observe(context: Context, model: LlmModel): Flow<DownloadProgress?> =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(workName(model.id)).map { infos ->
                infos.lastOrNull()?.toDownloadProgress(model.id)
            }

        private fun DownloadProgress.toWorkData() = workDataOf(
            KEY_BYTES to bytesDownloaded,
            KEY_TOTAL to totalBytes,
            KEY_STATUS to status.name,
        )

        private fun WorkInfo.toDownloadProgress(modelId: String): DownloadProgress = when (state) {
            WorkInfo.State.RUNNING -> DownloadProgress(
                modelId = modelId,
                bytesDownloaded = progress.getLong(KEY_BYTES, 0L),
                totalBytes = progress.getLong(KEY_TOTAL, 0L),
                status = progress.getString(KEY_STATUS)?.let { runCatching { DownloadStatus.valueOf(it) }.getOrNull() }
                    ?: DownloadStatus.DOWNLOADING,
            )
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadProgress(
                modelId = modelId,
                status = DownloadStatus.PAUSED,
                error = when {
                    runAttemptCount > 0 -> "Connection lost. Will resume automatically."
                    constraints.requiredNetworkType == NetworkType.UNMETERED -> "Waiting for Wi-Fi…"
                    else -> "Waiting for network…"
                },
            )
            WorkInfo.State.SUCCEEDED -> DownloadProgress(modelId, status = DownloadStatus.COMPLETED)
            WorkInfo.State.FAILED -> DownloadProgress(
                modelId = modelId,
                status = DownloadStatus.FAILED,
                error = outputData.getString(KEY_ERROR) ?: "Download failed",
            )
            WorkInfo.State.CANCELLED -> DownloadProgress(modelId, status = DownloadStatus.IDLE)
        }
    }
}
