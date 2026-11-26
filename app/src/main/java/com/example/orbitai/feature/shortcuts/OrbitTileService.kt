package com.example.orbitai.feature.shortcuts

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.orbitai.MainActivity
import com.example.orbitai.feature.bubble.OrbitBubbleService

/**
 * Quick Settings tile: tap to talk to Orbit through the bubble (voice works only there); if the
 * bubble can't start, it opens the bubble settings. Long-press opens Orbit's settings. The tile shows as active while the bubble is on.
 */
class OrbitTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val bubbleOn = OrbitBubbleService.isUsable(this)
        tile.state = if (bubbleOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (bubbleOn) "Bubble on" else "Set up bubble"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (OrbitBubbleService.isUsable(this)) {
            OrbitBubbleService.trigger(this)
            return
        }
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_VOICE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
    }
}
