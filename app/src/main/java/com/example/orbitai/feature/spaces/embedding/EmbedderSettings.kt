package com.example.orbitai.feature.spaces.embedding

import android.content.Context
import androidx.core.content.edit
import com.example.orbitai.core.model.GECKO_EMBEDDING

/** Which embedding model search and Memory use. The small built-in one until the user picks another. */
class EmbedderSettings(context: Context) {

    private val prefs = context.getSharedPreferences("orbitai_embedder", Context.MODE_PRIVATE)

    var activeModelId: String
        get() = prefs.getString("active_model", null) ?: GECKO_EMBEDDING.id
        set(value) = prefs.edit { putString("active_model", value) }
}
