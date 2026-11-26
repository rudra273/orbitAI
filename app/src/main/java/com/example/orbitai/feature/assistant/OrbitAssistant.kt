package com.example.orbitai.feature.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.example.orbitai.MainActivity
import com.example.orbitai.feature.bubble.OrbitBubbleService

/**
 * Lets Orbit be chosen under Settings > Default apps > Digital assistant app, so the phone's
 * assistant gesture (for example holding the power button) opens Orbit (f048). It only hands off:
 * the Orbit Bubble listens when it is set up, otherwise a new chat opens.
 */
class OrbitInteractionService : VoiceInteractionService()

class OrbitSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = OrbitSession(this)
}

private class OrbitSession(private val context: Context) : VoiceInteractionSession(context) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        if (OrbitBubbleService.isUsable(context)) {
            OrbitBubbleService.trigger(context)
        } else {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .setAction(MainActivity.ACTION_NEW_CHAT)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }
        hide()
    }
}
