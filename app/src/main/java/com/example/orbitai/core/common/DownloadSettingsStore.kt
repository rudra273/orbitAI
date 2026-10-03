package com.example.orbitai.core.common

import android.content.Context

class DownloadSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("orbitai_download_prefs", Context.MODE_PRIVATE)

    /** Models larger than [LARGE_MODEL_BYTES] wait for an unmetered network when enabled. */
    var largeModelsOnWifiOnly: Boolean
        get() = prefs.getBoolean("large_models_wifi_only", true)
        set(value) {
            prefs.edit().putBoolean("large_models_wifi_only", value).apply()
        }

    companion object {
        const val LARGE_MODEL_BYTES = 1_000_000_000L
    }
}
