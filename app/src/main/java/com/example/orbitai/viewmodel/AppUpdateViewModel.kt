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
    private var downloadedApkFile: java.io.File? = null

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
                    if (!info.isUpdateAvailable) {
                        repository.cleanupOldUpdates()
                    }
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

    fun downloadAndInstallUpdate() {
        val current = _uiState.value
        val url = current.downloadUrl
        if (current.isDownloading || url.isNullOrBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloading = true,
                    downloadProgress = 0,
                    errorMessage = null,
                    installMessage = null,
                )
            }

            repository.downloadApk(
                downloadUrl = url,
                versionName = current.latestVersion.orEmpty(),
                onProgress = { progress ->
                    _uiState.update { state ->
                        state.copy(isDownloading = true, downloadProgress = progress)
                    }
                },
            ).onSuccess { apkFile ->
                downloadedApkFile = apkFile
                repository.launchInstaller(apkFile)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isDownloading = false,
                                downloadProgress = 100,
                                isReadyToInstall = true,
                                isUpdateAvailable = false,
                                installMessage = "Download completed. Confirm install in Android prompt."
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isDownloading = false,
                                isReadyToInstall = true,
                                isUpdateAvailable = false,
                                errorMessage = error.message ?: "Unable to open installer",
                            )
                        }
                    }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        errorMessage = error.message ?: "Unable to download update",
                    )
                }
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
        // Don't re-check if we already downloaded the APK and are waiting
        // for the user to finish installing.  Re-fetching the release
        // would wipe the "ready to install" state and show "Update" again.
        if (downloadedApkFile?.exists() == true) return
        checkForUpdates()
    }

    /** Re-launch the installer for the already-downloaded APK. */
    fun installDownloadedUpdate() {
        val apk = downloadedApkFile
        if (apk == null || !apk.exists()) {
            // File was removed — reset and let the user re-download.
            downloadedApkFile = null
            _uiState.update {
                it.copy(
                    isReadyToInstall = false,
                    isUpdateAvailable = true,
                    installMessage = null,
                )
            }
            return
        }
        repository.launchInstaller(apk)
            .onSuccess {
                _uiState.update {
                    it.copy(installMessage = "Opening installer…")
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Unable to open installer",
                        installMessage = null,
                    )
                }
            }
    }
}
