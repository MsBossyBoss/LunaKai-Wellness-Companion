package com.fancie.aicompanion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AdultRoleplayConfig(
    val enabled: Boolean,
    val endpointUrl: String,
    val modelName: String,
)
class AdultRoleplayRepository(
    private val ollamaRepository: OllamaCompanionRepository = OllamaCompanionRepository(),
) {
    suspend fun sendMessage(
        companion: CompanionContext,
        userMessage: String,
        history: List<CompanionChatTurn>,
    ): CompanionBrainState = withContext(Dispatchers.IO) {
        val localCompanion = companion.copy(
            adultProviderEnabled = true,
            adultProviderEndpoint = LunaKaiLocalConfig.ollamaGenerateEndpoint(companion.serverHost),
            adultProviderModel = LunaKaiLocalConfig.OLLAMA_MODEL,
        )
        ollamaRepository.sendMessage(localCompanion, userMessage, history)
    }

    companion object {
        const val DEFAULT_ADULT_MODEL = LunaKaiLocalConfig.OLLAMA_MODEL
        val RECOMMENDED_MODELS = listOf(LunaKaiLocalConfig.OLLAMA_MODEL)
    }
}
