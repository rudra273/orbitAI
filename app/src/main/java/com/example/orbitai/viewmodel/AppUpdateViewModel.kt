package com.example.orbitai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orbitai.data.AppUpdateInfo
import com.example.orbitai.data.AppUpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUpdateUiState(
    val installedVersion: String = "",
    val latestVersion: String? = null,
    val isChecking: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val downloadUrl: String? = null,
    val releaseUrl: String? = null,
    val errorMessage: String? = null,
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
                .onSuccess { info -> _uiState.value = info.toUiState() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            installedVersion = repository.installedVersion(),
                            isChecking = false,
                            errorMessage = error.message ?: "Unable to check for updates",
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
            isUpdateAvailable = isUpdateAvailable,
            downloadUrl = downloadUrl,
            releaseUrl = releaseUrl,
        )
    }
}
