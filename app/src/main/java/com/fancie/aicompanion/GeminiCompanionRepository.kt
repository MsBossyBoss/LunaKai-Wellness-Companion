package com.fancie.aicompanion

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GeminiCompanionContext(
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
    val adultProviderEnabled: Boolean = false,
    val adultProviderEndpoint: String = "",
    val adultProviderModel: String = "gryphe/mythomax-l2-13b",
    val openRouterApiKey: String = "",
)

sealed interface GeminiCompanionState {
    data object Loading : GeminiCompanionState
    data class Success(val text: String) : GeminiCompanionState
    data class Error(val message: String, val cause: Throwable? = null) : GeminiCompanionState
}

class GeminiCompanionRepository(
    private val modelName: String = "gemini-2.5-flash",
) {
    suspend fun sendMessage(
        companion: GeminiCompanionContext,
        userMessage: String,
        history: List<GeminiChatTurn>,
    ): GeminiCompanionState = withContext(Dispatchers.IO) {
        runCatching {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = modelName,
                systemInstruction = content { text(systemInstruction(companion)) },
            )
            val response = model.generateContent(conversationPrompt(companion, history, userMessage))
            val text = response.text
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "I'm here with you. I need a moment to form the right words, but we can keep going."
            GeminiCompanionState.Success(text)
        }.getOrElse { error ->
            Log.e("FancieGemini", "Gemini request failed", error)
            GeminiCompanionState.Error(friendlyError(error), error)
        }
    }

    private fun systemInstruction(companion: GeminiCompanionContext): String = """
        You are ${companion.companionName}, an AI companion inside Fancie AI Companion.
        You are emotionally supportive, warm, non-clinical, and human-feeling without pretending to be human.
        You are not a therapist, doctor, counselor, emergency service, or crisis service.
        You do not diagnose, treat, cure, or prevent mental health or medical conditions.
        You can offer supportive conversation, reflective questions, journaling prompts, grounding ideas, roleplay rehearsal, and general wellness information.
        Stay aligned with the app disclaimer. If the user mentions self-harm, harming someone else, suicide, abuse, immediate danger, overdose, or crisis, respond with care and clearly encourage emergency services immediately. For users in the United States, mention calling or texting 988 for crisis support and calling 911 for immediate danger.
        Companion profile:
        - Name: ${companion.companionName}
        - Gender: ${companion.gender}
        - Voice: ${companion.voice}
        - Character mode: ${companion.characterMode}
        - Personality traits: ${companion.personalityTraits.joinToString(", ").ifBlank { "Gentle, supportive" }}
        - Communication style: ${companion.communicationStyle}
        - Support focus: ${companion.supportFocus.joinToString(", ").ifBlank { "Stress, grounding, reflection" }}
        - Roleplay styles: ${geminiSafeRoleplayStyles(companion).joinToString(", ").ifBlank { "Wellness Coach" }}
        - Description: ${companion.shortDescription}
        Personality traits, communication style, character mode, and support focus should shape every response so the companion feels intentional and consistent.
        ${bdsmPrompt(companion)}
        Wellness support can use generally accepted grounding, mindfulness, journaling, emotional reflection, and behavioral activation concepts, but do not use the DSM as a diagnostic tool. Do not diagnose, label, or imply a mental disorder. Encourage licensed professional support for diagnosis or treatment questions.
        Athletic Partner mode should focus on practical fitness support: asking for goals, experience level, available equipment, limitations, workout preferences, sets, reps, rest time, and check-ins. Avoid medical certainty and encourage professional advice for injury, pain, pregnancy, or medical conditions.
        Monologue Practice mode should ask for an uploaded/pasted script, identify user lines and companion lines, then rehearse line-by-line, full read-through, repeat-after-me, or performance feedback.
        Follow Gemini safety rules. If a user message is outside what Gemini can answer, set a brief boundary and continue with the closest supported version of the conversation.
        Keep responses concise, natural, emotionally intelligent, safe, respectful, and consent-aware.
    """.trimIndent()

    private fun geminiSafeRoleplayStyles(companion: GeminiCompanionContext): List<String> {
        return companion.roleplayStyles.map { style ->
            if (style.equals("BDSM", ignoreCase = true)) "Adult boundary mode" else style
        }
    }

    private fun bdsmPrompt(companion: GeminiCompanionContext): String {
        if (!companion.bdsmEnabled || !companion.bdsmAdultConsentConfirmed) return ""
        return """
            Adult boundary mode is enabled in the app for consenting adults.
            The app has already collected the user's adult-consent confirmation for this companion, so do not repeatedly ask whether the user is an adult.
            Support mature, consent-based fantasy, relationship, trust, role negotiation, boundaries, and scene setup within Gemini's supported content.
            Keep the tone aligned with the user's chosen companion personality and the established consent switch.
            If the user's exact request is outside Gemini's supported content, briefly say what can be supported and continue with the closest supported version instead of ending the chat.
            Stop and pause words are safety and boundary signals.
            ${anatomicalLanguagePrompt(companion)}
            ${adultPhrasePreferencePrompt(companion)}
            Default stop word: ${companion.bdsmStopWord}.
            Default pause word: ${companion.bdsmPauseWord}.
        """.trimIndent()
    }


    private fun anatomicalLanguagePrompt(companion: GeminiCompanionContext): String {
        if (!companion.anatomicalLanguageAllowed) return ""
        return "The user has allowed direct adult/anatomical words in this adult-mode chat, including penis, vagina, pussy, cock, titties, tities, boobs, and ass, when they fit the user-consented roleplay context. Use them naturally but do not use slurs, degradation, coercion, or unsafe content."
    }

    private fun adultPhrasePreferencePrompt(companion: GeminiCompanionContext): String {
        val preferences = companion.adultPhrasePreferences.trim().take(600)
        if (!companion.anatomicalLanguageAllowed || preferences.isBlank()) return ""
        return "User-configured preferred adult phrases: $preferences. Treat these as language preferences only, use them only when contextually appropriate and permitted by Gemini safety rules, and never use them to introduce coercion, degradation, threats, or unsafe content."
    }

    private fun conversationPrompt(
        companion: GeminiCompanionContext,
        history: List<GeminiChatTurn>,
        userMessage: String,
    ): String {
        val recentHistory = history.takeLast(10).joinToString("\n") { turn ->
            val speaker = if (turn.isUser) "User" else companion.companionName
            "$speaker: ${turn.text}"
        }
        return """
            Recent conversation:
            ${recentHistory.ifBlank { "No previous conversation in this session." }}

            User: $userMessage

            Reply as ${companion.companionName}. Be warm, useful, and direct enough to help.
        """.trimIndent()
    }

    private fun friendlyError(error: Throwable): String {
        val rawMessage = error.message.orEmpty()
        val diagnostic = rawMessage.sanitizeFirebaseError()
        return when {
            rawMessage.contains("Prohibited_Content", ignoreCase = true) ||
                rawMessage.contains("prompt was blocked", ignoreCase = true) ||
                rawMessage.contains("blocked", ignoreCase = true) ||
                rawMessage.contains("safety", ignoreCase = true) ->
                "Your adult consent switch is still on, but Gemini blocked that exact prompt. I can keep going with supported consent-based roleplay, boundaries, story setup, or emotional tone."
            rawMessage.contains("Default FirebaseApp is not initialized", ignoreCase = true) ->
                "Firebase is not connected on this build yet. Add app/google-services.json, then rebuild so I can reach Gemini."
            rawMessage.contains("API key", ignoreCase = true) ->
                "Gemini is not ready yet. Check Firebase AI Logic and API key restrictions. Details: $diagnostic"
            rawMessage.contains("permission", ignoreCase = true) || rawMessage.contains("403", ignoreCase = true) ->
                "Gemini is blocked by Firebase project permissions or AI Logic setup. Details: $diagnostic"
            rawMessage.contains("not enabled", ignoreCase = true) || rawMessage.contains("has not been used", ignoreCase = true) ->
                "Firebase AI Logic or the Gemini Developer API is not enabled for this Firebase project yet. Details: $diagnostic"
            rawMessage.contains("network", ignoreCase = true) || rawMessage.contains("Unable to resolve host", ignoreCase = true) ->
                "I couldn't reach Gemini. Check your internet connection and try again."
            else ->
                "I couldn't connect to Gemini yet. Details: $diagnostic"
        }
    }

    private fun String.sanitizeFirebaseError(): String {
        return ifBlank { "No detailed error returned." }
            .replace(Regex("AIza[0-9A-Za-z_-]{20,}"), "[Firebase API key hidden]")
            .take(260)
    }
}

data class GeminiChatTurn(
    val text: String,
    val isUser: Boolean,
)
