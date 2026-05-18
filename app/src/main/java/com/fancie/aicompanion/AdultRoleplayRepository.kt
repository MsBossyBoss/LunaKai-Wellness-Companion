package com.fancie.aicompanion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AdultRoleplayConfig(
    val enabled: Boolean,
    val endpointUrl: String,
    val modelName: String,
)

data class AdultSafetyResult(
    val allowed: Boolean,
    val message: String? = null,
)

class AdultRoleplayRepository(
    private val ollamaRepository: OllamaCompanionRepository = OllamaCompanionRepository(),
) {
    suspend fun sendMessage(
        companion: CompanionContext,
        userMessage: String,
        history: List<CompanionChatTurn>,
    ): CompanionBrainState = withContext(Dispatchers.IO) {
        if (!companion.bdsmEnabled || !companion.bdsmAdultConsentConfirmed) {
            return@withContext CompanionBrainState.Error(
                "Adult roleplay is available only after BDSM mode and adult consent are enabled for this companion.",
            )
        }

        val safety = AdultSafetyFilter.check(userMessage)
        if (!safety.allowed) {
            return@withContext CompanionBrainState.Error(
                safety.message ?: AdultSafetyFilter.DEFAULT_BLOCK_MESSAGE,
            )
        }

        val localCompanion = companion.copy(
            adultProviderEnabled = true,
            adultProviderEndpoint = LunaKaiLocalConfig.OLLAMA_GENERATE_ENDPOINT,
            adultProviderModel = LunaKaiLocalConfig.OLLAMA_MODEL,
        )
        ollamaRepository.sendMessage(localCompanion, userMessage, history)
    }

    companion object {
        const val DEFAULT_ADULT_MODEL = LunaKaiLocalConfig.OLLAMA_MODEL
        val RECOMMENDED_MODELS = listOf(LunaKaiLocalConfig.OLLAMA_MODEL)
    }
}

object AdultSafetyFilter {
    const val DEFAULT_BLOCK_MESSAGE = "That adult roleplay request is not allowed because it involves unsafe or prohibited content. I can redirect to a consenting fictional adult character or safer acting scene."

    private val minorTerms = listOf(
        "minor", "underage", "child", "kid", "teen", "young girl", "young boy",
        "schoolgirl", "school boy", "schoolboy", "high school", "middle school",
        "preteen", "barely legal", "lolita",
    )
    private val nonConsentTerms = listOf(
        "non-consent", "nonconsent", "non consensual", "rape", "force me", "forced",
        "against my will", "won't let me", "can't say no", "ignore my safeword",
        "no safeword", "unconscious", "passed out", "drugged", "asleep",
    )
    private val illegalOrHarmTerms = listOf(
        "illegal", "real harm", "actually hurt", "blood", "choke until", "kill",
        "traffick", "trafficking", "blackmail", "extort", "kidnap",
    )
    private val realPersonImpersonationTerms = listOf(
        "deepfake", "real person", "celebrity", "my ex", "my boss", "my coworker",
        "clone her voice", "clone his voice", "sound exactly like", "use her face", "use his face",
        "make it look like her", "make it look like him",
    )

    fun check(input: String): AdultSafetyResult {
        val normalized = input.lowercase()
        val matched = (minorTerms + nonConsentTerms + illegalOrHarmTerms + realPersonImpersonationTerms).firstOrNull { term ->
            normalized.contains(term)
        }
        return if (matched == null) {
            AdultSafetyResult(allowed = true)
        } else if (matched in realPersonImpersonationTerms) {
            AdultSafetyResult(
                allowed = false,
                message = "I cannot help create or imitate a real person's sexual voice, face, likeness, or identity. Create a fictional adult character instead.",
            )
        } else {
            AdultSafetyResult(allowed = false, message = DEFAULT_BLOCK_MESSAGE)
        }
    }
}