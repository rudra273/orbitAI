package com.example.orbitai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.data.AVAILABLE_MODELS
import com.example.orbitai.data.AVAILABLE_EMBEDDING_MODELS
import com.example.orbitai.data.EMBEDDING_DOWNLOAD_URLS
import com.example.orbitai.data.EMBEDDING_TOKENIZER_URLS
import com.example.orbitai.data.EmbeddingModelConfig
import com.example.orbitai.data.DownloadProgress
import com.example.orbitai.data.DownloadStatus
import com.example.orbitai.data.LlmModel
import com.example.orbitai.data.MODEL_DOWNLOAD_URLS
import com.example.orbitai.data.ModelDownloader
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    val downloader = ModelDownloader(application)

    // modelId -> progress
    private val _progress = MutableStateFlow<Map<String, DownloadProgress>>(
        AVAILABLE_MODELS.associate { m ->
            m.id to DownloadProgress(
                modelId = m.id,
                status = if (downloader.isDownloaded(m)) DownloadStatus.COMPLETED else DownloadStatus.IDLE
            )
        }
    )
    val progress: StateFlow<Map<String, DownloadProgress>> = _progress.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()

    fun startDownload(model: LlmModel, customUrl: String? = null) {
        val url = customUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: MODEL_DOWNLOAD_URLS[model.id]
            ?: return

        jobs[model.id]?.cancel()
        jobs[model.id] = viewModelScope.launch {
            downloader.download(model.id, url, model.fileName).collect { p ->
                _progress.update { it + (model.id to p) }
            }
        }
    }

    fun cancelDownload(model: LlmModel) {
        jobs[model.id]?.cancel()
        downloader.cancelDownload(model.id)
        _progress.update {
            it + (model.id to DownloadProgress(model.id, status = DownloadStatus.IDLE))
        }
    }

    fun deleteModel(model: LlmModel) {
        downloader.deleteModel(model)
        _progress.update {
            it + (model.id to DownloadProgress(model.id, status = DownloadStatus.IDLE))
        }
    }

    fun refreshStatus() {
        _progress.update { current ->
            current.mapValues { (id, p) ->
                val model = AVAILABLE_MODELS.find { it.id == id } ?: return@mapValues p
                if (downloader.isDownloaded(model)) p.copy(status = DownloadStatus.COMPLETED)
                else if (p.status == DownloadStatus.COMPLETED) DownloadProgress(id, status = DownloadStatus.IDLE)
                else p
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
        _embeddingProgress.update {
            it + (model.id to DownloadProgress(model.id, status = DownloadStatus.IDLE))
        }
    }
}