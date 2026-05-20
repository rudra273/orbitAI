package com.example.orbitai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.feature.update.AppUpdateInfo
import com.example.orbitai.feature.update.AppUpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUpdateUiState(
    val installedVersion: String = "",
    val latestVersion: String? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val isUpdateAvailable: Boolean = false,
    val isReadyToInstall: Boolean = false,
    val downloadUrl: String? = null,
    val releaseUrl: String? = null,
    val errorMessage: String? = null,
    val installMessage: String? = null,
)

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppUpdateRepository(application)

    private val _uiState = MutableStateFlow(
        AppUpdateUiState(installedVersion = repository.installedVersion())
    )
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    installedVersion = repository.installedVersion(),
                    isChecking = true,
                    errorMessage = null,
                )
            }

            repository.fetchLatestRelease()
                .onSuccess { info ->
                    _uiState.value = info.toUiState()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            installedVersion = repository.installedVersion(),
                            isChecking = false,
                            isDownloading = false,
                            errorMessage = error.message ?: "Unable to check for updates",
                        )
                    }
                }
        }
    }

    fun openUpdatePage() {
        val current = _uiState.value
        val url = current.releaseUrl
        if (url.isNullOrBlank()) return

        repository.openReleasePage(url)
            .onSuccess {
                _uiState.update {
                    it.copy(errorMessage = null, installMessage = "Opened release page")
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Unable to open update page")
                }
            }
    }

    private fun AppUpdateInfo.toUiState(): AppUpdateUiState {
        return AppUpdateUiState(
            installedVersion = installedVersion,
            latestVersion = latestVersion,
            isChecking = false,
            isDownloading = false,
            downloadProgress = 0,
            isUpdateAvailable = isUpdateAvailable,
            isReadyToInstall = false,
            downloadUrl = downloadUrl,
            releaseUrl = releaseUrl,
            errorMessage = null,
            installMessage = null,
        )
    }

    fun refreshAfterResume() {
        checkForUpdates()
    }
}
