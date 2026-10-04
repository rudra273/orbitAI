package com.example.orbitai.feature.shortcuts

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.example.orbitai.MainActivity
import com.example.orbitai.feature.bubble.BubbleSettingsStore
import com.example.orbitai.feature.bubble.OrbitBubbleService

/**
 * Quick Settings tile: tap to talk to Orbit (through the bubble when it's on, otherwise voice
 * mode in the app). Long-press opens Orbit's settings. The tile shows as active while the bubble is on.
 */
class OrbitTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val bubbleOn = bubbleUsable()
        tile.state = if (bubbleOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (bubbleOn) "Bubble on" else "Voice"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (bubbleUsable()) {
            OrbitBubbleService.trigger(this)
            return
        }
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_VOICE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun bubbleUsable(): Boolean =
        BubbleSettingsStore(this).isFloatingBubbleEnabled &&
            OrbitBubbleService.canDrawOverlays(this) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}
