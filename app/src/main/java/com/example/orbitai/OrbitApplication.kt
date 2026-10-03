package com.example.orbitai

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.StrictMode
import com.example.orbitai.core.AppContainer
import kotlinx.coroutines.launch

class OrbitApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            container.diagnostics.recordCrash(thread, throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
        container.appScope.launch { container.diagnostics.recordExitReasons() }

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // The app is in the background list: free the model unless a chat or bubble run is using it.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            container.appScope.launch { container.llmRepository.releaseIfIdle() }
        }
    }
}

/** Shared app-wide dependencies. Requires the manifest to use [OrbitApplication]. */
val Context.appContainer: AppContainer
    get() = (applicationContext as OrbitApplication).container
