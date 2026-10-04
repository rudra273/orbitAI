package com.example.orbitai.feature.voice

import android.content.Context
import androidx.core.content.edit

/** Read-aloud and voice mode preferences. */
class VoiceSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("orbitai_voice", Context.MODE_PRIVATE)

    /** Speak each new reply automatically. */
    var autoRead: Boolean
        get() = prefs.getBoolean("auto_read", false)
        set(value) = prefs.edit { putBoolean("auto_read", value) }

    /** 1.0 is normal speed. */
    var speechRate: Float
        get() = prefs.getFloat("speech_rate", 1.0f)
        set(value) = prefs.edit { putFloat("speech_rate", value) }

    /** TextToSpeech voice name, or null for the system default. */
    var voiceName: String?
        get() = prefs.getString("voice_name", null)
        set(value) = prefs.edit { putString("voice_name", value) }

    var voiceModeIntroShown: Boolean
        get() = prefs.getBoolean("voice_mode_intro_shown", false)
        set(value) = prefs.edit { putBoolean("voice_mode_intro_shown", value) }
}
