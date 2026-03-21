package com.example.orbitai.data

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
    val downloadedLocalModels = AVAILABLE_MODELS.filter(modelDownloader::isDownloaded)
    return cloudModels + downloadedLocalModels
}
