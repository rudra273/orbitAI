package com.example.orbitai.core.model

import com.example.orbitai.core.common.TokenStore

const val GEMINI_CHAT_MODEL_ID = "gemini-api"

fun TokenStore.geminiChatModelOrNull(): LlmModel? {
    if (!hasGeminiConfig()) return null
    return LlmModel(
        id = GEMINI_CHAT_MODEL_ID,
        displayName = geminiModelName,
        fileName = "",
        description = "Cloud model via API key",
        paramCount = "API",
        provider = ModelProvider.GEMINI,
    )
}

fun availableChatModels(
    modelDownloader: ModelDownloader,
    tokenStore: TokenStore,
): List<LlmModel> {
    val cloudModels = listOfNotNull(tokenStore.geminiChatModelOrNull())
    val downloadedLocalModels = AVAILABLE_MODELS.filter { model ->
        model.isChatSelectable && modelDownloader.isDownloaded(model)
    }
    return cloudModels + downloadedLocalModels
}
