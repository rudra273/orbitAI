package com.example.orbitai.viewmodel

import com.example.orbitai.appContainer
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.core.model.AVAILABLE_MODELS
import com.example.orbitai.core.model.AVAILABLE_EMBEDDING_MODELS
import com.example.orbitai.core.model.EMBEDDING_DOWNLOAD_URLS
import com.example.orbitai.core.model.EMBEDDING_TOKENIZER_URLS
import com.example.orbitai.core.model.EmbeddingModelConfig
import com.example.orbitai.core.model.DownloadProgress
import com.example.orbitai.core.model.DownloadStatus
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.ModelDownloader
import com.example.orbitai.core.model.ModelDownloadWorker
import com.example.orbitai.core.model.MODEL_DOWNLOAD_SPECS
import com.example.orbitai.core.model.formatModelSize
import com.example.orbitai.core.model.DeviceFit
import com.example.orbitai.core.model.DeviceSpecs
import com.example.orbitai.core.model.ModelFit
import com.example.orbitai.core.common.DownloadSettingsStore
import com.example.orbitai.core.engine.BackendCascade
import com.example.orbitai.core.engine.BackendPreference
import com.example.orbitai.core.engine.BackendSettingsStore
import com.example.orbitai.core.engine.BackendSpeedTest
import com.example.orbitai.core.engine.HardwareBackend
import com.example.orbitai.core.engine.SpeedTestResults
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    val downloader = application.appContainer.modelDownloader
    val downloadSettings = DownloadSettingsStore(application)
    val deviceSpecs = DeviceSpecs(application)

    fun fitOf(model: LlmModel): ModelFit = DeviceFit.fitOf(model, deviceSpecs.totalRamBytes)

    val recommendedModelId: String? by lazy { DeviceFit.recommendedModelId(deviceSpecs.totalRamBytes) }

    /** Latest WorkManager state per catalog model (null = no scheduled work). */
    private val workState = java.util.concurrent.ConcurrentHashMap<String, DownloadProgress>()

    // modelId -> progress
    private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(
        AVAILABLE_MODELS.associate { m -> m.id to resolveStatus(m, null) }
    )
    val progress: StateFlow<Map<String, DownloadProgress>> = _progress.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()

    init {
        AVAILABLE_MODELS.forEach { model ->
            viewModelScope.launch {
                ModelDownloadWorker.observe(application, model).collect { work ->
                    if (work == null) workState.remove(model.id) else workState[model.id] = work
                    _progress.update { it + (model.id to resolveStatus(model, work)) }
                }
            }
        }
    }

    /** Combines background-work state with what is on disk (installed files or a resumable partial). */
    private fun resolveStatus(model: LlmModel, work: DownloadProgress?): DownloadProgress {
        val active = work?.status == DownloadStatus.DOWNLOADING ||
            work?.status == DownloadStatus.VERIFYING ||
            work?.status == DownloadStatus.PAUSED
        return when {
            active -> work!!
            downloader.isDownloaded(model) -> DownloadProgress(model.id, status = DownloadStatus.COMPLETED)
            work?.status == DownloadStatus.FAILED -> work
            else -> {
                val partial = downloader.partialBytes(model)
                val total = MODEL_DOWNLOAD_SPECS[model.id]?.totalSizeBytes ?: 0L
                if (partial > 0L) {
                    DownloadProgress(
                        modelId = model.id,
                        bytesDownloaded = partial,
                        totalBytes = total,
                        status = DownloadStatus.PAUSED,
                        error = "Paused · ${formatModelSize(partial)}${if (total > 0) " of ${formatModelSize(total)}" else ""}",
                        canResume = true,
                    )
                } else {
                    DownloadProgress(model.id, status = DownloadStatus.IDLE)
                }
            }
        }
    }

    fun startDownload(model: LlmModel, customUrl: String? = null) {
        val customDownloadUrl = customUrl?.trim()?.takeIf { it.isNotEmpty() }
        if (customDownloadUrl != null) {
            jobs[model.id]?.cancel()
            jobs[model.id] = viewModelScope.launch {
                downloader.download(model.id, customDownloadUrl, model.fileName, requiresAuth = false).collect { p ->
                    _progress.update { it + (model.id to p) }
                }
            }
            return
        }

        if (!MODEL_DOWNLOAD_SPECS.containsKey(model.id)) {
                _progress.update {
                    it + (model.id to DownloadProgress(
                        modelId = model.id,
                        status = DownloadStatus.FAILED,
                        error = "Download spec not configured for ${model.displayName}",
                    ))
                }
                return
        }

        val size = MODEL_DOWNLOAD_SPECS[model.id]?.totalSizeBytes ?: 0L
        val remaining = size - downloader.partialBytes(model)
        val free = deviceSpecs.freeModelStorageBytes()
        if (remaining > 0 && !DeviceFit.hasStorageFor(remaining, free)) {
            _progress.update {
                it + (model.id to DownloadProgress(
                    modelId = model.id,
                    status = DownloadStatus.FAILED,
                    error = "Not enough storage: needs ${formatModelSize(remaining)}, ${formatModelSize(free)} free.",
                ))
            }
            return
        }

        ModelDownloadWorker.enqueue(getApplication(), model)
    }

    /** Stops the background download and keeps the partial file so it can resume. */
    fun pauseDownload(model: LlmModel) {
        ModelDownloadWorker.cancel(getApplication(), model.id)
    }

    /** Stops the download and discards any partial data. */
    fun cancelDownload(model: LlmModel) {
        jobs[model.id]?.cancel()
        downloader.cancelDownload(model.id)
        ModelDownloadWorker.cancel(getApplication(), model.id)
        if (!downloader.isDownloaded(model)) downloader.deleteModel(model)
        _progress.update {
            it + (model.id to DownloadProgress(model.id, status = DownloadStatus.IDLE))
        }
    }

    fun deleteModel(model: LlmModel) {
        ModelDownloadWorker.cancel(getApplication(), model.id)
        downloader.deleteModel(model)
        _progress.update {
            it + (model.id to DownloadProgress(model.id, status = DownloadStatus.IDLE))
        }
    }

    fun setLargeModelsOnWifiOnly(enabled: Boolean) {
        downloadSettings.largeModelsOnWifiOnly = enabled
    }

    fun refreshStatus() {
        _progress.update { current ->
            current.mapValues { (id, p) ->
                val model = AVAILABLE_MODELS.find { it.id == id } ?: return@mapValues p
                if (jobs[id]?.isActive == true) p else resolveStatus(model, workState[id])
            }
        }
        _embeddingProgress.update { current ->
            current.mapValues { (id, p) ->
                val model = AVAILABLE_EMBEDDING_MODELS.find { it.id == id } ?: return@mapValues p
                if (downloader.isEmbeddingDownloaded(model)) p.copy(status = DownloadStatus.COMPLETED)
                else if (p.status == DownloadStatus.COMPLETED) DownloadProgress(id, status = DownloadStatus.IDLE)
                else p
            }
        }
    }

    // ── Embedding model (Gecko) ───────────────────────────────────────────────

    private val _embeddingProgress = MutableStateFlow<Map<String, DownloadProgress>>(
        AVAILABLE_EMBEDDING_MODELS.associate { m ->
            m.id to DownloadProgress(
                modelId = m.id,
                status = if (downloader.isEmbeddingDownloaded(m)) DownloadStatus.COMPLETED else DownloadStatus.IDLE
            )
        }
    )
    val embeddingProgress: StateFlow<Map<String, DownloadProgress>> = _embeddingProgress.asStateFlow()

    private val embedderSettings = com.example.orbitai.feature.spaces.embedding.EmbedderSettings(getApplication<Application>())

    val activeEmbeddingModelId: String get() = embedderSettings.activeModelId

    /** Chunks still holding vectors from another model than [modelId]; 0 when search is up to date. */
    fun reindexRemaining(modelId: String) = com.example.orbitai.core.database.AppDatabase.getInstance(getApplication<Application>())
        .ragDocumentDao().observeChunksToEmbed(modelId)

    /** Switches search and Memory to [model] (if downloaded) and re-indexes in the background. */
    fun useEmbeddingModel(model: EmbeddingModelConfig) {
        if (!downloader.isEmbeddingDownloaded(model) || embedderSettings.activeModelId == model.id) return
        embedderSettings.activeModelId = model.id
        com.example.orbitai.feature.spaces.ReembedWorker.enqueue(getApplication<Application>())
    }

    fun startEmbeddingDownload(model: EmbeddingModelConfig) {
        val url = EMBEDDING_DOWNLOAD_URLS[model.id] ?: return
        jobs[model.id]?.cancel()
        jobs[model.id] = viewModelScope.launch {
            // 1. Download tokenizer first (small file)
            val tokenizerUrl = EMBEDDING_TOKENIZER_URLS[model.id]
            if (tokenizerUrl != null && model.tokenizerFileName != null) {
                var failed = false
                downloader.download("${model.id}-tok", tokenizerUrl, model.tokenizerFileName, requiresAuth = false)
                    .collect { p ->
                        if (p.status == DownloadStatus.FAILED) {
                            _embeddingProgress.update { it + (model.id to p.copy(modelId = model.id)) }
                            failed = true
                        }
                    }
                if (failed) return@launch
            }
            // 2. Download main model (shows progress to UI)
            downloader.download(model.id, url, model.fileName, requiresAuth = false).collect { p ->
                _embeddingProgress.update { it + (model.id to p) }
            }
        }
    }

    fun cancelEmbeddingDownload(model: EmbeddingModelConfig) {
        jobs[model.id]?.cancel()
        downloader.cancelDownload(model.id)
        downloader.cancelDownload("${model.id}-tok")
        _embeddingProgress.update {
            it + (model.id to DownloadProgress(model.id, status = DownloadStatus.IDLE))
        }
    }

    fun deleteEmbeddingModel(model: EmbeddingModelConfig) {
        downloader.deleteEmbeddingModel(model)
        if (embedderSettings.activeModelId == model.id) {
            embedderSettings.activeModelId = com.example.orbitai.core.model.GECKO_EMBEDDING.id
            com.example.orbitai.feature.spaces.ReembedWorker.enqueue(getApplication<Application>())
        }
        _embeddingProgress.update {
            it + (model.id to DownloadProgress(model.id, status = DownloadStatus.IDLE))
        }
    }

    // ── Run on (GPU / CPU / NPU) and speed test ──────────────────────────────

    data class SpeedTestState(
        val modelId: String,
        val started: Boolean = false,
        val running: HardwareBackend? = null,
        val results: SpeedTestResults? = null,
        val error: String? = null,
    )

    private val backendSettings = BackendSettingsStore(application)
    private val speedTest = BackendSpeedTest(application, application.appContainer.llmRepository)

    private val _speedTestState = MutableStateFlow<SpeedTestState?>(null)
    val speedTestState: StateFlow<SpeedTestState?> = _speedTestState.asStateFlow()

    /** Bumped when a choice or test result changes, so rows re-read them. */
    private val _backendVersion = MutableStateFlow(0)
    val backendVersion: StateFlow<Int> = _backendVersion.asStateFlow()

    fun supportedBackends(model: LlmModel): List<HardwareBackend> = com.example.orbitai.core.engine.supportedBackends(model)

    fun backendPreference(model: LlmModel): BackendPreference = backendSettings.preference(model.id)

    fun speedResults(model: LlmModel): SpeedTestResults? = backendSettings.results(model.id)

    fun setBackendPreference(model: LlmModel, preference: BackendPreference) {
        backendSettings.setPreference(model.id, preference)
        // Picking a backend gives it a fresh try even if it failed before.
        preference.backend?.let { BackendCascade(getApplication(), java.io.File(model.fileName).name).forget(it) }
        _backendVersion.update { it + 1 }
    }

    fun openSpeedTest(model: LlmModel) {
        if (_speedTestState.value?.let { it.started && it.results == null && it.error == null } == true) return
        _speedTestState.value = SpeedTestState(model.id)
    }

    fun startSpeedTest(model: LlmModel) {
        _speedTestState.value = SpeedTestState(model.id, started = true)
        viewModelScope.launch {
            _speedTestState.value = try {
                val results = speedTest.run(model) { backend -> _speedTestState.update { it?.copy(running = backend) } }
                SpeedTestState(model.id, started = true, results = results)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                SpeedTestState(model.id, started = true, error = e.message ?: e.javaClass.simpleName)
            }
            _backendVersion.update { it + 1 }
        }
    }

    /** Closes the dialog unless a test is still running. */
    fun dismissSpeedTest() {
        val state = _speedTestState.value ?: return
        if (state.started && state.results == null && state.error == null) return
        _speedTestState.value = null
    }
}
