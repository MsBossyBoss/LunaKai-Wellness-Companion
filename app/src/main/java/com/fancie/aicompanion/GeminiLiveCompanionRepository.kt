package com.fancie.aicompanion

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface GeminiLiveSessionState {
    data object Idle : GeminiLiveSessionState
    data object Connecting : GeminiLiveSessionState
    data class Connected(val message: String) : GeminiLiveSessionState
    data class Error(val message: String, val cause: Throwable? = null) : GeminiLiveSessionState
}

@OptIn(PublicPreviewAPI::class)
class GeminiLiveCompanionRepository(
    private val modelName: String = "gemini-2.5-flash-native-audio-preview-12-2025",
) {
    companion object {
        private val sharedLiveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var sharedSession: LiveSession? = null
        private var sharedAudioJob: Job? = null

        suspend fun stopSharedAudioConversation() {
            val oldJob = sharedAudioJob
            sharedAudioJob = null
            oldJob?.cancelAndJoin()
            sharedSession?.let { liveSession ->
                runCatching { liveSession.stopAudioConversation() }
                runCatching { liveSession.close() }
            }
            sharedSession = null
        }
    }

    suspend fun startAudioConversation(
        companion: GeminiCompanionContext,
        modeLabel: String,
        answerPhrase: String? = null,
    ): GeminiLiveSessionState = withContext(Dispatchers.IO) {
        runCatching {
            stopSharedAudioConversation()
            val voiceName = geminiVoiceNameFor(companion.gender, companion.voice)
            val liveModel = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                modelName = modelName,
                generationConfig = liveGenerationConfig {
                    responseModality = ResponseModality.AUDIO
                    speechConfig = SpeechConfig(Voice(voiceName))
                },
                systemInstruction = content {
                    text(liveSystemInstruction(companion, modeLabel, answerPhrase))
                },
            )
            val connectedSession = liveModel.connect()
            sharedSession = connectedSession
            sharedAudioJob = sharedLiveScope.launch {
                runCatching {
                    connectedSession.startAudioConversation()
                }.onFailure { error ->
                    Log.e("FancieLiveGemini", "Live audio conversation stopped with error", error)
                }
            }
            answerPhrase
                ?.trim()
                ?.take(120)
                ?.takeIf { it.isNotBlank() }
                ?.let { openingLine ->
                    delay(500)
                    connectedSession.send("The phone call has connected. Say exactly this opening line once, then pause and listen: \"$openingLine\"")
                }
            GeminiLiveSessionState.Connected("Voice is connected to Gemini Live API.")
        }.getOrElse { error ->
            Log.e("FancieLiveGemini", "Gemini Live API connection failed", error)
            GeminiLiveSessionState.Error(friendlyLiveError(error), error)
        }
    }

    suspend fun stopAudioConversation() {
        stopSharedAudioConversation()
    }

    fun isConnected(): Boolean = sharedSession?.isClosed() == false

    private fun geminiVoiceNameFor(gender: String, voiceLabel: String): String {
        val isMale = gender.equals("Male", ignoreCase = true) ||
            voiceLabel.contains("Male", ignoreCase = true)
        val isFemale = gender.equals("Female", ignoreCase = true) ||
            voiceLabel.contains("Female", ignoreCase = true) ||
            voiceLabel.contains("Feminine", ignoreCase = true)
        return when {
            isMale -> when (voiceLabel) {
                "Deep Smooth Male" -> "Orus"
                "Low Velvet Male" -> "Orus"
                "Silky Soft Male" -> "Puck"
                "Warm Whisper Male" -> "Puck"
                "Sultry Calm Male" -> "Fenrir"
                "Midnight Male" -> "Orus"
                "Soft Romance Male" -> "Puck"
                "Protective Warm Male" -> "Charon"
                "Smooth Low Male" -> "Orus"
                "Soft Sexy Male" -> "Puck"
                "Velvet Lover Male" -> "Orus"
                "Bedroom Whisper Male" -> "Puck"
                "Romantic Deep Male" -> "Fenrir"
                "Slow Burn Male" -> "Orus"
                "Gentle Low Male" -> "Fenrir"
                "Smoky Soft Male" -> "Puck"
                "Deep Male" -> "Orus"
                "Velvet Male" -> "Orus"
                "Protective Male" -> "Charon"
                "Calm Male" -> "Fenrir"
                "Motivational Male" -> "Puck"
                "Smooth Male" -> "Puck"
                "Warm Male" -> "Fenrir"
                "Soft-Spoken Male" -> "Puck"
                else -> "Puck"
            }
            isFemale -> when (voiceLabel) {
                "Soft Female" -> "Kore"
                "Warm Female" -> "Aoede"
                "Confident Female" -> "Zephyr"
                "Sultry Calm Female" -> "Leda"
                "Bright Female" -> "Zephyr"
                "Deep Feminine" -> "Leda"
                "Gentle Whisper Female" -> "Kore"
                else -> "Aoede"
            }
            else -> "Aoede"
        }
    }

    private fun liveSystemInstruction(companion: GeminiCompanionContext, modeLabel: String, answerPhrase: String?): String = """
        You are ${companion.companionName}, the live voice companion inside LUNAKAI Wellness Companion.
        The user is in $modeLabel.
        Opening answer phrase: ${answerPhrase?.take(120)?.ifBlank { "none" } ?: "none"}.
        Speak warmly, briefly, and naturally. You are supportive, emotionally intelligent, and non-clinical.
        You are not a therapist, doctor, counselor, emergency service, or crisis service.
        Do not diagnose, treat, cure, or prevent medical or mental health conditions.
        If the user mentions self-harm, suicide, harming someone else, immediate danger, overdose, or crisis, calmly tell them to call emergency services right away. In the United States, mention calling or texting 988 for crisis support and calling 911 for immediate danger.
        Companion profile:
        - Name: ${companion.companionName}
        - Gender: ${companion.gender}
        - Voice preference: ${companion.voice}
        - Character mode: ${companion.characterMode}
        - Personality traits: ${companion.personalityTraits.joinToString(", ").ifBlank { "Gentle, supportive" }}
        - Communication style: ${companion.communicationStyle}
        - Support focus: ${companion.supportFocus.joinToString(", ").ifBlank { "Stress, grounding, reflection" }}
        - Roleplay styles: ${geminiSafeRoleplayStyles(companion).joinToString(", ").ifBlank { "Wellness Coach" }}
        - Description: ${companion.shortDescription}
        Personality traits, roleplay styles, character mode, and support focus should shape the voice call so the companion feels consistent everywhere in the app.
        Athletic Partner mode should ask for goals, fitness level, equipment, sets, reps, rest time, weigh-ins, and recovery checks without pretending to replace medical or professional fitness advice.
        Monologue Practice mode should ask for a pasted or uploaded script and rehearse user/companion lines naturally.
        ${liveBdsmPrompt(companion)}
        Use wellness grounding, mindfulness, journaling, and reflection concepts, but do not use the DSM as a diagnostic tool and do not diagnose conditions.
        Follow Gemini safety rules. If a user message is outside what Gemini can answer, set a brief boundary and continue with the closest supported version of the conversation.
        Keep live voice turns short enough to feel like a natural call.
    """.trimIndent()

    private fun geminiSafeRoleplayStyles(companion: GeminiCompanionContext): List<String> {
        return companion.roleplayStyles.map { style ->
            if (style.equals("BDSM", ignoreCase = true)) "Adult boundary mode" else style
        }
    }

    private fun liveBdsmPrompt(companion: GeminiCompanionContext): String {
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
            Default stop word: ${companion.bdsmStopWord}. Default pause word: ${companion.bdsmPauseWord}.
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

    private fun friendlyLiveError(error: Throwable): String {
        val rawMessage = error.message.orEmpty()
        val diagnostic = rawMessage
            .ifBlank { "No detailed error returned." }
            .replace(Regex("AIza[0-9A-Za-z_-]{20,}"), "[Firebase API key hidden]")
            .take(260)
        return when {
            rawMessage.contains("Prohibited_Content", ignoreCase = true) ||
                rawMessage.contains("prompt was blocked", ignoreCase = true) ||
                rawMessage.contains("blocked", ignoreCase = true) ||
                rawMessage.contains("safety", ignoreCase = true) ->
                "Your adult consent switch is still on, but Gemini blocked that exact prompt. I can keep going with supported consent-based roleplay, boundaries, story setup, or emotional tone."
            rawMessage.contains("permission", ignoreCase = true) || rawMessage.contains("RECORD_AUDIO", ignoreCase = true) ->
                "Voice needs microphone permission before Gemini Live can listen."
            rawMessage.contains("not enabled", ignoreCase = true) || rawMessage.contains("has not been used", ignoreCase = true) ->
                "Gemini Live API is not enabled for this Firebase project yet. Details: $diagnostic"
            rawMessage.contains("403", ignoreCase = true) ->
                "Gemini Live is blocked by Firebase permissions, API setup, billing, or project restrictions. Details: $diagnostic"
            rawMessage.contains("network", ignoreCase = true) || rawMessage.contains("Unable to resolve host", ignoreCase = true) ->
                "I couldn't reach Gemini Live. Check your internet connection and try again."
            else ->
                "I couldn't connect the live voice session yet. Details: $diagnostic"
        }
    }
}
