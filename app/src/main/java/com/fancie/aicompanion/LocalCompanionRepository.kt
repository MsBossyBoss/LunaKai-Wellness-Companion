package com.fancie.aicompanion

data class CompanionContext(
    val companionId: String,
    val companionName: String,
    val gender: String,
    val voice: String,
    val characterMode: String,
    val personalityTraits: List<String>,
    val communicationStyle: String,
    val supportFocus: List<String>,
    val shortDescription: String,
    val roleplayStyles: List<String> = emptyList(),
    val bdsmEnabled: Boolean = false,
    val bdsmAdultConsentConfirmed: Boolean = false,
    val bdsmStopWord: String = "Red",
    val bdsmPauseWord: String = "Yellow",
    val anatomicalLanguageAllowed: Boolean = false,
    val adultPhrasePreferences: String = "",
    val adultProviderEnabled: Boolean = true,
    val adultProviderEndpoint: String = LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT,
    val adultProviderModel: String = LunaKaiLocalConfig.OLLAMA_MODEL,
    val adminEmoIntelProfile: String = "",
)

sealed interface CompanionBrainState {
    data object Loading : CompanionBrainState
    data class Success(val text: String) : CompanionBrainState
    data class Error(val message: String, val cause: Throwable? = null) : CompanionBrainState
}

data class CompanionChatTurn(
    val text: String,
    val isUser: Boolean,
)