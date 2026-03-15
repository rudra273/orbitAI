package com.example.orbitai.data

import android.content.Context

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("orbitai_prefs", Context.MODE_PRIVATE)

    var huggingFaceToken: String
        get() = prefs.getString("hf_token", "") ?: ""
        set(value) = prefs.edit().putString("hf_token", value.trim()).apply()

    fun hasToken() = huggingFaceToken.isNotBlank()
}