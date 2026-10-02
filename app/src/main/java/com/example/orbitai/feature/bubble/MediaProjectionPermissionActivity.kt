package com.example.orbitai.feature.bubble

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class MediaProjectionPermissionActivity : Activity() {
    private var deliveredResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val manager = getSystemService(MediaProjectionManager::class.java)
            startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
        }
    }

    @Deprecated("Deprecated by platform, but sufficient for this tiny bridge activity.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            deliveredResult = true
            OrbitBubbleService.deliverMediaProjectionResult(resultCode, data)
            finish()
        }
    }

    override fun onDestroy() {
        if (!deliveredResult && !isChangingConfigurations) {
            OrbitBubbleService.deliverMediaProjectionResult(RESULT_CANCELED, null)
        }
        super.onDestroy()
    }

    private companion object {
        const val REQUEST_MEDIA_PROJECTION = 4202
    }
}
