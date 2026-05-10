package com.fancie.aicompanion

import android.Manifest
import android.app.Activity
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fancie.aicompanion.ui.theme.FancieAICompanionTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

private val AppBlack = Color(0xFF000000)
private val SoftBlack = Color(0xFF0A0A0F)
private val DeepPlumBlack = Color(0xFF140C14)
private var CardDark = Color(0xFF1A1A22)
private var CardAccent = Color(0xFF2A1626)
private var AccentPink = Color(0xFFE85AAE)
private var AccentRose = Color(0xFFD9488B)
private var AccentPurple = Color(0xFF8E5CFF)
private val TextPrimary = Color(0xFFF8F3F8)
private val TextSecondary = Color(0xFFE9E0EA)
private val BackgroundTop = AppBlack
private val BackgroundMiddle = SoftBlack
private val BackgroundBottom = DeepPlumBlack
private var RosePink = AccentPink
private var DeepRose = AccentRose
private var Lavender = AccentPurple
private var SoftPurple = Color(0xFFB66DFF)
private var CreamWhite = CardDark
private var TextDark = TextPrimary
private var TextMuted = Color(0xFFD8CDD8)
private var CardWhite = Color(0xE61A1A22)
private val GoldAccent = Color(0xFFE8C77A)
private val CalmBlue = Color(0xFF5DA8FF)
private val SuccessGreen = Color(0xFF66D19E)
private val WarningPeach = Color(0xFFE66A77)

private val MediumShape = RoundedCornerShape(20.dp)
private val LargeShape = RoundedCornerShape(28.dp)
private val ExtraLargeShape = RoundedCornerShape(36.dp)
private val PillShape = RoundedCornerShape(50)
private val ScreenPadding = 20.dp
private val SectionSpacing = 18.dp
private val CardPadding = 16.dp
private val CardCorner = 24.dp
private val BubbleHeight = 56.dp
private val GridGap = 12.dp

private data class AppAppearance(
    val mode: String,
    val palette: String,
    val background: List<Color>,
    val card: Color,
    val elevatedCard: Color,
    val control: Color,
    val secondaryButton: Color,
    val accentStart: Color,
    val accentMiddle: Color,
    val accentEnd: Color,
    val text: Color,
    val mutedText: Color,
    val border: Color,
)

private val defaultAppAppearance = appAppearance("Dark", "Rose Plum")
private val LocalAppAppearance = staticCompositionLocalOf { defaultAppAppearance }

private fun appAppearance(mode: String, palette: String): AppAppearance {
    val isLight = mode == "Light"
    val accents = when (palette) {
        "Lavender Dream" -> Triple(Color(0xFFC8A8FF), Color(0xFF9D7BEA), Color(0xFF6E56CF))
        "Calm Blue" -> Triple(Color(0xFFB7D8FF), Color(0xFF5DA8FF), Color(0xFF4B6BEA))
        "Emerald Gold" -> Triple(Color(0xFF9ED8B3), Color(0xFFE8C77A), Color(0xFFB88A28))
        "Peach Glow" -> Triple(Color(0xFFFFC7A8), Color(0xFFE85AAE), Color(0xFFD9488B))
        "Moonlit Mono" -> Triple(Color(0xFFE7E3EA), Color(0xFF8F8798), Color(0xFF4A4452))
        else -> Triple(Color(0xFFE85AAE), Color(0xFFD9488B), Color(0xFF8E5CFF))
    }
    val darkBackground = listOf(Color(0xFF000000), Color(0xFF0A0A0F), Color(0xFF140C14))
    val lightBackground = when (palette) {
        "Lavender Dream" -> listOf(Color(0xFFFFF8FF), Color(0xFFF1E8FF), Color(0xFFEAF2FF))
        "Calm Blue" -> listOf(Color(0xFFF8FCFF), Color(0xFFE9F5FF), Color(0xFFF6ECFF))
        "Emerald Gold" -> listOf(Color(0xFFFFFFF8), Color(0xFFF0F9ED), Color(0xFFFFF3D8))
        "Peach Glow" -> listOf(Color(0xFFFFFAF7), Color(0xFFFFE8DC), Color(0xFFFFEFF8))
        "Moonlit Mono" -> listOf(Color(0xFFFFFFFF), Color(0xFFF3F0F5), Color(0xFFE9E7EE))
        else -> listOf(Color(0xFFFFF7FB), Color(0xFFFFE9F4), Color(0xFFF2EAFF))
    }
    return AppAppearance(
        mode = mode,
        palette = palette,
        background = if (isLight) lightBackground else darkBackground,
        card = if (isLight) Color(0xF8FFFFFF) else Color(0xE61A1A22),
        elevatedCard = if (isLight) Color(0xFFFFFFFF) else Color(0xFF2A1626).copy(alpha = 0.96f),
        control = if (isLight) Color(0xFFF7EDF8) else Color(0xFF1A1A22).copy(alpha = 0.94f),
        secondaryButton = if (isLight) Color(0xFFEADDED) else Color(0xFF2A1626).copy(alpha = 0.92f),
        accentStart = accents.first,
        accentMiddle = accents.second,
        accentEnd = accents.third,
        text = if (isLight) Color(0xFF281D2D) else Color(0xFFF8F3F8),
        mutedText = if (isLight) Color(0xFF6F6176) else Color(0xFFD8CDD8),
        border = if (isLight) Color(0xFF281D2D).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.14f),
    )
}

private fun applyAppearanceGlobals(appearance: AppAppearance) {
    CardDark = appearance.control
    CardAccent = appearance.elevatedCard
    AccentPink = appearance.accentStart
    AccentRose = appearance.accentMiddle
    AccentPurple = appearance.accentEnd
    RosePink = appearance.accentStart
    DeepRose = appearance.accentMiddle
    Lavender = appearance.accentEnd
    SoftPurple = appearance.accentEnd
    CreamWhite = appearance.card
    TextDark = appearance.text
    TextMuted = appearance.mutedText
    CardWhite = appearance.card
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createFancieNotificationChannel()
        setContent {
            FancieAICompanionTheme(dynamicColor = false) {
                Surface(color = Color.Transparent) {
                    FancieApp()
                }
            }
        }
    }

    private fun createFancieNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("android.resource://$packageName/${R.raw.fancie_notification}")
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                "fancie_gentle_support",
                "LUNAKAI gentle support",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Soft, premium companion prompts and wellness reminders."
                setSound(soundUri, attributes)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

private enum class AppRoute(val title: String, val subtitle: String) {
    Splash("LUNAKAI Wellness Companion", "Preparing your companion..."),
    Welcome("Welcome", "Meet the companion that grows with you."),
    Login("Sign In", "Your companion space is waiting for you."),
    Home("Home", ""),
    Chat("Chat", "Choose how you want to connect."),
    TextChat("Text Chat", "Texting you"),
    Wellness("Wellness", "Support, grounding, and reflection."),
    LiveCompanion("Live Companion", "Talk, listen, or open a live companion call."),
    LiveCompanionCall("Live Companion Call", "Talk, text, or sit with your companion in real time."),
    CompanionState("Companion State", "See your companion's current support mode."),
    Companions("My Companions", "Manage your saved companions and choose who is active."),
    Personality("Companion Builder", "Shape identity, voice, look, and support style."),
    Journal("Journal", "A private space to release, reflect, and remember."),
    SavedJournalEntries("Journal Entries", "Review saved written, voice, and video entries."),
    Reflection("Reflection", "Understand what your feelings are trying to show you."),
    Grounding("Grounding", "Come back to your body, one breath at a time."),
    CheckIn("Check-In", "Name your mood and what support you need."),
    Affirmations("Affirmations", "Give your mind something safe to hold."),
    Breathing("Breathing", "Use a soft reset for your nervous system."),
    Fitness("Fitness", "Track movement and choose free workouts."),
    FitnessProgress("Fitness Progress", "Review workout time, sessions, and streaks."),
    Memory("Memory", "Control what your companion remembers about you."),
    Preferences("Settings", "Manage your companion, privacy, storage, and support settings."),
    ProfileInfo("Profile Info", "Manage your profile photo and personal details."),
    AccountSettings("Account Settings", "Manage sign-in, account access, and sign out."),
    VoiceLiveSettings("Voice & Live Settings", "Choose voice, microphone, speaker, and video chat options."),
    JournalStorageSettings("Journal Storage", "Choose where entries are saved."),
    PrivacySettings("Privacy Settings", "Control app lock, sync, previews, and data export."),
    NotificationSettings("Notifications", "Choose reminders, check-ins, and gentle prompts."),
    AppearanceSettings("Appearance", "Theme, accents, and companion orb style."),
    Disclaimer("Wellness Disclaimer", "Before we begin."),
    CrisisResources("Crisis Resources", "Support when things feel urgent."),
    FemaleAvatars("Female Avatars", "Choose Luna, Amara, Nova, or Selene."),
    MaleAvatars("Male Avatars", "Choose Kai, Atlas, Rome, or Saint."),
    CustomizeCompanion("Customize Companion", "Build a companion with your own style, energy, and personality."),
}

private enum class JournalStorage(val label: String, val body: String) {
    Device("Device only", "Stored locally only. Not synced across devices."),
    Cloud("Cloud only", "Stored in Firestore under your account."),
    Both("Device + Cloud", "Stored locally and in Firestore."),
}

private enum class ChatStorage(val label: String, val body: String) {
    Device("User device only", "Messages stay on this phone only."),
    Cloud("Personal storage only", "Messages save only under your signed-in user storage."),
    Both("Device + personal storage", "Messages save on this phone and under your signed-in user storage."),
}

private enum class JournalEntryType(val label: String) {
    TEXT("Written"),
    VOICE("Voice"),
    VIDEO("Video"),
}

private enum class LiveMode {
    TEXT,
    VOICE,
    VIDEO_STYLE,
}

private data class LiveCompanionCallUiState(
    val companionName: String = "Luna",
    val companionPhotoUri: String? = null,
    val avatarType: String = "orb",
    val isMicOn: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isCaptionOn: Boolean = true,
    val isCompanionSpeaking: Boolean = false,
    val isUserSpeaking: Boolean = false,
    val callStatus: String = "Ready to connect",
    val currentCaption: String = "I'm here with you. Tell me what's on your heart.",
    val selectedMode: LiveMode = LiveMode.TEXT,
)

data class BdsmIdentitySettings(
    val enabled: Boolean = false,
    val adultConsentConfirmed: Boolean = false,
    val defaultStopWord: String = "Red",
    val defaultPauseWord: String = "Yellow",
    val anatomicalLanguageAllowed: Boolean = true,
    val preferredAdultPhrases: String = "",
    val adultProviderEnabled: Boolean = false,
    val adultProviderEndpoint: String = "",
    val adultProviderModel: String = "llama-3.1-8b-instruct",
)

private data class BdsmSessionState(
    val isActive: Boolean = false,
    val setupComplete: Boolean = false,
    val setupStep: Int = 0,
    val companionRole: String? = null,
    val userRole: String? = null,
    val stopWord: String = "Red",
    val pauseWord: String = "Yellow",
    val hardLimits: String = "",
    val softLimits: String = "",
    val tone: String = "Soft",
    val aftercarePreference: String = "Soft reassurance",
)

private fun CompanionProfile.activeRoleplayStyles(): List<String> = roleplayStyles.ifEmpty { listOf(roleplayStyle) }

private fun CompanionProfile.isAdultRoleplaySelected(): Boolean = "BDSM" in activeRoleplayStyles()

private fun CompanionProfile.isAdultRoleplayEnabled(): Boolean =
    isAdultRoleplaySelected() && bdsmIdentitySettings.enabled && bdsmIdentitySettings.adultConsentConfirmed

data class CompanionProfile(
    val id: String,
    val name: String,
    val gender: String,
    val voice: String,
    val personalityTags: List<String>,
    val personalityTraits: List<String> = emptyList(),
    val communicationStyle: String,
    val communicationStyles: List<String> = emptyList(),
    val supportFocus: Set<String>,
    val shortDescription: String = "Safe, supportive, and personal.",
    val roleplayEnabled: Boolean = true,
    val roleplayStyle: String = "Supportive scene partner",
    val roleplayStyles: List<String> = emptyList(),
    val characterMode: String = "Companion",
    val relationshipDynamicTone: String = "Warm and respectful",
    val fantasyModePlaceholder: Boolean = false,
    val safeBoundaries: String = "Ask before intense roleplay; keep emotional safety on.",
    val photoUri: String? = null,
    val photoStoragePath: String? = null,
    val avatarType: String = "Glowing Orb",
    val imageResName: String = "",
    val imageResId: Int? = null,
    val isMock: Boolean = true,
    val isActive: Boolean = false,
    val lastUsedDate: String = "Today",
    val backstory: String? = null,
    val zodiacSign: String? = null,
    val characteristics: List<String> = emptyList(),
    val aiGeneratedBio: String? = null,
    val bdsmIdentitySettings: BdsmIdentitySettings = BdsmIdentitySettings(),
)

private data class CompanionExperienceProfile(
    val characterMode: String,
    val primarySupportFocus: String,
    val tone: String,
    val urgency: String,
    val urgencyScore: Int,
    val checkInStyle: String,
    val notificationStrategy: String,
    val sampleNotification: String,
    val chatReply: String,
    val stateMessage: String,
)

private data class ChatPreview(
    val companionId: String,
    val companionName: String,
    val preview: String,
    val time: String,
    val unreadCount: Int = 0,
    val photoUri: String? = null,
    val imageResId: Int? = null,
)

private data class ChatSummaryPreview(
    val companionId: String,
    val lastMessage: String,
    val updatedAt: Long,
)

private data class JournalEntry(
    val date: String,
    val mood: String,
    val title: String,
    val preview: String,
    val storageLocation: JournalStorage,
    val entryType: JournalEntryType = JournalEntryType.TEXT,
    val voiceEntryUri: String? = null,
    val videoEntryUri: String? = null,
    val videoStoragePath: String? = null,
)

private data class ReflectionPrompt(
    val title: String,
    val description: String,
)

private data class MemoryCategory(
    val title: String,
    val description: String,
    val enabled: Boolean,
)

private data class FreeWorkout(
    val title: String,
    val level: String,
    val minutes: Int,
    val focus: String,
    val moves: List<String>,
)

private data class AvatarOption(
    val name: String,
    val resName: String,
    val resId: Int,
    val description: String,
)

// Firebase-ready document shapes. These mirror the planned Firestore structure:
// users/{userId}/profile, users/{userId}/preferences, users/{userId}/companions,
// journalEntries, memories, checkIns, chats/{chatId}/messages.
// Storage uploads should use FirebaseStoragePaths so every file starts with users/{userId}/.
private data class FirebaseUserProfileDoc(val displayName: String, val email: String, val createdAt: String)
private data class FirebaseUserPreferencesDoc(
    val activeCompanionId: String?,
    val defaultJournalStorage: String,
    val askStorageEveryTime: Boolean,
    val hasAcceptedDisclaimer: Boolean,
    val hideDisclaimerOnLaunch: Boolean,
    val preferredVoiceGender: String,
    val theme: String,
    val cloudSyncEnabled: Boolean,
)

private fun femaleAvatarOptions() = listOf(
    AvatarOption("Luna", "luna_mock", R.drawable.luna_mock, "Gentle, reflective, and here for you."),
    AvatarOption("Amara", "amara_mock", R.drawable.amara_mock, "Soft, grounded, and emotionally warm."),
    AvatarOption("Nova", "nova_mock", R.drawable.nova_mock, "Bright, creative, and calming."),
    AvatarOption("Selene", "selene_mock", R.drawable.selene_mock, "Elegant, quiet, and intuitive."),
)

private fun maleAvatarOptions() = listOf(
    AvatarOption("Kai", "kai_mock", R.drawable.kai_mock, "Steady, protective, and here to help you slow things down."),
    AvatarOption("Atlas", "atlas_mock", R.drawable.atlas_mock, "Grounded, clear, and reassuring."),
    AvatarOption("Rome", "rome_mock", R.drawable.rome_mock, "Smooth, thoughtful, and motivating."),
    AvatarOption("Saint", "saint_mock", R.drawable.saint_mock, "Calm, safe, and deeply steady."),
)

private const val LUNAKAI_PREFS = "lunakai_preferences"
private const val KEY_COMPANIONS = "companions_json"
private const val KEY_ACTIVE_COMPANION = "active_companion_id"
private const val FRESH_CHAT_GREETING = "Hey, There!"
private const val CHAT_RESET_HEY_THERE_VERSION = 1
private fun chatResetHeyThereKey(uid: String) = "chat_reset_hey_there_version_$uid"

private fun Context.lunakaiPrefs() = getSharedPreferences(LUNAKAI_PREFS, Context.MODE_PRIVATE)
private fun Context.prefString(key: String, default: String = "") = lunakaiPrefs().getString(key, default) ?: default
private fun Context.prefBoolean(key: String, default: Boolean = false) = lunakaiPrefs().getBoolean(key, default)
private fun Context.prefInt(key: String, default: Int = 0) = lunakaiPrefs().getInt(key, default)
private fun Context.savePref(key: String, value: String) = lunakaiPrefs().edit().putString(key, value).apply()
private fun Context.savePref(key: String, value: Boolean) = lunakaiPrefs().edit().putBoolean(key, value).apply()
private fun Context.savePref(key: String, value: Int) = lunakaiPrefs().edit().putInt(key, value).apply()

private fun defaultCompanions() = listOf(
    CompanionProfile(
        id = "luna",
        name = "Luna",
        gender = "Female",
        voice = "Soft Female",
        personalityTags = listOf("Soft", "Gentle", "Reflective", "Supportive"),
        personalityTraits = listOf("Kind", "Sweet", "Playful"),
        communicationStyle = "Gentle",
        communicationStyles = listOf("Gentle", "Soothing"),
        supportFocus = setOf("Stress", "Reflection", "Journaling"),
        shortDescription = "Gentle, reflective, and here for you.",
        roleplayStyle = "Wellness Coach",
        roleplayStyles = listOf("Wellness Coach"),
        characterMode = "Best Friend",
        avatarType = "Glowing Orb",
        imageResName = "luna_mock",
        imageResId = R.drawable.luna_mock,
        isMock = true,
        isActive = true,
        lastUsedDate = "Today",
    ),
    CompanionProfile(
        id = "kai",
        name = "Kai",
        gender = "Male",
        voice = "Calm Male",
        personalityTags = listOf("Calm", "Protective", "Honest", "Grounded"),
        personalityTraits = listOf("Protective", "Ambitious", "Kind"),
        communicationStyle = "Direct",
        communicationStyles = listOf("Direct", "Motivational"),
        supportFocus = setOf("Confidence", "Grounding"),
        shortDescription = "Steady, protective, and here to help you slow things down.",
        roleplayStyle = "Athletic Partner",
        roleplayStyles = listOf("Athletic Partner"),
        characterMode = "Assistant",
        avatarType = "AI Avatar Placeholder",
        imageResName = "kai_mock",
        imageResId = R.drawable.kai_mock,
        isMock = true,
        isActive = false,
        lastUsedDate = "Yesterday",
    ),
)

private fun newCompanionDraft(base: CompanionProfile): CompanionProfile {
    return base.copy(
        id = "draft-${System.currentTimeMillis()}",
        name = "",
        gender = "No preference",
        voice = "Neutral Calm",
        personalityTags = emptyList(),
        personalityTraits = emptyList(),
        communicationStyle = "Gentle",
        communicationStyles = emptyList(),
        supportFocus = emptySet(),
        shortDescription = "",
        roleplayStyle = "Wellness Coach",
        roleplayStyles = listOf("Wellness Coach"),
        characterMode = "Best Friend",
        photoUri = null,
        photoStoragePath = null,
        avatarType = "Glowing Orb",
        imageResName = "",
        imageResId = null,
        isMock = false,
        isActive = false,
        lastUsedDate = "Today",
        bdsmIdentitySettings = BdsmIdentitySettings(),
    )
}

private fun loadCompanions(context: Context): List<CompanionProfile> {
    val defaults = defaultCompanions()
    val raw = context.prefString(KEY_COMPANIONS)
    if (raw.isBlank()) return defaults
    return runCatching {
        val saved = JSONArray(raw).toCompanionList()
        orderedCompanions(saved)
    }.getOrElse {
        Log.w("LunaKaiPrefs", "Could not load companions; using defaults", it)
        defaults
    }
}

private fun orderedCompanions(companions: List<CompanionProfile>): List<CompanionProfile> {
    val defaults = defaultCompanions()
    val savedById = companions.associateBy { it.id }
    val luna = (savedById["luna"] ?: defaults[0]).copy(id = "luna", name = "Luna", isMock = true)
    val kai = (savedById["kai"] ?: defaults[1]).copy(id = "kai", name = "Kai", isMock = true)
    val customCompanions = companions
        .filterNot { it.id == "luna" || it.id == "kai" || it.id.startsWith("draft-") }
        .distinctBy { it.id }
    return listOf(luna, kai) + customCompanions
}

private fun saveCompanions(context: Context, companions: List<CompanionProfile>) {
    val normalized = orderedCompanions(companions).map { companion ->
        when (companion.id) {
            "luna" -> companion.copy(name = "Luna", isMock = true)
            "kai" -> companion.copy(name = "Kai", isMock = true)
            else -> companion
        }
    }
    context.savePref(KEY_COMPANIONS, JSONArray(normalized.map { it.toJson() }).toString())
}

private fun CompanionProfile.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", if (id == "luna") "Luna" else if (id == "kai") "Kai" else name)
    .put("gender", gender)
    .put("voice", voice)
    .put("personalityTags", JSONArray(personalityTags))
    .put("personalityTraits", JSONArray(personalityTraits))
    .put("communicationStyle", communicationStyle)
    .put("communicationStyles", JSONArray(communicationStyles))
    .put("supportFocus", JSONArray(supportFocus.toList()))
    .put("shortDescription", shortDescription)
    .put("roleplayEnabled", roleplayEnabled)
    .put("roleplayStyle", roleplayStyle)
    .put("roleplayStyles", JSONArray(roleplayStyles))
    .put("characterMode", characterMode)
    .put("safeBoundaries", safeBoundaries)
    .put("photoUri", photoUri)
    .put("photoStoragePath", photoStoragePath)
    .put("avatarType", avatarType)
    .put("imageResName", imageResName)
    .put("isMock", isMock)
    .put("isActive", isActive)
    .put("lastUsedDate", lastUsedDate)
    .put("bdsmIdentitySettings", JSONObject()
        .put("enabled", bdsmIdentitySettings.enabled)
        .put("adultConsentConfirmed", bdsmIdentitySettings.adultConsentConfirmed)
        .put("defaultStopWord", bdsmIdentitySettings.defaultStopWord)
        .put("defaultPauseWord", bdsmIdentitySettings.defaultPauseWord)
        .put("anatomicalLanguageAllowed", bdsmIdentitySettings.anatomicalLanguageAllowed)
        .put("preferredAdultPhrases", bdsmIdentitySettings.preferredAdultPhrases)
        .put("adultProviderEnabled", bdsmIdentitySettings.adultProviderEnabled)
        .put("adultProviderEndpoint", bdsmIdentitySettings.adultProviderEndpoint)
        .put("adultProviderModel", bdsmIdentitySettings.adultProviderModel)
    )

private fun JSONArray.toCompanionList(): List<CompanionProfile> {
    return (0 until length()).mapNotNull { index ->
        optJSONObject(index)?.toCompanionProfile()
    }
}

private fun JSONObject.toCompanionProfile(): CompanionProfile {
    val bdsm = optJSONObject("bdsmIdentitySettings")
    val resName = optString("imageResName", "")
    return CompanionProfile(
        id = optString("id"),
        name = optString("name"),
        gender = optString("gender", "No preference"),
        voice = optString("voice", "Neutral Calm"),
        personalityTags = optJSONArray("personalityTags").toStringList(),
        personalityTraits = optJSONArray("personalityTraits").toStringList(),
        communicationStyle = optString("communicationStyle", "Gentle"),
        communicationStyles = optJSONArray("communicationStyles").toStringList(),
        supportFocus = optJSONArray("supportFocus").toStringList().toSet(),
        shortDescription = optString("shortDescription", "Safe, supportive, and personal."),
        roleplayEnabled = optBoolean("roleplayEnabled", true),
        roleplayStyle = optString("roleplayStyle", "Wellness Coach"),
        roleplayStyles = optJSONArray("roleplayStyles").toStringList(),
        characterMode = optString("characterMode", "Companion"),
        safeBoundaries = optString("safeBoundaries", "Ask before intense roleplay; keep emotional safety on."),
        photoUri = optString("photoUri").takeIf { it.isNotBlank() && it != "null" },
        photoStoragePath = optString("photoStoragePath").takeIf { it.isNotBlank() && it != "null" },
        avatarType = optString("avatarType", "Glowing Orb"),
        imageResName = resName,
        imageResId = imageResIdForName(resName),
        isMock = optBoolean("isMock", false),
        isActive = optBoolean("isActive", false),
        lastUsedDate = optString("lastUsedDate", "Today"),
        bdsmIdentitySettings = BdsmIdentitySettings(
            enabled = bdsm?.optBoolean("enabled", false) ?: false,
            adultConsentConfirmed = bdsm?.optBoolean("adultConsentConfirmed", false) ?: false,
            defaultStopWord = bdsm?.optString("defaultStopWord", "Red") ?: "Red",
            defaultPauseWord = bdsm?.optString("defaultPauseWord", "Yellow") ?: "Yellow",
            anatomicalLanguageAllowed = bdsm?.optBoolean("anatomicalLanguageAllowed", true) ?: true,
            preferredAdultPhrases = bdsm?.optString("preferredAdultPhrases", "") ?: "",
            adultProviderEnabled = bdsm?.optBoolean("adultProviderEnabled", false) ?: false,
            adultProviderEndpoint = bdsm?.optString("adultProviderEndpoint", "") ?: "",
            adultProviderModel = bdsm?.optString("adultProviderModel", "llama-3.1-8b-instruct") ?: "llama-3.1-8b-instruct",
        ),
    )
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optString(it).takeIf { value -> value.isNotBlank() } }
}

private fun imageResIdForName(name: String): Int? = when (name) {
    "luna_mock" -> R.drawable.luna_mock
    "amara_mock" -> R.drawable.amara_mock
    "nova_mock" -> R.drawable.nova_mock
    "selene_mock" -> R.drawable.selene_mock
    "kai_mock" -> R.drawable.kai_mock
    "atlas_mock" -> R.drawable.atlas_mock
    "rome_mock" -> R.drawable.rome_mock
    "saint_mock" -> R.drawable.saint_mock
    else -> null
}

private fun chatMessagesKey(companionId: String) = "chat_messages_$companionId"

private fun loadLocalChatMessages(context: Context, companionId: String): List<ChatMessage> {
    val raw = context.prefString(chatMessagesKey(companionId))
    if (raw.isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { item ->
                ChatMessage(sender = item.optString("sender"), text = item.optString("text"))
            }
        }
    }.getOrElse {
        Log.w("LunaKaiChat", "Could not load local chat for $companionId", it)
        emptyList()
    }
}

private fun saveLocalChatMessages(context: Context, companionId: String, messages: List<ChatMessage>) {
    val array = JSONArray()
    messages.takeLast(120).forEach { message ->
        array.put(
            JSONObject()
                .put("sender", message.sender)
                .put("text", message.text)
                .put("isUser", message.isUser),
        )
    }
    context.savePref(chatMessagesKey(companionId), array.toString())
}

private fun appendLocalChatExchange(
    context: Context,
    companionId: String,
    companionName: String,
    userText: String,
    companionText: String,
) {
    val current = loadLocalChatMessages(context, companionId)
    saveLocalChatMessages(
        context,
        companionId,
        current + ChatMessage(sender = ChatMessage.SENDER_USER, text = userText) + ChatMessage(sender = ChatMessage.SENDER_COMPANION, companionId = companionId, text = companionText),
    )
}

private suspend fun clearCloudChatMessages(companionId: String) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        Log.w("FancieFirestore", "User must be signed in before saving chat.")
        return
    }
    resetSingleCloudChatToFreshGreeting(user.uid, companionId, null)
}

private fun resetLocalChatToFreshGreeting(context: Context, companion: CompanionProfile) {
    saveLocalChatMessages(
        context,
        companion.id,
        listOf(
            ChatMessage(
                id = "fresh_${companion.id}",
                chatId = stableChatIdForCompanion(companion.id),
                companionId = companion.id,
                sender = ChatMessage.SENDER_COMPANION,
                text = FRESH_CHAT_GREETING,
                mode = ChatMessage.MODE_TEXT,
                createdAt = System.currentTimeMillis(),
            ),
        ),
    )
}

private suspend fun resetSingleCloudChatToFreshGreeting(uid: String, companionId: String, companionName: String?) {
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val chatId = stableChatIdForCompanion(companionId)
    val userChats = firestore.collection("users").document(uid).collection("chats")
    val chatIdsToClear = listOf(chatId, "text_$companionId", "call_$companionId", "video_$companionId").distinct()

    chatIdsToClear.forEach { idToClear ->
        val chatRef = userChats.document(idToClear)
        val snapshot = chatRef.collection("messages").get().awaitTask()
        val deleteBatch = firestore.batch()
        snapshot.documents.forEach { deleteBatch.delete(it.reference) }
        if (idToClear != chatId) deleteBatch.delete(chatRef)
        deleteBatch.commit().awaitTask()
    }

    val createdAt = System.currentTimeMillis()
    val messageId = "fresh_${companionId}_${createdAt}"
    val chatRef = userChats.document(chatId)
    chatRef.collection("messages").document(messageId).set(
        mapOf(
            "id" to messageId,
            "chatId" to chatId,
            "companionId" to companionId,
            "sender" to ChatMessage.SENDER_COMPANION,
            "text" to FRESH_CHAT_GREETING,
            "mode" to ChatMessage.MODE_TEXT,
            "createdAt" to createdAt,
        ),
    ).awaitTask()
    chatRef.set(
        buildMap<String, Any> {
            put("companionId", companionId)
            companionName?.let { put("companionName", it) }
            put("lastMessage", FRESH_CHAT_GREETING)
            put("updatedAt", createdAt)
            put("mode", ChatMessage.MODE_TEXT)
        },
        com.google.firebase.firestore.SetOptions.merge(),
    ).awaitTask()
}

private suspend fun resetAllCompanionMessagesToFreshGreeting(context: Context, companions: List<CompanionProfile>) {
    companions.forEach { resetLocalChatToFreshGreeting(context, it) }
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        Log.w("FancieFirestore", "User must be signed in before saving chat.")
        return
    }
    companions.forEach { companion ->
        resetSingleCloudChatToFreshGreeting(user.uid, companion.id, companion.name)
    }
}
@Composable
private fun FancieApp() {
    val context = LocalContext.current
    var signedIn by remember { mutableStateOf(firebaseCurrentUserExists()) }
    var route by remember { mutableStateOf(if (signedIn) AppRoute.Home else AppRoute.Welcome) }
    var hasAcceptedDisclaimer by remember { mutableStateOf(context.prefBoolean("has_accepted_disclaimer", true)) }
    var hideDisclaimerOnLaunch by remember { mutableStateOf(context.prefBoolean("hide_disclaimer_on_launch", false)) }
    var showDisclaimerOnLaunch by remember { mutableStateOf(context.prefBoolean("show_disclaimer_on_launch", true)) }
    var defaultJournalStorage by remember { mutableStateOf(runCatching { JournalStorage.valueOf(context.prefString("default_journal_storage", JournalStorage.Device.name)) }.getOrDefault(JournalStorage.Device)) }
    var defaultChatStorage by remember { mutableStateOf(runCatching { ChatStorage.valueOf(context.prefString("default_chat_storage", ChatStorage.Cloud.name)) }.getOrDefault(ChatStorage.Both)) }
    var askStorageEveryTime by remember { mutableStateOf(context.prefBoolean("ask_storage_every_time", false)) }
    var showJournalStorageSheet by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf("Calm") }
    var selectedJournalMood by remember { mutableStateOf("Calm") }
    var journalTitle by remember { mutableStateOf("Today I needed clarity") }
    var journalBody by remember { mutableStateOf("What do you need to get out of your heart today?") }
    var videoJournalState by remember { mutableStateOf("Ready") }
    var profilePhotoUri by remember { mutableStateOf<String?>(context.prefString("profile_photo_uri").takeIf { it.isNotBlank() }) }
    var displayName by remember { mutableStateOf(context.prefString("display_name", "Fancie")) }
    var preferredName by remember { mutableStateOf(context.prefString("preferred_name")) }
    var pronouns by remember { mutableStateOf(context.prefString("pronouns")) }
    var birthday by remember { mutableStateOf(context.prefString("birthday")) }
    var location by remember { mutableStateOf(context.prefString("location")) }
    var aboutMe by remember { mutableStateOf(context.prefString("about_me")) }
    var wellnessPreferences by remember { mutableStateOf(context.prefString("wellness_preferences", "Grounding, journaling, calm reminders")) }
    var notificationPreferences by remember { mutableStateOf(context.prefString("notification_preferences", "Daily gentle check-ins")) }
    var cloudSyncPreference by remember { mutableStateOf(context.prefBoolean("cloud_sync_preference", false)) }
    var appearanceMode by remember { mutableStateOf(context.prefString("appearance_mode", "Dark")) }
    var appearancePalette by remember { mutableStateOf(context.prefString("appearance_palette", "Rose Plum")) }
    var fitnessActiveWorkoutTitle by remember { mutableStateOf("Soft Start Stretch") }
    var fitnessTimerRunning by remember { mutableStateOf(false) }
    var fitnessSessionSeconds by remember { mutableStateOf(0) }
    var fitnessMinutesToday by remember { mutableStateOf(0) }
    var fitnessWorkoutsCompleted by remember { mutableStateOf(0) }
    var fitnessStreakDays by remember { mutableStateOf(0) }
    var fitnessGoalMinutes by remember { mutableStateOf(30f) }
    var fitnessTotalSeconds by remember { mutableStateOf(0) }
    var fitnessLastWorkoutTitle by remember { mutableStateOf("No workout completed yet") }
    val appearance = remember(appearanceMode, appearancePalette) {
        appAppearance(appearanceMode, appearancePalette)
    }
    applyAppearanceGlobals(appearance)

    LaunchedEffect(
        hasAcceptedDisclaimer,
        hideDisclaimerOnLaunch,
        showDisclaimerOnLaunch,
        defaultJournalStorage,
        defaultChatStorage,
        askStorageEveryTime,
        profilePhotoUri,
        displayName,
        preferredName,
        pronouns,
        birthday,
        location,
        aboutMe,
        wellnessPreferences,
        notificationPreferences,
        cloudSyncPreference,
        appearanceMode,
        appearancePalette,
    ) {
        context.savePref("has_accepted_disclaimer", hasAcceptedDisclaimer)
        context.savePref("hide_disclaimer_on_launch", hideDisclaimerOnLaunch)
        context.savePref("show_disclaimer_on_launch", showDisclaimerOnLaunch)
        context.savePref("default_journal_storage", defaultJournalStorage.name)
        context.savePref("default_chat_storage", defaultChatStorage.name)
        context.savePref("ask_storage_every_time", askStorageEveryTime)
        context.savePref("profile_photo_uri", profilePhotoUri.orEmpty())
        context.savePref("display_name", displayName)
        context.savePref("preferred_name", preferredName)
        context.savePref("pronouns", pronouns)
        context.savePref("birthday", birthday)
        context.savePref("location", location)
        context.savePref("about_me", aboutMe)
        context.savePref("wellness_preferences", wellnessPreferences)
        context.savePref("notification_preferences", notificationPreferences)
        context.savePref("cloud_sync_preference", cloudSyncPreference)
        context.savePref("appearance_mode", appearanceMode)
        context.savePref("appearance_palette", appearancePalette)
    }

    LaunchedEffect(fitnessTimerRunning) {
        while (fitnessTimerRunning) {
            delay(1000)
            fitnessSessionSeconds += 1
        }
    }

    val companions = remember { mutableStateListOf<CompanionProfile>().apply { addAll(loadCompanions(context)) } }
    var activeCompanionId by remember { mutableStateOf(context.prefString(KEY_ACTIVE_COMPANION, "luna")) }
    var companionBuilderDraft by remember { mutableStateOf<CompanionProfile?>(null) }
    val activeCompanion = companions.firstOrNull { it.id == activeCompanionId } ?: companions.first()
    val companionPersistenceKey = companions.joinToString("|") { it.toJson().toString() }
    LaunchedEffect(activeCompanionId, companionPersistenceKey) {
        val safeActiveId = activeCompanionId.takeIf { id -> companions.any { it.id == id } } ?: "luna"
        context.savePref(KEY_ACTIVE_COMPANION, safeActiveId)
        saveCompanions(context, companions)
    }
    LaunchedEffect(signedIn, companionPersistenceKey) {
        val user = FirebaseAuth.getInstance().currentUser ?: return@LaunchedEffect
        val resetKey = chatResetHeyThereKey(user.uid)
        if (context.prefInt(resetKey, 0) < CHAT_RESET_HEY_THERE_VERSION) {
            runCatching { resetAllCompanionMessagesToFreshGreeting(context, orderedCompanions(companions)) }
                .onSuccess { context.savePref(resetKey, CHAT_RESET_HEY_THERE_VERSION) }
                .onFailure { error -> Log.w("FancieChatReset", "Could not reset chat messages", error) }
        }
    }
    val routeHistory = remember { mutableStateListOf<AppRoute>() }
    val topLevelRoutes = setOf(
        AppRoute.Chat,
        AppRoute.CompanionState,
        AppRoute.Companions,
        AppRoute.CrisisResources,
        AppRoute.Disclaimer,
        AppRoute.Home,
        AppRoute.Journal,
        AppRoute.Memory,
        AppRoute.Personality,
        AppRoute.Preferences,
        AppRoute.Wellness,
    )

    fun navigateTo(nextRoute: AppRoute, recordHistory: Boolean = true) {
        if (nextRoute == route) return
        val topLevelSwitch = route in topLevelRoutes && nextRoute in topLevelRoutes
        if (topLevelSwitch) routeHistory.clear()
        if (recordHistory && !topLevelSwitch) routeHistory.add(route)
        route = nextRoute
    }

    fun goBack(fallbackRoute: AppRoute = AppRoute.Home) {
        route = if (routeHistory.isNotEmpty()) {
            routeHistory.removeAt(routeHistory.lastIndex)
        } else {
            fallbackRoute
        }
    }

    fun signOutAndReturnToLogin() {
        firebaseSignOut()
        signedIn = false
        routeHistory.clear()
        route = AppRoute.Login
    }

    fun setActiveCompanion(id: String) {
        activeCompanionId = id
        companions.forEachIndexed { index, companion ->
            companions[index] = companion.copy(
                name = if (companion.id == "luna") "Luna" else if (companion.id == "kai") "Kai" else companion.name,
                isActive = companion.id == id,
            )
        }
    }

    fun openCreateCompanion() {
        companionBuilderDraft = newCompanionDraft(activeCompanion)
        navigateTo(AppRoute.Personality)
    }

    fun updateCompanion(changed: CompanionProfile) {
        val normalized = when (changed.id) {
            "luna" -> changed.copy(name = "Luna", isMock = true)
            "kai" -> changed.copy(name = "Kai", isMock = true)
            else -> changed
        }
        if (companionBuilderDraft != null) {
            companionBuilderDraft = normalized
        } else {
            val index = companions.indexOfFirst { it.id == normalized.id }
            if (index >= 0) companions[index] = normalized
        }
    }

    LaunchedEffect(Unit) {
        delay(850)
        if (route == AppRoute.Splash) route = AppRoute.Welcome
    }

    LaunchedEffect(route) {
        if (route !in listOf(AppRoute.LiveCompanionCall, AppRoute.LiveCompanion)) {
            GeminiLiveCompanionRepository.stopSharedAudioConversation()
        }
    }

    BackHandler(enabled = signedIn && (route != AppRoute.Home || showJournalStorageSheet)) {
        if (showJournalStorageSheet) {
            showJournalStorageSheet = false
        } else {
            goBack()
        }
    }

    BackHandler(enabled = !signedIn && route !in listOf(AppRoute.Splash, AppRoute.Welcome)) {
        when (route) {
            AppRoute.CrisisResources -> route = AppRoute.Disclaimer
            AppRoute.Disclaimer -> route = AppRoute.Welcome
            AppRoute.Login -> route = AppRoute.Welcome
            else -> route = AppRoute.Welcome
        }
    }

    if (!signedIn) {
        when (route) {
            AppRoute.Splash -> SplashScreen()
            AppRoute.Welcome -> WelcomeScreen(
                onGetStarted = {
                    navigateTo(if (!hasAcceptedDisclaimer || (showDisclaimerOnLaunch && !hideDisclaimerOnLaunch)) {
                        AppRoute.Disclaimer
                    } else {
                        AppRoute.Login
                    })
                },
                onLogin = { navigateTo(AppRoute.Login) },
            )
            AppRoute.Disclaimer -> DisclaimerScreen(
                hasAcceptedDisclaimer = hasAcceptedDisclaimer,
                hideDisclaimerOnLaunch = hideDisclaimerOnLaunch,
                onAcceptedChange = { hasAcceptedDisclaimer = it },
                onHideChange = { hideDisclaimerOnLaunch = it },
                onContinue = {
                    hasAcceptedDisclaimer = true
                    navigateTo(AppRoute.Login)
                },
                onCrisis = { navigateTo(AppRoute.CrisisResources) },
            )
            AppRoute.CrisisResources -> CrisisResourcesScreen(onBack = { goBack(AppRoute.Disclaimer) })
            else -> LoginScreen(
                defaultChatStorage = defaultChatStorage,
                onChatStorageChange = { defaultChatStorage = it },
                onSignedIn = {
                    signedIn = true
                    routeHistory.clear()
                    route = AppRoute.Home
                },
            )
        }
        return
    }

    CompositionLocalProvider(LocalAppAppearance provides appearance) {
        AppShell(
            route = route,
            activeCompanion = activeCompanion,
            profilePhotoUri = profilePhotoUri,
            onNavigate = { navigateTo(it) },
            onProfileInfo = { navigateTo(AppRoute.ProfileInfo) },
            onSignOut = { signOutAndReturnToLogin() },
            onJournalStorage = { showJournalStorageSheet = true },
            onJournalStorageSelected = { defaultJournalStorage = it },
            content = {
            when (route) {
                AppRoute.Home -> HomeDashboardScreen(
                    companion = activeCompanion,
                    companions = companions,
                    selectedMood = selectedMood,
                    onMoodSelected = { selectedMood = it },
                    onNavigate = { navigateTo(it) },
                    onSelectCompanion = { id -> setActiveCompanion(id) },
                    onCreateCompanion = { openCreateCompanion() },
                )
                AppRoute.Wellness -> WellnessHubScreen(onNavigate = { navigateTo(it) })
                AppRoute.LiveCompanion -> LiveCompanionScreen(
                    companion = activeCompanion,
                    onNavigate = { navigateTo(it) },
                )
                AppRoute.Chat -> ChatConnectScreen(
                    companion = activeCompanion,
                    onText = { navigateTo(AppRoute.TextChat) },
                    onCall = { navigateTo(AppRoute.LiveCompanionCall) },
                    onVideo = { navigateTo(AppRoute.LiveCompanionCall) },
                )
                AppRoute.TextChat -> CompanionChatScreen(
                    companion = activeCompanion,
                    chatStorage = defaultChatStorage,
                    onNavigate = { navigateTo(it) },
                )
                AppRoute.LiveCompanionCall -> LiveCompanionCallScreen(
                    companion = activeCompanion,
                    onNavigate = { navigateTo(it) },
                )
                AppRoute.CompanionState -> CompanionStateScreen(
                    companion = activeCompanion,
                    onNavigate = { navigateTo(it) },
                )
                AppRoute.Companions -> MyCompanionsScreen(
                    companions = companions,
                    activeCompanionId = activeCompanionId,
                    onSetActive = { id -> setActiveCompanion(id) },
                    onCreate = { openCreateCompanion() },
                    onEdit = { id ->
                        companionBuilderDraft = null
                        setActiveCompanion(id)
                        navigateTo(AppRoute.Personality)
                    },
                    onChat = {
                        setActiveCompanion(it)
                        navigateTo(AppRoute.TextChat)
                    },
                    onLive = {
                        setActiveCompanion(it)
                        navigateTo(AppRoute.LiveCompanionCall)
                    },
                    onDelete = { id ->
                        if (id !in listOf("luna", "kai") && companions.size > 2) {
                            companions.removeAll { it.id == id }
                            if (activeCompanionId == id) activeCompanionId = companions.first().id
                        }
                    },
                )
                AppRoute.Personality -> PersonalityBuilderScreen(
                    companion = companionBuilderDraft ?: activeCompanion,
                    isCreatingNew = companionBuilderDraft != null,
                    onCompanionChange = { changed -> updateCompanion(changed) },
                    onSave = {
                        companionBuilderDraft?.let { draft ->
                            val newCompanion = draft.copy(
                                id = "custom-${System.currentTimeMillis()}",
                                name = draft.name.ifBlank { "New Companion" },
                                isMock = false,
                                isActive = true,
                                lastUsedDate = "Today",
                            )
                            val nextCompanions = orderedCompanions(companions.map { it.copy(isActive = false) } + newCompanion)
                            companions.clear()
                            companions.addAll(nextCompanions)
                            activeCompanionId = newCompanion.id
                            companionBuilderDraft = null
                        }
                        navigateTo(AppRoute.Companions)
                    },
                    onOpenFemaleAvatars = { navigateTo(AppRoute.FemaleAvatars) },
                    onOpenMaleAvatars = { navigateTo(AppRoute.MaleAvatars) },
                    onCustomizeCompanion = { navigateTo(AppRoute.CustomizeCompanion) },
                )
                AppRoute.CustomizeCompanion -> CustomizeCompanionScreen(
                    baseCompanion = activeCompanion,
                    onCancel = { goBack(AppRoute.Personality) },
                    onSave = { companion, setActive ->
                        val newCompanion = companion.copy(
                            id = "custom-${System.currentTimeMillis()}",
                            name = companion.name.ifBlank { "New Companion" },
                            isMock = false,
                            isActive = setActive,
                            lastUsedDate = "Today",
                        )
                        val baseCompanions = if (setActive) companions.map { it.copy(isActive = false) } else companions.toList()
                        val nextCompanions = orderedCompanions(baseCompanions + newCompanion)
                        companions.clear()
                        companions.addAll(nextCompanions)
                        if (setActive) activeCompanionId = newCompanion.id
                        companionBuilderDraft = null
                        navigateTo(AppRoute.Companions)
                    },
                )
                AppRoute.FemaleAvatars -> AvatarSelectionScreen(
                    title = "Female Avatars",
                    options = femaleAvatarOptions(),
                    selected = (companionBuilderDraft ?: activeCompanion).avatarType,
                    onBack = { goBack(AppRoute.Personality) },
                    onSelected = { option ->
                        val target = companionBuilderDraft ?: activeCompanion
                        val changed = target.copy(
                            gender = "Female",
                            voice = voiceForGender("Female", target.voice),
                            avatarType = option.name,
                            imageResName = option.resName,
                            imageResId = option.resId,
                            photoUri = null,
                        )
                        updateCompanion(changed)
                        goBack(AppRoute.Personality)
                    },
                )
                AppRoute.MaleAvatars -> AvatarSelectionScreen(
                    title = "Male Avatars",
                    options = maleAvatarOptions(),
                    selected = (companionBuilderDraft ?: activeCompanion).avatarType,
                    onBack = { goBack(AppRoute.Personality) },
                    onSelected = { option ->
                        val target = companionBuilderDraft ?: activeCompanion
                        val changed = target.copy(
                            gender = "Male",
                            voice = voiceForGender("Male", target.voice),
                            avatarType = option.name,
                            imageResName = option.resName,
                            imageResId = option.resId,
                            photoUri = null,
                        )
                        updateCompanion(changed)
                        goBack(AppRoute.Personality)
                    },
                )
                AppRoute.Journal -> JournalScreen(
                    title = journalTitle,
                    body = journalBody,
                    mood = selectedJournalMood,
                    storage = defaultJournalStorage,
                    askEveryTime = askStorageEveryTime,
                    videoJournalState = videoJournalState,
                    onTitleChange = { journalTitle = it },
                    onBodyChange = { journalBody = it },
                    onMoodChange = { selectedJournalMood = it },
                    onReflect = { navigateTo(AppRoute.Reflection) },
                    onStorageSheet = { showJournalStorageSheet = true },
                    onStorageChange = { defaultJournalStorage = it },
                    onVideoStateChange = { videoJournalState = it },
                    onViewEntries = { navigateTo(AppRoute.SavedJournalEntries) },
                )
                AppRoute.SavedJournalEntries -> SavedJournalEntriesScreen(storage = defaultJournalStorage)
                AppRoute.Reflection -> ReflectionScreen(onBack = { goBack(AppRoute.Wellness) }, onSaveToJournal = { navigateTo(AppRoute.Journal) })
                AppRoute.Grounding -> GroundingScreen(onBack = { goBack(AppRoute.Wellness) })
                AppRoute.CheckIn -> CheckInScreen(onBack = { goBack(AppRoute.Wellness) }, onSaveToJournal = { navigateTo(AppRoute.Journal) })
                AppRoute.Affirmations -> AffirmationsScreen(onBack = { goBack(AppRoute.Wellness) })
                AppRoute.Breathing -> BreathingScreen(onBack = { goBack(AppRoute.Wellness) })
                AppRoute.Fitness -> FitnessScreen(
                    onBack = { goBack(AppRoute.Wellness) },
                    activeWorkoutTitle = fitnessActiveWorkoutTitle,
                    timerRunning = fitnessTimerRunning,
                    sessionSeconds = fitnessSessionSeconds,
                    minutesToday = fitnessMinutesToday,
                    workoutsCompleted = fitnessWorkoutsCompleted,
                    streakDays = fitnessStreakDays,
                    goalMinutes = fitnessGoalMinutes,
                    totalSeconds = fitnessTotalSeconds,
                    lastWorkoutTitle = fitnessLastWorkoutTitle,
                    onGoalChange = { fitnessGoalMinutes = it },
                    onSelectWorkout = {
                        fitnessActiveWorkoutTitle = it
                        fitnessSessionSeconds = 0
                        fitnessTimerRunning = false
                    },
                    onStartActive = { fitnessTimerRunning = true },
                    onAddFiveMinutes = { fitnessMinutesToday += 5 },
                    onReset = {
                        fitnessTimerRunning = false
                        fitnessSessionSeconds = 0
                        fitnessMinutesToday = 0
                        fitnessWorkoutsCompleted = 0
                        fitnessStreakDays = 0
                        fitnessTotalSeconds = 0
                        fitnessLastWorkoutTitle = "No workout completed yet"
                    },
                    onCompleteWorkout = { plannedMinutes ->
                        fitnessTimerRunning = false
                        val completedSeconds = if (fitnessSessionSeconds > 0) fitnessSessionSeconds else plannedMinutes * 60
                        fitnessTotalSeconds += completedSeconds
                        fitnessMinutesToday += maxOf(1, (completedSeconds + 59) / 60)
                        fitnessWorkoutsCompleted += 1
                        fitnessStreakDays = maxOf(fitnessStreakDays, 1)
                        fitnessLastWorkoutTitle = fitnessActiveWorkoutTitle
                        fitnessSessionSeconds = 0
                    },
                    onProgress = { navigateTo(AppRoute.FitnessProgress) },
                )
                AppRoute.FitnessProgress -> FitnessProgressScreen(
                    onBack = { goBack(AppRoute.Fitness) },
                    activeWorkoutTitle = fitnessActiveWorkoutTitle,
                    timerRunning = fitnessTimerRunning,
                    sessionSeconds = fitnessSessionSeconds,
                    minutesToday = fitnessMinutesToday,
                    workoutsCompleted = fitnessWorkoutsCompleted,
                    streakDays = fitnessStreakDays,
                    goalMinutes = fitnessGoalMinutes,
                    totalSeconds = fitnessTotalSeconds,
                    lastWorkoutTitle = fitnessLastWorkoutTitle,
                )
                AppRoute.Memory -> MemoryScreen()
                AppRoute.Preferences -> PreferencesScreen(
                    companion = activeCompanion,
                    defaultJournalStorage = defaultJournalStorage,
                    defaultChatStorage = defaultChatStorage,
                    askStorageEveryTime = askStorageEveryTime,
                    hasAcceptedDisclaimer = hasAcceptedDisclaimer,
                    hideDisclaimerOnLaunch = hideDisclaimerOnLaunch,
                    showDisclaimerOnLaunch = showDisclaimerOnLaunch,
                    onStorageChange = { defaultJournalStorage = it },
                    onChatStorageChange = { defaultChatStorage = it },
                    onAskEveryTimeChange = { askStorageEveryTime = it },
                    onHideDisclaimerChange = { hideDisclaimerOnLaunch = it },
                    onShowDisclaimerChange = { showDisclaimerOnLaunch = it },
                    onNavigate = { navigateTo(it) },
                    onProfileInfo = { navigateTo(AppRoute.ProfileInfo) },
                    onSignOut = { signOutAndReturnToLogin() },
                )
                AppRoute.ProfileInfo -> ProfileInfoScreen(
                    profilePhotoUri = profilePhotoUri,
                    displayName = displayName,
                    email = currentFirebaseEmail() ?: "Not available",
                    birthday = birthday,
                    preferredName = preferredName,
                    pronouns = pronouns,
                    location = location,
                    aboutMe = aboutMe,
                    wellnessPreferences = wellnessPreferences,
                    defaultCompanion = activeCompanion.name,
                    notificationPreferences = notificationPreferences,
                    cloudSyncPreference = cloudSyncPreference,
                    onProfilePhotoChange = { profilePhotoUri = it },
                    onDisplayNameChange = { displayName = it },
                    onBirthdayChange = { birthday = it },
                    onPreferredNameChange = { preferredName = it },
                    onPronounsChange = { pronouns = it },
                    onLocationChange = { location = it },
                    onAboutMeChange = { aboutMe = it },
                    onWellnessPreferencesChange = { wellnessPreferences = it },
                    onNotificationPreferencesChange = { notificationPreferences = it },
                    onCloudSyncPreferenceChange = { cloudSyncPreference = it },
                    onSave = { goBack(AppRoute.Preferences) },
                )
                AppRoute.AccountSettings -> AccountSettingsScreen(onSignOut = { signOutAndReturnToLogin() })
                AppRoute.VoiceLiveSettings -> VoiceLiveSettingsScreen(
                    companion = activeCompanion,
                    companions = companions,
                    onSelectCompanion = { id -> setActiveCompanion(id) },
                    onVoiceSelected = { voice ->
                        val index = companions.indexOfFirst { it.id == activeCompanion.id }
                        if (index >= 0) companions[index] = companions[index].copy(voice = voice)
                    },
                    onLiveSettings = { navigateTo(AppRoute.LiveCompanionCall) },
                )
                AppRoute.JournalStorageSettings -> JournalStorageSettingsScreen(
                    defaultJournalStorage = defaultJournalStorage,
                    askStorageEveryTime = askStorageEveryTime,
                    onStorageChange = { defaultJournalStorage = it },
                    onAskEveryTimeChange = { askStorageEveryTime = it },
                )
                AppRoute.PrivacySettings -> PrivacySettingsScreen()
                AppRoute.NotificationSettings -> NotificationSettingsScreen(companion = activeCompanion)
                AppRoute.AppearanceSettings -> AppearanceSettingsScreen(
                    companion = activeCompanion,
                    mode = appearanceMode,
                    palette = appearancePalette,
                    onSaveAppearance = { savedMode, savedPalette ->
                        appearanceMode = savedMode
                        appearancePalette = savedPalette
                    },
                )
                AppRoute.Disclaimer -> DisclaimerScreen(
                    hasAcceptedDisclaimer = hasAcceptedDisclaimer,
                    hideDisclaimerOnLaunch = hideDisclaimerOnLaunch,
                    onAcceptedChange = { hasAcceptedDisclaimer = it },
                    onHideChange = { hideDisclaimerOnLaunch = it },
                    onContinue = { navigateTo(AppRoute.Preferences) },
                    onCrisis = { navigateTo(AppRoute.CrisisResources) },
                    onBack = { goBack(AppRoute.Wellness) },
                )
                AppRoute.CrisisResources -> CrisisResourcesScreen(onBack = { goBack(AppRoute.Wellness) })
                AppRoute.Splash, AppRoute.Welcome, AppRoute.Login -> HomeDashboardScreen(
                    companion = activeCompanion,
                    companions = companions,
                    selectedMood = selectedMood,
                    onMoodSelected = { selectedMood = it },
                    onNavigate = { navigateTo(it) },
                    onSelectCompanion = { activeCompanionId = it },
                    onCreateCompanion = { openCreateCompanion() },
                )
            }
            },
        )

        if (showJournalStorageSheet) {
            StorageChoiceSheet(
                selected = defaultJournalStorage,
                onSelected = {
                    defaultJournalStorage = it
                    showJournalStorageSheet = false
                },
                onDismiss = { showJournalStorageSheet = false },
            )
        }
    }
}

@Composable
private fun AppShell(
    route: AppRoute,
    activeCompanion: CompanionProfile,
    profilePhotoUri: String?,
    onNavigate: (AppRoute) -> Unit,
    onProfileInfo: () -> Unit,
    onSignOut: () -> Unit,
    onJournalStorage: () -> Unit,
    onJournalStorageSelected: (JournalStorage) -> Unit,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SoftDrawer(
                currentRoute = route,
                profilePhotoUri = profilePhotoUri,
                onProfileInfo = {
                    scope.launch { drawerState.close() }
                    onProfileInfo()
                },
                onNavigate = {
                    scope.launch { drawerState.close() }
                    onNavigate(it)
                },
            )
        },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    route = route,
                    companion = activeCompanion,
                    profilePhotoUri = profilePhotoUri,
                    onMenu = { scope.launch { drawerState.open() } },
                    onNavigate = onNavigate,
                    onProfileInfo = onProfileInfo,
                    onSignOut = onSignOut,
                    onJournalStorage = onJournalStorage,
                    onJournalStorageSelected = onJournalStorageSelected,
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SoftDrawer(
    currentRoute: AppRoute,
    profilePhotoUri: String?,
    onProfileInfo: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    val items = listOf(
        "Home" to AppRoute.Home,
        "Chat" to AppRoute.Chat,
        "Companion Builder" to AppRoute.Personality,
        "Companion State" to AppRoute.CompanionState,
        "Crisis Resources" to AppRoute.CrisisResources,
        "Disclaimer" to AppRoute.Disclaimer,
        "Journal" to AppRoute.Journal,
        "Wellness" to AppRoute.Wellness,
    )

    ModalDrawerSheet(
        drawerContainerColor = CreamWhite.copy(alpha = 0.94f),
        drawerContentColor = TextDark,
        modifier = Modifier.width(318.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BackgroundTop, BackgroundMiddle, BackgroundBottom)))
                .padding(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfilePhotoButton(size = 58.dp, photoUri = profilePhotoUri, onClick = onProfileInfo)
                    Column {
                        Text("LUNAKAI Wellness Companion", color = TextDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Tap + to update your profile", color = TextMuted, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.72f), modifier = Modifier.padding(vertical = 10.dp))
                items.forEach { (label, route) ->
                    NavigationDrawerItem(
                        label = { Text(label, fontWeight = FontWeight.SemiBold) },
                        selected = currentRoute == route,
                        onClick = { onNavigate(route) },
                        icon = { SoftMenuIcon(label) },
                        shape = LargeShape,
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumTopBar(
    route: AppRoute,
    companion: CompanionProfile,
    profilePhotoUri: String?,
    onMenu: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onProfileInfo: () -> Unit,
    onSignOut: () -> Unit,
    onJournalStorage: () -> Unit,
    onJournalStorageSelected: (JournalStorage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val hideOverflow = route in listOf(
        AppRoute.Preferences,
        AppRoute.ProfileInfo,
        AppRoute.AccountSettings,
        AppRoute.VoiceLiveSettings,
        AppRoute.JournalStorageSettings,
        AppRoute.PrivacySettings,
        AppRoute.NotificationSettings,
        AppRoute.AppearanceSettings,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BackgroundTop, BackgroundMiddle)))
            .padding(top = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconCircleButton(R.drawable.ic_menu, "Open menu", onMenu)
            if (route == AppRoute.Home) {
                Spacer(Modifier.weight(1f))
                ProfilePhotoButton(size = 46.dp, photoUri = profilePhotoUri, onClick = onProfileInfo)
                IconCircleButton(R.drawable.ic_settings, "Settings", onClick = { onNavigate(AppRoute.Preferences) })
                return@Row
            }
            if (route == AppRoute.TextChat || route == AppRoute.LiveCompanionCall) {
                CompanionAvatar(size = 42.dp, label = companion.name.take(2), glow = false, photoUri = companion.photoUri, imageResId = companion.imageResId)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (route == AppRoute.TextChat) companion.name else route.title,
                    color = TextDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (route == AppRoute.TextChat) "Texting you" else route.subtitle,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!hideOverflow) {
                Box {
                    IconCircleButton(R.drawable.ic_more_vert, "More options", onClick = { expanded = true })
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(CardDark)) {
                        overflowItems(route).forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(menuOptionIcon(item)),
                                        contentDescription = item,
                                        tint = DeepRose,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    when (item) {
                                        "Change Companion", "Manage My Companions" -> onNavigate(AppRoute.Companions)
                                        "Open Settings" -> onNavigate(AppRoute.Preferences)
                                        "Voice Settings" -> onNavigate(AppRoute.VoiceLiveSettings)
                                        "Privacy Settings" -> onNavigate(AppRoute.PrivacySettings)
                                        "View Disclaimer" -> onNavigate(AppRoute.Disclaimer)
                                        "Video", "Video Chat" -> onNavigate(AppRoute.LiveCompanionCall)
                                        "Phone" -> onNavigate(AppRoute.LiveCompanionCall)
                                        "Clear Chat" -> scope.launch { clearCloudChatMessages(companion.id) }
                                        "Save Chat to Journal" -> onNavigate(AppRoute.Journal)
                                        "View Journal Entries" -> onNavigate(AppRoute.SavedJournalEntries)
                                        "Save Call Notes to Journal" -> onNavigate(AppRoute.Journal)
                                        "Companion Appearance" -> onNavigate(AppRoute.AppearanceSettings)
                                        "End Session" -> onNavigate(AppRoute.Home)
                                        "Storage Location" -> onJournalStorage()
                                        "Save on Device" -> onJournalStorageSelected(JournalStorage.Device)
                                        "Save to Cloud" -> onJournalStorageSelected(JournalStorage.Cloud)
                                        "Save to Both" -> onJournalStorageSelected(JournalStorage.Both)
                                        "Create Companion" -> onNavigate(AppRoute.Personality)
                                        "View Crisis Resources" -> onNavigate(AppRoute.CrisisResources)
                                        "Progress Tracker" -> onNavigate(AppRoute.FitnessProgress)
                                        "Open Wellness" -> onNavigate(AppRoute.Wellness)
                                        "Sign Out" -> onSignOut()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun overflowItems(route: AppRoute): List<String> {
    return when (route) {
        AppRoute.Home -> listOf("Change Companion", "Open Settings", "View Disclaimer", "Sign Out")
        AppRoute.Chat -> listOf("Open Settings", "View Disclaimer", "Sign Out")
        AppRoute.TextChat -> listOf("Video Chat", "Phone", "Change Companion", "Open Settings", "Clear Chat", "Save Chat to Journal")
        AppRoute.LiveCompanionCall -> listOf("Change Companion", "Voice Settings", "Companion Appearance", "Save Call Notes to Journal", "Turn Captions On/Off", "Privacy Settings", "End Session")
        AppRoute.Journal -> listOf("Storage Location", "Save on Device", "Save to Cloud", "Save to Both", "View Journal Entries", "Export Journal", "Clear Draft", "Privacy Settings")
        AppRoute.Fitness -> listOf("Progress Tracker", "Open Wellness")
        AppRoute.FitnessProgress -> listOf("Open Wellness")
        AppRoute.Companions -> listOf("Create Companion", "Sort Companions", "Show Active Only", "Delete Companion")
        AppRoute.Preferences -> listOf("Reset Settings", "Export Data", "Delete Account Data", "View Disclaimer", "View Crisis Resources")
        else -> listOf("Open Settings", "View Disclaimer", "Sign Out")
    }
}

private fun menuOptionIcon(item: String): Int {
    return when {
        item == "Video" || item == "Video Chat" -> R.drawable.ic_video
        item == "Phone" || item.contains("Live", ignoreCase = true) || item.contains("End", ignoreCase = true) -> R.drawable.ic_call_end
        item.contains("Companion", ignoreCase = true) -> R.drawable.ic_profile
        item.contains("Voice", ignoreCase = true) -> R.drawable.ic_speaker
        item.contains("Memory", ignoreCase = true) -> R.drawable.ic_memory
        item.contains("Journal", ignoreCase = true) -> R.drawable.ic_journal
        item.contains("Progress", ignoreCase = true) || item.contains("Wellness", ignoreCase = true) -> R.drawable.ic_heart
        item.contains("Privacy", ignoreCase = true) || item.contains("Settings", ignoreCase = true) -> R.drawable.ic_settings
        item.contains("Crisis", ignoreCase = true) || item.contains("Disclaimer", ignoreCase = true) -> R.drawable.ic_warning
        else -> R.drawable.ic_sparkle
    }
}

private fun currentFirebaseEmail(): String? {
    return runCatching {
        FirebaseAuth.getInstance().currentUser?.email
    }.getOrNull()
}

private fun firebaseCurrentUserExists(): Boolean {
    return runCatching { FirebaseAuth.getInstance().currentUser != null }.getOrDefault(false)
}

private fun firebaseSignOut() {
    runCatching { FirebaseAuth.getInstance().signOut() }
        .onFailure { Log.w("FancieAuth", "Firebase sign out failed", it) }
}

private suspend fun firebaseSignIn(email: String, password: String) {
    FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), password).awaitTask()
}

private suspend fun firebaseCreateAccount(email: String, password: String) {
    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.trim(), password).awaitTask()
}

private suspend fun firebaseSendPasswordReset(email: String) {
    FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim()).awaitTask()
}

private fun characterModeOptions() = listOf(
    "Best Friend",
    "Romantic Partner",
    "The Opps",
    "Assistant",
    "Wellness Coach",
    "Athletic Partner",
    "Creative Muse",
    "Practice Partner",
    "Customize Companion",
)

private fun supportFocusOptions() = listOf(
    "Stress",
    "Self-worth",
    "Confidence",
    "Grounding",
    "Journaling",
    "Reflection",
    "Relationships",
    "Creativity",
    "Fitness",
)

private fun femaleVoiceOptions() = listOf(
    "Soft Female",
    "Warm Female",
    "Confident Female",
    "Sultry Calm Female",
    "Bright Female",
    "Deep Feminine",
    "Gentle Whisper Female",
)

private fun maleVoiceOptions() = listOf(
    "Calm Male",
    "Deep Male",
    "Protective Male",
    "Smooth Male",
    "Warm Male",
    "Motivational Male",
    "Soft-Spoken Male",
)

private fun neutralVoiceOptions() = listOf("Neutral Calm", "Neutral Bright", "Custom Voice Later")

private fun voiceOptionsFor(gender: String) = when (gender) {
    "Female" -> femaleVoiceOptions()
    "Male" -> maleVoiceOptions()
    else -> femaleVoiceOptions() + maleVoiceOptions() + neutralVoiceOptions()
}

private fun voiceForGender(gender: String, currentVoice: String): String {
    val allowed = voiceOptionsFor(gender)
    return currentVoice.takeIf { it in allowed } ?: allowed.firstOrNull() ?: currentVoice
}

private fun wakeAssistantIdFor(companion: CompanionProfile): String {
    return if (companion.id == "kai") "kai" else "luna"
}

private fun wakeAssistantName(id: String): String = if (id == "kai") "Kai" else "Luna"

private fun wakePhraseFor(id: String): String = "Hey ${wakeAssistantName(id)}"

private fun normalizeWakePhrase(text: String): String {
    return text
        .lowercase()
        .filter { it.isLetterOrDigit() || it.isWhitespace() }
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun spokenWakePhraseMatches(spokenText: String, targetPhrase: String): Boolean {
    val spoken = normalizeWakePhrase(spokenText)
    val target = normalizeWakePhrase(targetPhrase)
    return spoken == target || spoken.contains(target)
}

private fun calculateCompanionExperience(
    companion: CompanionProfile,
    characterModeOverride: String = companion.characterMode,
    supportFocusOverride: String = companion.supportFocus.firstOrNull() ?: "Stress",
    urgencyOverride: String = "Calculated from character preferences",
): CompanionExperienceProfile {
    val traits = companion.personalityTraits + companion.personalityTags
    val protective = traits.any { it.equals("Protective", ignoreCase = true) }
    val motivational = companion.communicationStyles.any { it.equals("Motivational", ignoreCase = true) } ||
        companion.communicationStyle.equals("Motivational", ignoreCase = true)
    val direct = companion.communicationStyles.any { it.equals("Direct", ignoreCase = true) } ||
        companion.communicationStyle.equals("Direct", ignoreCase = true)
    val playful = traits.any { it.equals("Playful", ignoreCase = true) || it.equals("Funny", ignoreCase = true) }
    val baseScore = when (characterModeOverride) {
        "The Opps" -> 82
        "Athletic Partner" -> 76
        "Assistant" -> 64
        "Wellness Coach" -> 58
        "Romantic Partner" -> 50
        "Best Friend" -> 46
        else -> 52
    }
    val supportBoost = when (supportFocusOverride) {
        "Fitness", "Confidence" -> 10
        "Stress", "Grounding" -> -8
        "Self-worth", "Reflection", "Journaling" -> -4
        else -> 0
    }
    val traitBoost = listOf(
        if (protective) 8 else 0,
        if (motivational) 7 else 0,
        if (direct) 5 else 0,
        if (playful) -3 else 0,
    ).sum()
    val calculatedScore = (baseScore + supportBoost + traitBoost).coerceIn(18, 95)
    val urgencyScore = when (urgencyOverride) {
        "Soft only" -> minOf(calculatedScore, 34)
        "Normal" -> calculatedScore.coerceIn(40, 68)
        "High urgency" -> maxOf(calculatedScore, 78)
        else -> calculatedScore
    }
    val urgency = when {
        urgencyScore >= 76 -> "High, action-focused"
        urgencyScore >= 55 -> "Balanced and attentive"
        else -> "Soft and gentle"
    }
    val tone = when (characterModeOverride) {
        "The Opps" -> "Playful push with a little edge"
        "Romantic Partner" -> "Warm, affectionate, emotionally close"
        "Assistant" -> "Clear, organized, and practical"
        "Athletic Partner" -> "Energizing, accountable, body-positive"
        "Wellness Coach" -> "Grounded, reflective, supportive"
        "Creative Muse" -> "Imaginative, encouraging, idea-forward"
        else -> if (direct) "Honest and supportive" else "Gentle and emotionally present"
    }
    val checkInStyle = when (supportFocusOverride) {
        "Fitness" -> "Movement nudges and workout accountability"
        "Stress", "Grounding" -> "Calm reminders and nervous-system resets"
        "Journaling", "Reflection" -> "Thoughtful prompts and emotional pattern checks"
        "Confidence", "Self-worth" -> "Encouragement, progress reminders, and self-belief"
        "Relationships" -> "Boundary-aware reflection and communication prompts"
        "Creativity" -> "Idea sparks and momentum nudges"
        else -> "Supportive check-ins"
    }
    val notificationStrategy = "$urgency urgency - $checkInStyle"
    val sampleNotification = when (characterModeOverride) {
        "The Opps" -> "Hey, you said you wanted better for yourself. Tiny step. Now."
        "Romantic Partner" -> "Hey love, pause with me for a second. How are you really feeling?"
        "Assistant" -> "Quick check-in: want to log your mood, movement, or next priority?"
        "Athletic Partner" -> "Ready for a short reset? We can move for ${if (supportFocusOverride == "Fitness") "10 minutes" else "one small round"}."
        else -> "I'm here with you. Want a gentle check-in or a small next step?"
    }
    val chatReply = when (characterModeOverride) {
        "The Opps" -> "Okay, I hear you. But we are not letting doubt run the room. Name the smallest next move and I will stay with you through it."
        "Romantic Partner" -> "I hear you, love. Come closer to the truth of it with me. Is this asking for comfort, clarity, or reassurance?"
        "Assistant" -> "I hear you. Let's sort this into one clear next step, one thing to pause, and one thing you do not need to carry right now."
        "Athletic Partner" -> "I hear you. Breathe first, then we choose one strong step. Do you need grounding, movement, or a quick plan?"
        else -> "I hear you. You do not have to carry everything at once. Let's choose the next gentle step."
    }
    val stateMessage = when (characterModeOverride) {
        "The Opps" -> "I'm in playful push mode. I will challenge the doubt without attacking you."
        "Romantic Partner" -> "I'm tuned into closeness and care. We can move gently and make room for what you feel."
        "Assistant" -> "I'm organized and steady with you. We can make things clearer, one step at a time."
        "Athletic Partner" -> "I'm focused on momentum and accountability. We can move, reset, and keep it realistic."
        else -> "I'm tuned into softness today. We can move slowly and make space for what you feel."
    }
    return CompanionExperienceProfile(
        characterMode = characterModeOverride,
        primarySupportFocus = supportFocusOverride,
        tone = tone,
        urgency = urgency,
        urgencyScore = urgencyScore,
        checkInStyle = checkInStyle,
        notificationStrategy = notificationStrategy,
        sampleNotification = sampleNotification,
        chatReply = chatReply,
        stateMessage = stateMessage,
    )
}

private fun userMessageMentionsBdsm(message: String): Boolean {
    val normalized = message.lowercase()
    return listOf(
        "bdsm",
        "dominance",
        "submission",
        "dominant",
        "submissive",
        "safeword",
        "safe word",
        "scene",
        "roleplay",
        "fantasy",
        "dom",
        "sub",
    ).any { it in normalized }
}

private fun nextBdsmSetupResponse(
    companion: CompanionProfile,
    currentState: BdsmSessionState,
    userMessage: String,
): Pair<String, BdsmSessionState>? {
    val settings = companion.bdsmIdentitySettings
    if (!settings.enabled || !settings.adultConsentConfirmed) return null
    val message = userMessage.trim()
    val lower = message.lowercase()
    val stopWord = currentState.stopWord.ifBlank { settings.defaultStopWord }
    val pauseWord = currentState.pauseWord.ifBlank { settings.defaultPauseWord }

    if (currentState.isActive && stopWord.isNotBlank() && lower.contains(stopWord.lowercase())) {
        return "I'm stopping now. You're safe here. Do you want grounding, reassurance, or to return to regular chat?" to
            currentState.copy(isActive = false, setupComplete = false, setupStep = 0)
    }
    if (currentState.isActive && pauseWord.isNotBlank() && lower.contains(pauseWord.lowercase())) {
        return "Paused. Do you want me to soften the tone, change the scene, or stop completely?" to currentState
    }

    val shouldHandleSetup = !currentState.setupComplete && (currentState.isActive || userMessageMentionsBdsm(message))
    if (!shouldHandleSetup) return null

    return when (currentState.setupStep) {
        0 -> "We can explore that in a consent-based way. Before we start, what role would you like me to play, and what role would you like to play?" to
            currentState.copy(isActive = true, setupStep = 1)
        1 -> "What are your hard limits and soft limits for this scene?" to
            currentState.copy(companionRole = message, userRole = "User-defined", setupStep = 2)
        2 -> "Do you want to use ${settings.defaultStopWord} as the stop word and ${settings.defaultPauseWord} as the pause word, or would you like to choose your own?" to
            currentState.copy(hardLimits = message, setupStep = 3)
        3 -> "What tone do you want me to use: soft, playful, firm, nurturing, or intense but safe?" to
            currentState.copy(stopWord = settings.defaultStopWord, pauseWord = settings.defaultPauseWord, setupStep = 4)
        else -> "Got it. I'll stay within those boundaries. You can pause with ${currentState.pauseWord} or stop completely with ${currentState.stopWord} at any time. What opening direction feels right?" to
            currentState.copy(tone = message, setupComplete = true, isActive = true, setupStep = 5)
    }
}

@Composable
private fun SplashScreen() {
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CompanionAvatar(size = 132.dp, label = "FA", glow = true)
            Spacer(Modifier.height(24.dp))
            Text("LUNAKAI Wellness Companion", color = TextDark, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                "Your soft place to think, feel, and be understood.",
                color = TextMuted,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text("Preparing your companion...", color = DeepRose, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 28.dp))
        }
    }
}

@Composable
private fun WelcomeScreen(onGetStarted: () -> Unit, onLogin: () -> Unit) {
    GradientBackground {
        ScreenScroll {
            Text("AI WELLNESS COMPANION", color = DeepRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Meet the companion that grows with you.", color = TextDark, fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
            Text("Chat, reflect, journal, and build a supportive AI presence that remembers what matters to you.", color = TextMuted, fontSize = 16.sp)
            GlassCard(padding = 0.dp, background = CardAccent.copy(alpha = 0.72f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                        .clip(LargeShape)
                        .border(1.dp, AccentPink.copy(alpha = 0.24f), LargeShape),
                ) {
                    Image(
                        painter = painterResource(R.drawable.welcome_luna_kai),
                        contentDescription = "Luna and Kai companion portraits",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Black.copy(alpha = 0.42f),
                                    ),
                                ),
                            ),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                SoftFeatureChip("Private reflections")
                SoftFeatureChip("Emotional check-ins")
                SoftFeatureChip("Personalized companion")
            }
            PrimaryGradientButton("Get Started", onClick = onGetStarted)
            SecondarySoftButton("I already have an account", onClick = onLogin)
        }
    }
}

@Composable
private fun LoginScreen(
    defaultChatStorage: ChatStorage,
    onChatStorageChange: (ChatStorage) -> Unit,
    onSignedIn: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(currentFirebaseEmail().orEmpty()) }
    var password by remember { mutableStateOf("") }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun validateCredentials(): Boolean {
        authMessage = when {
            email.isBlank() -> "Enter your email to continue."
            password.length < 6 -> "Password must be at least 6 characters."
            else -> null
        }
        return authMessage == null
    }

    fun runAuth(actionName: String, action: suspend () -> Unit) {
        if (!validateCredentials()) return
        loading = true
        authMessage = "$actionName..."
        scope.launch {
            runCatching { action() }
                .onSuccess {
                    authMessage = "Signed in. Your private companion space is ready."
                    onSignedIn()
                }
                .onFailure { error ->
                    Log.w("FancieAuth", "$actionName failed", error)
                    authMessage = error.message ?: "$actionName failed. Check Firebase Authentication setup."
                }
            loading = false
        }
    }

    GradientBackground {
        ScreenScroll {
            Text("LUNAKAI Wellness Companion", color = TextDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Welcome back, love.", color = TextDark, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text("Your companion space is waiting for you.", color = TextMuted, fontSize = 16.sp)
            GlassCard {
                RoundedInputField(value = email, onValueChange = { email = it }, label = "Email")
                Spacer(Modifier.height(12.dp))
                RoundedInputField(value = password, onValueChange = { password = it }, label = "Password")
                Spacer(Modifier.height(18.dp))
                SoftDropdown(
                    label = "Chat message saving",
                    selected = defaultChatStorage.label,
                    options = ChatStorage.entries.map { it.label },
                    onSelected = { label ->
                        ChatStorage.entries.firstOrNull { it.label == label }?.let(onChatStorageChange)
                    },
                )
                Text(defaultChatStorage.body, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(12.dp))
                authMessage?.let {
                    Text(it, color = if (it.contains("failed", ignoreCase = true)) WarningPeach else TextMuted, fontSize = 13.sp, lineHeight = 17.sp)
                }
                PrimaryGradientButton(
                    if (loading) "Working..." else "Sign In",
                    onClick = {
                        if (!loading) runAuth("Signing in") { firebaseSignIn(email, password) }
                    },
                )
                SecondarySoftButton(
                    if (loading) "Please wait..." else "Create Account",
                    onClick = {
                        if (!loading) runAuth("Creating account") { firebaseCreateAccount(email, password) }
                    },
                )
                TextButton(
                    onClick = {
                        if (email.isBlank()) {
                            authMessage = "Enter your email first, then tap forgot password."
                        } else {
                            loading = true
                            authMessage = "Sending reset email..."
                            scope.launch {
                                runCatching { firebaseSendPasswordReset(email) }
                                    .onSuccess { authMessage = "Password reset email sent." }
                                    .onFailure { error ->
                                        Log.w("FancieAuth", "Password reset failed", error)
                                        authMessage = error.message ?: "Password reset failed."
                                    }
                                loading = false
                            }
                        }
                    },
                ) { Text("Forgot password?", color = DeepRose) }
            }
            Text("Your thoughts stay in your private companion space.", color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun chatSummaryMillis(value: Any?): Long = when (value) {
    is Long -> value
    is Int -> value.toLong()
    is Double -> value.toLong()
    is com.google.firebase.Timestamp -> value.toDate().time
    else -> 0L
}

private fun chatPreviewTime(updatedAt: Long, fallback: String): String {
    if (updatedAt <= 0L) return fallback
    val elapsedSeconds = ((System.currentTimeMillis() - updatedAt) / 1000).coerceAtLeast(0)
    return when {
        elapsedSeconds < 60 -> "Now"
        elapsedSeconds < 3600 -> "${elapsedSeconds / 60}m"
        elapsedSeconds < 86400 -> "${elapsedSeconds / 3600}h"
        else -> "${elapsedSeconds / 86400}d"
    }
}
@Composable
private fun HomeDashboardScreen(
    companion: CompanionProfile,
    companions: List<CompanionProfile>,
    selectedMood: String,
    onMoodSelected: (String) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSelectCompanion: (String) -> Unit,
    onCreateCompanion: () -> Unit,
) {
    val orderedHomeCompanions = orderedCompanions(companions)
    var chatSummaries by remember { mutableStateOf<Map<String, ChatSummaryPreview>>(emptyMap()) }
    DisposableEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.w("FancieFirestore", "User must be signed in before saving chat.")
            onDispose { }
        } else {
            val registration = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("chats")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("FancieFirestore", "Could not load chat summaries", error)
                        return@addSnapshotListener
                    }
                    chatSummaries = snapshot?.documents.orEmpty().mapNotNull { document ->
                        val companionId = document.getString("companionId") ?: return@mapNotNull null
                        if (document.id != stableChatIdForCompanion(companionId)) return@mapNotNull null
                        val lastMessage = document.getString("lastMessage").orEmpty()
                        companionId to ChatSummaryPreview(
                            companionId = companionId,
                            lastMessage = lastMessage,
                            updatedAt = chatSummaryMillis(document.get("updatedAt")),
                        )
                    }.toMap()
                }
            onDispose { registration.remove() }
        }
    }
    val chatPreviewCompanions = orderedHomeCompanions
        .sortedWith(compareByDescending<CompanionProfile> { chatSummaries[it.id]?.updatedAt ?: 0L }.thenBy { it.name })
        .take(8)
    val chatPreviews = chatPreviewCompanions.map { savedCompanion ->
        val summary = chatSummaries[savedCompanion.id]
        val fallbackPreview = if (savedCompanion.id == "luna") {
            FRESH_CHAT_GREETING
        } else if (savedCompanion.id == "kai") {
            FRESH_CHAT_GREETING
        } else {
            FRESH_CHAT_GREETING
        }
        val fallbackTime = if (savedCompanion.id == "luna" || savedCompanion.id == "kai") "Ready" else savedCompanion.lastUsedDate
        ChatPreview(
            companionId = savedCompanion.id,
            companionName = savedCompanion.name,
            preview = summary?.lastMessage?.takeIf { it.isNotBlank() } ?: fallbackPreview,
            time = chatPreviewTime(summary?.updatedAt ?: 0L, fallbackTime),
            unreadCount = 0,
            photoUri = savedCompanion.photoUri,
            imageResId = savedCompanion.imageResId,
        )
    }
    val visibleSlots: List<CompanionProfile?> = orderedHomeCompanions.take(8).map { it } + List(maxOf(0, 8 - orderedHomeCompanions.size)) { null }
    val greeting = timeBasedGreeting("Fancie")

    GradientBackground {
        ScreenScroll {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(greeting, color = TextDark, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("What's on your mind?", color = TextMuted, fontSize = 17.sp)
            }

            SectionHeader("My Companions")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                visibleSlots.forEach { savedCompanion ->
                    SavedCompanionSlot(
                        companion = savedCompanion,
                        active = savedCompanion?.id == companion.id,
                        onClick = {
                            if (savedCompanion == null) {
                                onCreateCompanion()
                            } else {
                                onSelectCompanion(savedCompanion.id)
                                onNavigate(AppRoute.TextChat)
                            }
                        },
                    )
                }
            }

            SectionHeader("Chats")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                chatPreviews.forEach { preview ->
                    ChatPreviewRow(
                        preview = preview,
                        active = preview.companionId == companion.id,
                        onClick = {
                            onSelectCompanion(preview.companionId)
                            onNavigate(AppRoute.TextChat)
                        },
                    )
                }
            }
        }
    }
}

private fun timeBasedGreeting(userName: String?): String {
    val name = userName?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good day"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    return "$greeting$name"
}

@Composable
private fun HomeEntryCard(
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .shadow(8.dp, LargeShape)
            .clip(LargeShape)
            .background(color.copy(alpha = 0.30f))
            .border(1.dp, Color.White.copy(alpha = 0.58f), LargeShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SoftIcon(title, color)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextDark, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, color = TextMuted, fontSize = 14.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(">", color = DeepRose, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SavedCompanionSlot(
    companion: CompanionProfile?,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(86.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (companion == null) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(CardDark.copy(alpha = 0.92f))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = DeepRose, fontSize = 30.sp, fontWeight = FontWeight.Light)
            }
            Text("Create", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        } else {
            Box {
                CompanionAvatar(size = 74.dp, label = companion.name.take(2), glow = active, photoUri = companion.photoUri, imageResId = companion.imageResId)
                if (active) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                            .border(2.dp, CreamWhite, CircleShape),
                    )
                }
            }
            Text(companion.name, color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ChatPreviewRow(preview: ChatPreview, active: Boolean, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.clickable(onClick = onClick),
        padding = 14.dp,
        background = if (active) CardAccent.copy(alpha = 0.96f) else CardDark.copy(alpha = 0.92f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompanionAvatar(size = 54.dp, label = preview.companionName.take(2), glow = active, photoUri = preview.photoUri, imageResId = preview.imageResId)
            Column(modifier = Modifier.weight(1f)) {
                Text(preview.companionName, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(preview.preview, color = TextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(preview.time, color = TextMuted, fontSize = 11.sp)
                if (preview.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(DeepRose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(preview.unreadCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatConnectScreen(
    companion: CompanionProfile,
    onText: () -> Unit,
    onCall: () -> Unit,
    onVideo: () -> Unit,
) {
    GradientBackground {
        ScreenScroll {
            GlassCard(background = CardAccent.copy(alpha = 0.94f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CompanionAvatar(
                        size = 82.dp,
                        label = companion.name.take(2),
                        glow = true,
                        photoUri = companion.photoUri,
                        imageResId = companion.imageResId,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(companion.name, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("How would you like to connect?", color = TextMuted, fontSize = 15.sp)
                    }
                }
            }
            SectionHeader("Connection options")
            Row(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
                ConnectOptionCard("Text", "Message quietly.", R.drawable.ic_chat_bubble, RosePink, Modifier.weight(1f), onText)
                ConnectOptionCard("Call", "Phone-style live call.", R.drawable.ic_call_end, CalmBlue, Modifier.weight(1f), onCall)
            }
            ConnectOptionCard("Video Chat", "See your companion's face while talking.", R.drawable.ic_video, AccentPurple, Modifier.fillMaxWidth(), onVideo)
        }
    }
}

@Composable
private fun ConnectOptionCard(
    title: String,
    subtitle: String,
    iconResId: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = modifier
            .height(132.dp)
            .clickable(onClick = onClick),
        background = CardDark.copy(alpha = 0.94f),
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(28.dp),
        )
        Text(title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CompanionChatScreen(companion: CompanionProfile, chatStorage: ChatStorage = ChatStorage.Cloud, onNavigate: (AppRoute) -> Unit) {
    val chatViewModel: ChatViewModel = viewModel(key = "chat_${companion.id}")
    val experience = remember(companion.id, companion.characterMode, companion.supportFocus, companion.communicationStyle, companion.personalityTraits) {
        calculateCompanionExperience(companion)
    }
    val geminiContext = remember(companion) {
        GeminiCompanionContext(
            companionId = companion.id,
            companionName = companion.name,
            gender = companion.gender,
            voice = companion.voice,
            characterMode = companion.characterMode,
            personalityTraits = companion.personalityTraits.ifEmpty { companion.personalityTags },
            communicationStyle = companion.communicationStyle,
            supportFocus = companion.supportFocus.toList(),
            shortDescription = companion.shortDescription,
            roleplayStyles = companion.activeRoleplayStyles(),
            bdsmEnabled = companion.isAdultRoleplayEnabled(),
            bdsmAdultConsentConfirmed = companion.bdsmIdentitySettings.adultConsentConfirmed,
            bdsmStopWord = companion.bdsmIdentitySettings.defaultStopWord,
            bdsmPauseWord = companion.bdsmIdentitySettings.defaultPauseWord,
            anatomicalLanguageAllowed = companion.isAdultRoleplayEnabled() && companion.bdsmIdentitySettings.anatomicalLanguageAllowed,
            adultPhrasePreferences = if (companion.isAdultRoleplayEnabled() && companion.bdsmIdentitySettings.anatomicalLanguageAllowed) companion.bdsmIdentitySettings.preferredAdultPhrases else "",
            adultProviderEnabled = companion.isAdultRoleplayEnabled() && companion.bdsmIdentitySettings.adultProviderEnabled,
            adultProviderEndpoint = companion.bdsmIdentitySettings.adultProviderEndpoint,
            adultProviderModel = companion.bdsmIdentitySettings.adultProviderModel,
        )
    }
    LaunchedEffect(companion.id) { chatViewModel.loadMessages(companion.id) }
    val messages by chatViewModel.messages.collectAsState()
    val isTyping by chatViewModel.isTyping.collectAsState()
    val geminiState by chatViewModel.geminiState.collectAsState()
    val chatStatus by chatViewModel.chatStatus.collectAsState()
    val displayMessages = messages.ifEmpty {
        listOf(ChatMessage(sender = ChatMessage.SENDER_COMPANION, chatId = stableChatIdForCompanion(companion.id), companionId = companion.id, text = FRESH_CHAT_GREETING))
    }
    var input by rememberSaveable(companion.id, chatStorage.label) { mutableStateOf("") }
    val chatScrollState = rememberScrollState()
    LaunchedEffect(displayMessages.size, displayMessages.lastOrNull()?.id, isTyping) {
        delay(80)
        chatScrollState.animateScrollTo(chatScrollState.maxValue)
    }
    var bdsmSessionState by remember(companion.id) { mutableStateOf(BdsmSessionState()) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondarySoftButton("See Companion Live", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.LiveCompanionCall) })
                SecondarySoftButton("Settings", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Preferences) })
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(chatScrollState)
                    .padding(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                displayMessages.forEach { ChatBubble(it) }
                if (isTyping) ThinkingBubble(companion.name)
            }
            GlassCard(padding = 10.dp) {
                when (val state = geminiState) {
                    GeminiCompanionState.Loading -> Text("${companion.name} is connecting to Gemini...", color = TextMuted, fontSize = 12.sp)
                    is GeminiCompanionState.Error -> Text(state.message, color = WarningPeach, fontSize = 12.sp, lineHeight = 16.sp)
                    is GeminiCompanionState.Success, null -> Unit
                }
                chatStatus?.let {
                    Text(it, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Tell me what's on your mind...") },
                        modifier = Modifier.weight(1f),
                        shape = LargeShape,
                        minLines = 1,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedPlaceholderColor = TextMuted,
                            unfocusedPlaceholderColor = TextMuted,
                            cursorColor = AccentPink,
                            focusedBorderColor = AccentPink,
                            unfocusedBorderColor = TextMuted.copy(alpha = 0.42f),
                            focusedContainerColor = CardDark.copy(alpha = 0.62f),
                            unfocusedContainerColor = CardDark.copy(alpha = 0.54f),
                        ),
                    )
                    SmallCircleButton("send") {
                        if (input.isNotBlank() && !isTyping) {
                            val userText = input.trim()
                            val history = messages.map { GeminiChatTurn(text = it.text, isUser = it.isUser) }
                            val bdsmSetupResponse = nextBdsmSetupResponse(companion, bdsmSessionState, userText)
                            input = ""
                            if (bdsmSetupResponse != null) {
                                val (reply, nextState) = bdsmSetupResponse
                                bdsmSessionState = nextState
                                chatViewModel.sendPreparedReply(companion, userText, reply, ChatMessage.MODE_TEXT)
                            } else {
                                chatViewModel.sendMessage(companion, geminiContext, userText, history, ChatMessage.MODE_TEXT)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun LiveCompanionCallScreen(companion: CompanionProfile, onNavigate: (AppRoute) -> Unit) {
    var state by remember(companion.id) {
        mutableStateOf(
            LiveCompanionCallUiState(
                companionName = companion.name,
                companionPhotoUri = companion.photoUri,
                avatarType = companion.avatarType,
                callStatus = "Ready to connect",
                currentCaption = "I'm listening. Tell me what's been sitting on your heart.",
            ),
        )
    }
    var showTextChat by remember { mutableStateOf(false) }
    var showCaptureSheet by remember { mutableStateOf(false) }
    var captureMessage by remember { mutableStateOf<String?>(null) }
    val chatViewModel: ChatViewModel = viewModel(key = "live_chat_${companion.id}")
    LaunchedEffect(companion.id) { chatViewModel.loadMessages(companion.id) }
    val chatMessages by chatViewModel.messages.collectAsState()
    val liveChatStatus by chatViewModel.chatStatus.collectAsState()
    val liveIsTyping by chatViewModel.isTyping.collectAsState()
    var textInput by rememberSaveable(companion.id, "live") { mutableStateOf("") }
    val liveChatScrollState = rememberScrollState()
    LaunchedEffect(showTextChat, chatMessages.size, chatMessages.lastOrNull()?.id, liveIsTyping) {
        if (showTextChat) {
            delay(80)
            liveChatScrollState.animateScrollTo(liveChatScrollState.maxValue)
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "live-call-glow")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1300), repeatMode = RepeatMode.Reverse),
        label = "pulse",
    )
    val wave by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(animation = tween(850), repeatMode = RepeatMode.Reverse),
        label = "wave",
    )
    val micGlow by animateFloatAsState(if (state.isMicOn) 1f else 0f, label = "mic-glow")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val liveRepository = remember { GeminiLiveCompanionRepository() }
    val geminiContext = remember(companion) {
        GeminiCompanionContext(
            companionId = companion.id,
            companionName = companion.name,
            gender = companion.gender,
            voice = companion.voice,
            characterMode = companion.characterMode,
            personalityTraits = companion.personalityTraits.ifEmpty { companion.personalityTags },
            communicationStyle = companion.communicationStyle,
            supportFocus = companion.supportFocus.toList(),
            shortDescription = companion.shortDescription,
            roleplayStyles = companion.activeRoleplayStyles(),
            bdsmEnabled = companion.isAdultRoleplayEnabled(),
            bdsmAdultConsentConfirmed = companion.bdsmIdentitySettings.adultConsentConfirmed,
            bdsmStopWord = companion.bdsmIdentitySettings.defaultStopWord,
            bdsmPauseWord = companion.bdsmIdentitySettings.defaultPauseWord,
            anatomicalLanguageAllowed = companion.isAdultRoleplayEnabled() && companion.bdsmIdentitySettings.anatomicalLanguageAllowed,
            adultPhrasePreferences = if (companion.isAdultRoleplayEnabled() && companion.bdsmIdentitySettings.anatomicalLanguageAllowed) companion.bdsmIdentitySettings.preferredAdultPhrases else "",
            adultProviderEnabled = companion.isAdultRoleplayEnabled() && companion.bdsmIdentitySettings.adultProviderEnabled,
            adultProviderEndpoint = companion.bdsmIdentitySettings.adultProviderEndpoint,
            adultProviderModel = companion.bdsmIdentitySettings.adultProviderModel,
        )
    }
    var liveSessionState by remember { mutableStateOf<GeminiLiveSessionState>(GeminiLiveSessionState.Idle) }
    var cameraEnabled by remember { mutableStateOf(false) }
    var pendingLiveMode by remember { mutableStateOf<LiveMode?>(null) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    fun startGeminiLive(mode: LiveMode) {
        val modeLabel = when (mode) {
            LiveMode.VIDEO_STYLE -> "video chat with the companion's visual avatar"
            LiveMode.VOICE -> "voice call"
            LiveMode.TEXT -> "text chat"
        }
        state = state.copy(
            selectedMode = mode,
            isMicOn = true,
            isUserSpeaking = true,
            isCompanionSpeaking = false,
            callStatus = "Connecting to Gemini Live...",
            currentCaption = "Connecting voice. When it is ready, speak naturally to ${companion.name}.",
        )
        cameraEnabled = false
        liveSessionState = GeminiLiveSessionState.Connecting
        scope.launch {
            val result = liveRepository.startAudioConversation(geminiContext, modeLabel)
            liveSessionState = result
            state = when (result) {
                GeminiLiveSessionState.Idle,
                GeminiLiveSessionState.Connecting -> state
                is GeminiLiveSessionState.Connected -> state.copy(
                    callStatus = "Voice connected",
                    isMicOn = true,
                    isUserSpeaking = true,
                    currentCaption = "${companion.name} can hear you now. Speak when you're ready.",
                )
                is GeminiLiveSessionState.Error -> state.copy(
                    callStatus = "Voice setup needs attention",
                    isMicOn = false,
                    isUserSpeaking = false,
                    currentCaption = result.message,
                )
            }
        }
    }
    val livePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val mode = pendingLiveMode
        pendingLiveMode = null
        val micGranted = grants[Manifest.permission.RECORD_AUDIO] ?: context.hasPermission(Manifest.permission.RECORD_AUDIO)
        val cameraGranted = grants[Manifest.permission.CAMERA] ?: context.hasPermission(Manifest.permission.CAMERA)
        if (!micGranted) {
            permissionMessage = "Voice mode needs microphone permission before it can start."
        } else if (mode != null) {
            permissionMessage = null
            startGeminiLive(mode)
        }
    }
    fun requestOrStartGeminiLive(mode: LiveMode) {
        val requiredPermissions = listOf(Manifest.permission.RECORD_AUDIO)
        val missing = requiredPermissions.filterNot { context.hasPermission(it) }
        if (missing.isNotEmpty()) {
            pendingLiveMode = mode
            permissionMessage = "Allow microphone access to start ${companion.name}."
            livePermissionLauncher.launch(missing.toTypedArray())
        } else {
            permissionMessage = null
            startGeminiLive(mode)
        }
    }
    fun stopGeminiLive() {
        scope.launch {
            liveRepository.stopAudioConversation()
            liveSessionState = GeminiLiveSessionState.Idle
            cameraEnabled = false
            state = state.copy(
                isMicOn = false,
                isUserSpeaking = false,
                isCompanionSpeaking = false,
                callStatus = "Mic muted",
                currentCaption = "Live voice is off. Tap Mic or Voice Chat when you want to reconnect.",
            )
        }
    }

    DisposableEffect(liveRepository) {
        onDispose {
            scope.launch { liveRepository.stopAudioConversation() }
        }
    }

    LaunchedEffect(state.isMicOn, state.selectedMode) {
        if (state.isMicOn) {
            state = state.copy(
                callStatus = "Listening...",
                isUserSpeaking = true,
                isCompanionSpeaking = false,
                currentCaption = "I'm listening. Take your time. Your thoughts are welcome here.",
            )
        }
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            ScreenScroll {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${companion.name} is here with you", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    SoftStatusChip("Live - emotionally present", SuccessGreen)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                        .shadow((16 + pulse * 10).dp, ExtraLargeShape)
                        .clip(ExtraLargeShape)
                        .background(Brush.verticalGradient(listOf(CardDark.copy(alpha = 0.98f), CardAccent.copy(alpha = 0.94f), DeepPlumBlack.copy(alpha = 0.98f))))
                        .border(1.dp, AccentPink.copy(alpha = 0.34f), ExtraLargeShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SoftGlowDot(Modifier.align(Alignment.TopStart).padding(32.dp), RosePink, pulse)
                    SoftGlowDot(Modifier.align(Alignment.TopEnd).padding(42.dp), CalmBlue, 1f - pulse)
                    SoftGlowDot(Modifier.align(Alignment.CenterEnd).padding(28.dp), GoldAccent, wave)

                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size((210 + pulse * 28).dp)
                                .clip(CircleShape)
                                .border(3.dp, RosePink.copy(alpha = 0.22f + wave * 0.38f), CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size((174 + pulse * 18).dp)
                                .clip(CircleShape)
                                .border(2.dp, Lavender.copy(alpha = 0.32f + wave * 0.42f), CircleShape),
                        )
                        CompanionAvatar(size = 142.dp, label = companion.name.take(2), glow = true, photoUri = companion.photoUri, imageResId = companion.imageResId)
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AudioWaveBar(wave, 20.dp)
                        AudioWaveBar(1f - wave, 34.dp)
                        AudioWaveBar(wave, 26.dp)
                        AudioWaveBar(1f - wave, 40.dp)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.28f))
                            .padding(18.dp),
                    ) {
                        Column {
                            Text(companion.name, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text(callDisplayStatus(state), color = Color.White.copy(alpha = 0.88f), fontSize = 15.sp)
                        }
                    }
                }

                if (state.isCaptionOn) {
                    GlassCard {
                        Text("Live Captions", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("\"${state.currentCaption}\"", color = TextMuted, fontSize = 16.sp, lineHeight = 22.sp)
                    }
                }
                LiveSessionStatusCard(liveSessionState = liveSessionState, permissionMessage = permissionMessage)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    ModeChip("Text Chat", state.selectedMode == LiveMode.TEXT) {
                        stopGeminiLive()
                        state = state.copy(selectedMode = LiveMode.TEXT)
                        showTextChat = true
                    }
                    ModeChip("Voice Chat", state.selectedMode == LiveMode.VOICE) { requestOrStartGeminiLive(LiveMode.VOICE) }
                    ModeChip("Video Chat", state.selectedMode == LiveMode.VIDEO_STYLE) { requestOrStartGeminiLive(LiveMode.VIDEO_STYLE) }
                }

                if (state.selectedMode == LiveMode.VIDEO_STYLE) {
                    CompanionVideoChatPanel(
                        companion = companion,
                        speaking = state.isCompanionSpeaking || state.isMicOn,
                        status = callDisplayStatus(state),
                    )
                }

                captureMessage?.let { message ->
                    GlassCard(background = SuccessGreen.copy(alpha = 0.24f)) {
                        Text(message, color = TextDark, fontWeight = FontWeight.Bold)
                        Text("Only the current app screen is captured when you tap Capture. Nothing records in the background.", color = TextMuted)
                    }
                }

                GlassCard {
                    Text("Call Controls", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        CallControlButton("Mic", R.drawable.ic_mic, state.isMicOn, RosePink.copy(alpha = 0.18f + micGlow * 0.42f)) {
                            if (state.isMicOn) {
                                stopGeminiLive()
                            } else {
                                requestOrStartGeminiLive(if (state.selectedMode == LiveMode.VIDEO_STYLE) LiveMode.VIDEO_STYLE else LiveMode.VOICE)
                            }
                        }
                        CallControlButton("Speaker", R.drawable.ic_speaker, state.isSpeakerOn, CalmBlue) {
                            state = state.copy(isSpeakerOn = !state.isSpeakerOn)
                        }
                        CallControlButton("Switch", R.drawable.ic_swap, false, GoldAccent) { onNavigate(AppRoute.Companions) }
                        CallControlButton("Capture", R.drawable.ic_capture, showCaptureSheet, SuccessGreen) { showCaptureSheet = true }
                        CallControlButton("End", R.drawable.ic_call_end, false, WarningPeach) {
                            stopGeminiLive()
                            onNavigate(AppRoute.Home)
                        }
                    }
                }
            }

            if (showCaptureSheet) {
                AlertDialog(
                    onDismissRequest = { showCaptureSheet = false },
                    containerColor = CardDark,
                    title = { Text("Screen Capture", color = TextDark, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Capture this live companion moment?", color = TextMuted)
                            PrimaryGradientButton("Capture Screen") {
                                captureMessage = "Captured successfully."
                                showCaptureSheet = false
                            }
                            SecondarySoftButton("Save to Device") {
                                captureMessage = "Captured successfully. Save-to-device is ready for real capture wiring."
                                showCaptureSheet = false
                            }
                            SecondarySoftButton("Save to Journal") {
                                captureMessage = "Captured successfully. Journal save can connect to entries next."
                                showCaptureSheet = false
                                onNavigate(AppRoute.Journal)
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showCaptureSheet = false }) { Text("Cancel", color = DeepRose) }
                    },
                )
            }

            if (showTextChat) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.16f))
                        .clickable { showTextChat = false },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    GlassCard(
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable(enabled = false) {},
                        background = CreamWhite.copy(alpha = 0.96f),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Text Chat", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { showTextChat = false }) { Text("Close", color = DeepRose) }
                        }
                        Column(
                            modifier = Modifier
                                .height(180.dp)
                                .verticalScroll(liveChatScrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            chatMessages.ifEmpty { listOf(ChatMessage(sender = ChatMessage.SENDER_COMPANION, chatId = stableChatIdForCompanion(companion.id), companionId = companion.id, text = FRESH_CHAT_GREETING)) }.forEach { ChatBubble(it) }
                        }
                        liveChatStatus?.let { Text(it, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp) }
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Type what you want to say...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = LargeShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedPlaceholderColor = TextMuted,
                                unfocusedPlaceholderColor = TextMuted,
                                cursorColor = AccentPink,
                                focusedBorderColor = AccentPink,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.42f),
                                focusedContainerColor = CardDark.copy(alpha = 0.62f),
                                unfocusedContainerColor = CardDark.copy(alpha = 0.54f),
                            ),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryGradientButton(
                                "Send",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (textInput.isNotBlank() && !liveIsTyping) {
                                        val userText = textInput.trim()
                                        val mode = when (state.selectedMode) {
                                            LiveMode.VIDEO_STYLE -> ChatMessage.MODE_VIDEO
                                            LiveMode.VOICE -> ChatMessage.MODE_CALL
                                            LiveMode.TEXT -> ChatMessage.MODE_TEXT
                                        }
                                        val history = chatMessages.map { GeminiChatTurn(text = it.text, isUser = it.isUser) }
                                        chatViewModel.sendMessage(companion, geminiContext, userText, history, mode)
                                        state = state.copy(currentCaption = userText, isUserSpeaking = true, isCompanionSpeaking = false)
                                        textInput = ""
                                    }},
                            )
                            SecondarySoftButton("Save to Journal", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Journal) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(label: String, iconResId: Int, active: Boolean, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(if (active || label == "End") 14.dp else 6.dp, CircleShape)
                .clip(CircleShape)
                .background(if (label == "End") WarningPeach.copy(alpha = 0.86f) else CardAccent.copy(alpha = 0.92f))
                .border(
                    1.dp,
                    if (active) color.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.18f),
                    CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = label,
                tint = if (label == "End") Color.White else if (active) Color.White else TextSecondary,
                modifier = Modifier.size(25.dp),
            )
        }
        Text(label, color = TextMuted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(
                if (selected) {
                    Brush.horizontalGradient(listOf(DeepRose, Lavender))
                } else {
                    Brush.horizontalGradient(listOf(CardDark.copy(alpha = 0.96f), CardAccent.copy(alpha = 0.88f)))
                },
            )
            .border(1.dp, Color.White.copy(alpha = 0.62f), PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, color = if (selected) Color.White else TextDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun AudioWaveBar(level: Float, maxHeight: Dp) {
    Box(
        modifier = Modifier
            .width(8.dp)
            .height(maxHeight * (0.35f + level * 0.65f))
            .clip(PillShape)
            .background(Color.White.copy(alpha = 0.72f)),
    )
}

@Composable
private fun SoftGlowDot(modifier: Modifier, color: Color, pulse: Float) {
    Box(
        modifier = modifier
            .size((18 + pulse * 12).dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.26f + pulse * 0.28f)),
    )
}

private fun callDisplayStatus(state: LiveCompanionCallUiState): String {
    return when {
        state.isUserSpeaking -> "Listening..."
        state.isCompanionSpeaking -> "Speaking softly..."
        state.isMicOn -> "Listening..."
        else -> state.callStatus
    }
}

@Composable
private fun LiveSessionStatusCard(
    liveSessionState: GeminiLiveSessionState,
    permissionMessage: String?,
) {
    val message = when (liveSessionState) {
        GeminiLiveSessionState.Idle -> permissionMessage
        GeminiLiveSessionState.Connecting -> "Connecting to Gemini Live voice..."
        is GeminiLiveSessionState.Connected -> liveSessionState.message
        is GeminiLiveSessionState.Error -> liveSessionState.message
    } ?: return
    val isError = liveSessionState is GeminiLiveSessionState.Error || permissionMessage != null
    GlassCard(background = if (isError) WarningPeach.copy(alpha = 0.18f) else CardDark.copy(alpha = 0.94f)) {
        Text(
            if (isError) "Live setup" else "Live connected",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(message, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun CameraPreviewPanel(
    enabled: Boolean,
    companionName: String,
    onRequestCamera: () -> Unit,
) {
    GlassCard(background = CardDark.copy(alpha = 0.94f)) {
        Text("Video Preview", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Use this to test the video side of the live companion call. Gemini voice can run with it while camera-frame understanding is prepared.",
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        if (enabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(LargeShape)
                    .background(Color.Black)
                    .border(1.dp, AccentPink.copy(alpha = 0.36f), LargeShape),
            ) {
                CameraPreviewSurface(modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.38f))
                        .padding(12.dp),
                ) {
                    Text(
                        "$companionName video preview active",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            SecondarySoftButton("Start Video Preview", onClick = onRequestCamera)
        }
    }
}

@Composable
private fun CompanionVideoChatPanel(
    companion: CompanionProfile,
    speaking: Boolean,
    status: String,
) {
    val transition = rememberInfiniteTransition(label = "companion-video-avatar")
    val pulse by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "video-pulse",
    )
    GlassCard(background = CardDark.copy(alpha = 0.94f)) {
        Text("Video Chat", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("The screen stays on your companion's face. Only one live voice session can run at a time.", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .clip(ExtraLargeShape)
                .background(Brush.verticalGradient(listOf(DeepPlumBlack, CardAccent, AppBlack)))
                .border(1.dp, AccentPink.copy(alpha = 0.36f), ExtraLargeShape),
            contentAlignment = Alignment.Center,
        ) {
            SoftGlowDot(Modifier.align(Alignment.TopStart).padding(28.dp), RosePink, pulse)
            SoftGlowDot(Modifier.align(Alignment.TopEnd).padding(32.dp), AccentPurple, 1f - pulse)
            Box(
                modifier = Modifier
                    .size((190 + if (speaking) pulse * 34 else 0f).dp)
                    .clip(CircleShape)
                    .border(3.dp, AccentPink.copy(alpha = 0.25f + pulse * 0.38f), CircleShape),
            )
            CompanionAvatar(size = 156.dp, label = companion.name.take(2), glow = true, photoUri = companion.photoUri, imageResId = companion.imageResId)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .padding(16.dp),
            ) {
                Column {
                    Text(companion.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(status, color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewSurface(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    if (activity == null) {
        Box(
            modifier = modifier.background(CardAccent),
            contentAlignment = Alignment.Center,
        ) {
            Text("Camera preview needs an active screen.", color = TextMuted, textAlign = TextAlign.Center)
        }
        return
    }
    AndroidView(
        modifier = modifier,
        factory = { previewContext ->
            PreviewView(previewContext).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    runCatching {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also { preview ->
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            activity,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                        )
                    }.onFailure { error ->
                        Log.e("FancieCamera", "Camera preview failed", error)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        },
    )
    DisposableEffect(context) {
        onDispose {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    runCatching { cameraProviderFuture.get().unbindAll() }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
    }
}

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun availableAudioInputSources(context: Context): List<String> {
    val sources = mutableListOf("Phone microphone only when selected")
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return sources
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val bluetoothPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).forEach { device ->
            val deviceName = device.productName?.toString()?.takeIf { it.isNotBlank() }
            val label = when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET -> if (bluetoothPermissionGranted) "Bluetooth headset${deviceName?.let { ": $it" }.orEmpty()}" else null
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio${deviceName?.let { ": $it" }.orEmpty()}"
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headset${deviceName?.let { ": $it" }.orEmpty()}"
                AudioDeviceInfo.TYPE_BUILTIN_MIC -> null
                else -> null
            }
            label?.let { sources.add(it) }
        }
    }
    return sources.distinct()
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
}

@Composable
private fun LiveCompanionScreen(companion: CompanionProfile, onNavigate: (AppRoute) -> Unit) {
    GradientBackground {
        ScreenScroll {
            GlassCard(background = CardAccent.copy(alpha = 0.94f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CompanionAvatar(
                        size = 170.dp,
                        label = companion.name.take(2),
                        glow = true,
                        photoUri = companion.photoUri,
                        imageResId = companion.imageResId,
                    )
                    Text(companion.name, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    SoftStatusChip("Ready to connect", SuccessGreen)
                    Text(companion.voice, color = TextSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    Text(
                        "Text, talk, or sit with your companion in a premium live space. Video Chat keeps the companion's face on screen while voice connects through Gemini Live.",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
            CompanionVideoChatPanel(companion = companion, speaking = false, status = "Ready for Video Chat")
            PrimaryGradientButton("Open Live Companion Call", onClick = { onNavigate(AppRoute.LiveCompanionCall) })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Text Chat", "Continue the conversation by typing.", "chat", RosePink, Modifier.weight(1f)) { onNavigate(AppRoute.TextChat) }
                QuickActionCard("Voice Chat", "Microphone-ready companion mode.", "voice", CalmBlue, Modifier.weight(1f)) { onNavigate(AppRoute.LiveCompanionCall) }
            }
            QuickActionCard("Video Chat", "See your companion's face while talking.", "video", AccentPurple, Modifier.fillMaxWidth()) { onNavigate(AppRoute.LiveCompanionCall) }
            GlassCard {
                Text("Video Chat is companion-facing.", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("This version shows the selected companion photo/avatar with live voice status. Full photo-to-talking-avatar lip sync requires a dedicated avatar animation service later.", color = TextMuted, lineHeight = 21.sp)
            }
        }
    }
}

@Composable
private fun AvatarSelectionScreen(
    title: String,
    options: List<AvatarOption>,
    selected: String,
    onBack: () -> Unit,
    onSelected: (AvatarOption) -> Unit,
) {
    GradientBackground {
        ScreenScroll {
            WellnessBackButton(onBack, "Back to Builder")
            SectionHeader(title)
            options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { option ->
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(226.dp)
                                .clickable { onSelected(option) },
                            padding = 14.dp,
                            background = if (selected == option.name) CardAccent.copy(alpha = 0.98f) else CardDark.copy(alpha = 0.92f),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                CompanionAvatar(size = 104.dp, label = option.name.take(2), glow = selected == option.name, imageResId = option.resId)
                                Text(option.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                            }
                            if (selected == option.name) SoftStatusChip("Selected", AccentPink)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MyCompanionsScreen(
    companions: List<CompanionProfile>,
    activeCompanionId: String,
    onSetActive: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onChat: (String) -> Unit,
    onLive: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    GradientBackground {
        ScreenScroll {
            if (companions.isEmpty()) {
                GlassCard {
                    Text("You haven't created a companion yet.", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Create one that feels safe, supportive, and personal to you.", color = TextMuted)
                    PrimaryGradientButton("Create Companion", onClick = onCreate)
                }
            } else {
                companions.forEach { companion ->
                    CompanionLibraryCard(
                        companion = companion,
                        active = activeCompanionId == companion.id,
                        onChat = { onChat(companion.id) },
                        onLive = { onLive(companion.id) },
                        onEdit = { onEdit(companion.id) },
                        onSetActive = { onSetActive(companion.id) },
                        onDelete = { onDelete(companion.id) },
                    )
                }
                PrimaryGradientButton("Create Companion", onClick = onCreate)
            }
        }
    }
}

@Composable
private fun CompanionLibraryCard(
    companion: CompanionProfile,
    active: Boolean,
    onChat: () -> Unit,
    onLive: () -> Unit,
    onEdit: () -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CompanionAvatar(size = 76.dp, label = companion.name.take(2), glow = active, photoUri = companion.photoUri, imageResId = companion.imageResId)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(companion.name, color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (active) SoftStatusChip("Active", SuccessGreen)
                }
                Text("Gender: ${companion.gender}", color = TextMuted)
                Text("Voice: ${companion.voice}", color = TextMuted)
                Text("Last used: ${companion.lastUsedDate}", color = TextMuted, fontSize = 13.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            companion.personalityTags.forEach { SoftFeatureChip(it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondarySoftButton("Chat", modifier = Modifier.weight(1f), onClick = onChat)
            SecondarySoftButton("Live", modifier = Modifier.weight(1f), onClick = onLive)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondarySoftButton("Edit", modifier = Modifier.weight(1f), onClick = onEdit)
            SecondarySoftButton("Set Active", modifier = Modifier.weight(1f), onClick = onSetActive)
            SecondarySoftButton("Delete", modifier = Modifier.weight(1f), onClick = onDelete)
        }
    }
}

@Composable
private fun PersonalityBuilderScreen(
    companion: CompanionProfile,
    isCreatingNew: Boolean = false,
    onCompanionChange: (CompanionProfile) -> Unit,
    onSave: () -> Unit,
    onOpenFemaleAvatars: () -> Unit = {},
    onOpenMaleAvatars: () -> Unit = {},
    onCustomizeCompanion: () -> Unit = {},
    onCancel: () -> Unit = onSave,
) {
    val genderOptions = listOf("Female", "Male", "Nonbinary", "Custom", "No preference")
    val femaleVoiceOptions = listOf("Soft Female", "Warm Female", "Confident Female", "Sultry Calm Female", "Bright Female", "Deep Feminine", "Gentle Whisper Female")
    val maleVoiceOptions = listOf("Calm Male", "Deep Male", "Protective Male", "Smooth Male", "Warm Male", "Motivational Male", "Soft-Spoken Male")
    val neutralVoiceOptions = listOf("Neutral Calm", "Neutral Bright", "Custom Voice Later")
    val personalityTraitOptions = listOf("Protective", "Jealous", "Clingy", "Rude", "Playful", "Funny", "Ambitious", "Kind", "Sweet", "Provider")
    val communicationCards = listOf(
        "Gentle" to "Soft, nurturing, calming",
        "Direct" to "Clear, honest, no overthinking",
        "Deep" to "Reflective, emotional, thoughtful",
        "Flirty" to "Warm, playful, and still respectful",
        "Motivational" to "Pushes me with love",
        "Soothing" to "Slow, calming, and tender",
    )
    val supportOptions = listOf("Stress", "Self-worth", "Confidence", "Grounding", "Journaling", "Reflection", "Relationships", "Creativity", "Fitness")
    val roleplayStyles = listOf("Wellness Coach", "Athletic Partner", "Monologue Practice", "BDSM")
    val characterModes = listOf(
        "Best Friend" to "Supportive, loyal, honest, and emotionally present.",
        "Romantic Partner" to "Affectionate, attentive, emotionally close, and caring.",
        "The Opps" to "A slight hater who indirectly pushes you toward the things you think you can't do.",
        "Assistant" to "Helpful, organized, focused, and practical.",
    )
    val context = LocalContext.current
    val allowedVoiceOptions = when (companion.gender) {
        "Female" -> femaleVoiceOptions
        "Male" -> maleVoiceOptions
        else -> femaleVoiceOptions + maleVoiceOptions + neutralVoiceOptions
    }
    var traitLimitMessage by remember { mutableStateOf<String?>(null) }
    var timerDuration by remember { mutableStateOf("1 minute") }
    var motivationStyle by remember { mutableStateOf("Soft encouragement") }
    var workoutTimerState by remember { mutableStateOf("Ready") }
    var pastedScript by remember { mutableStateOf("") }
    var stopWord by remember { mutableStateOf("Pause") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onCompanionChange(
                companion.copy(
                    photoUri = it.toString(),
                    avatarType = "Uploaded Photo",
                ),
            )
        }
    }

    GradientBackground {
        ScreenScroll {
            CompanionImageSquare(companion)
            SectionHeader("Appearance")
            GlassCard {
                SoftDropdown(
                    label = "Photo / Avatar Type",
                    selected = if (companion.photoUri != null) "Upload Photo" else "Choose Avatar",
                    options = listOf("Upload Photo", "Choose Avatar", "Remove Photo"),
                ) { option ->
                    when (option) {
                        "Upload Photo" -> photoPicker.launch(arrayOf("image/*"))
                        "Choose Avatar" -> onCompanionChange(companion.copy(avatarType = "Choose Avatar", photoUri = null))
                        "Remove Photo" -> onCompanionChange(companion.copy(avatarType = "Glowing Orb", photoUri = null, photoStoragePath = null, imageResName = "", imageResId = null))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Female Avatars", modifier = Modifier.weight(1f), onClick = onOpenFemaleAvatars)
                    SecondarySoftButton("Male Avatars", modifier = Modifier.weight(1f), onClick = onOpenMaleAvatars)
                }
                Text("Firebase Storage path ready: users/{userId}/companions/ using the signed-in Auth UID.", color = TextMuted, fontSize = 13.sp)
            }
            SectionHeader("Character Mode")
            CharacterModeGrid(characterModes, companion.characterMode) { mode ->
                if (mode == "Customize Companion") {
                    onCustomizeCompanion()
                } else {
                    onCompanionChange(companion.copy(characterMode = mode))
                }
            }
            SectionHeader("Basic Info")
            GlassCard {
                if (companion.id in listOf("luna", "kai") && !isCreatingNew) {
                    MiniStateCard("Companion name", companion.name)
                    Text("Luna and Kai are built-in companions, so their names stay fixed. Their photo, voice, personality, roleplay style, and support focus can still be customized.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
                } else {
                    RoundedInputField(companion.name, { onCompanionChange(companion.copy(name = it)) }, "Companion name")
                }
                SoftDropdown("Gender", companion.gender, genderOptions) { gender ->
                    onCompanionChange(companion.copy(gender = gender, voice = voiceForGender(gender, companion.voice)))
                }
                RoundedInputField(companion.shortDescription, { onCompanionChange(companion.copy(shortDescription = it)) }, "Short description", minLines = 2)
            }
            SectionHeader("Voice")
            GlassCard {
                SoftDropdown("Companion voice", companion.voice, allowedVoiceOptions) { voice ->
                    onCompanionChange(companion.copy(voice = voice))
                }
            }

            SectionHeader("Personality Traits", "Choose up to 4 traits.")
            GlassCard {
                CheckboxOptionGrid(
                    options = personalityTraitOptions,
                    selected = companion.personalityTraits.toSet(),
                    onToggle = { option ->
                        val selected = option in companion.personalityTraits
                        if (selected) {
                            traitLimitMessage = null
                            onCompanionChange(companion.copy(personalityTraits = companion.personalityTraits - option))
                        } else if (companion.personalityTraits.size >= 4) {
                            traitLimitMessage = "You can choose up to 4 personality traits."
                        } else {
                            traitLimitMessage = null
                            onCompanionChange(companion.copy(personalityTraits = companion.personalityTraits + option))
                        }
                    },
                )
                traitLimitMessage?.let {
                    Text(it, color = WarningPeach, fontWeight = FontWeight.SemiBold)
                }
            }

            SectionHeader("Roleplay Settings")
            GlassCard {
                ToggleRow("Roleplay enabled", companion.roleplayEnabled) { onCompanionChange(companion.copy(roleplayEnabled = it)) }
                val selectedRoleplayStyles = companion.roleplayStyles.ifEmpty { listOf(companion.roleplayStyle) }.toSet()
                CheckboxOptionGrid(
                    options = roleplayStyles,
                    selected = selectedRoleplayStyles,
                    onToggle = { option ->
                        val next = if (option in selectedRoleplayStyles) {
                            selectedRoleplayStyles - option
                        } else {
                            selectedRoleplayStyles + option
                        }
                        val nextList = next.ifEmpty { setOf("Wellness Coach") }.toList()
                        val bdsmSettings = if (option == "BDSM" && option !in selectedRoleplayStyles) {
                            companion.bdsmIdentitySettings.copy(enabled = companion.bdsmIdentitySettings.adultConsentConfirmed)
                        } else if (option == "BDSM") {
                            companion.bdsmIdentitySettings.copy(enabled = false)
                        } else {
                            companion.bdsmIdentitySettings
                        }
                        val primaryRoleplayStyle = if (option in nextList) option else nextList.first()
                        onCompanionChange(companion.copy(roleplayStyles = nextList, roleplayStyle = primaryRoleplayStyle, bdsmIdentitySettings = bdsmSettings))
                    },
                )
                RoleplayDetails(
                    style = companion.roleplayStyle,
                    adultRoleplaySelected = companion.isAdultRoleplaySelected(),
                    timerDuration = timerDuration,
                    onTimerDurationChange = { timerDuration = it },
                    workoutTimerState = workoutTimerState,
                    onWorkoutTimerStateChange = { workoutTimerState = it },
                    motivationStyle = motivationStyle,
                    onMotivationStyleChange = { motivationStyle = it },
                    pastedScript = pastedScript,
                    onPastedScriptChange = { pastedScript = it },
                    stopWord = stopWord,
                    onStopWordChange = { stopWord = it },
                    boundaries = companion.safeBoundaries,
                    onBoundariesChange = { onCompanionChange(companion.copy(safeBoundaries = it)) },
                    bdsmIdentitySettings = companion.bdsmIdentitySettings,
                    onBdsmConsentChange = { confirmed ->
                        onCompanionChange(companion.copy(
                            bdsmIdentitySettings = companion.bdsmIdentitySettings.copy(
                                enabled = confirmed && "BDSM" in selectedRoleplayStyles,
                                adultConsentConfirmed = confirmed,
                            ),
                        ))
                    },
                    onAnatomicalLanguageAllowedChange = { allowed ->
                        onCompanionChange(companion.copy(
                            bdsmIdentitySettings = companion.bdsmIdentitySettings.copy(anatomicalLanguageAllowed = allowed),
                        ))
                    },
                    onPreferredAdultPhrasesChange = { phrases ->
                        onCompanionChange(companion.copy(
                            bdsmIdentitySettings = companion.bdsmIdentitySettings.copy(preferredAdultPhrases = phrases),
                        ))
                    },
                    onAdultProviderEnabledChange = { enabled ->
                        onCompanionChange(companion.copy(
                            bdsmIdentitySettings = companion.bdsmIdentitySettings.copy(adultProviderEnabled = enabled),
                        ))
                    },
                    onAdultProviderEndpointChange = { endpoint ->
                        onCompanionChange(companion.copy(
                            bdsmIdentitySettings = companion.bdsmIdentitySettings.copy(adultProviderEndpoint = endpoint),
                        ))
                    },
                    onAdultProviderModelChange = { model ->
                        onCompanionChange(companion.copy(
                            bdsmIdentitySettings = companion.bdsmIdentitySettings.copy(adultProviderModel = model),
                        ))
                    },
                )
            }

            SectionHeader("Communication Style")
            GlassCard {
                val selectedCommunicationStyles = companion.communicationStyles.ifEmpty { listOf(companion.communicationStyle) }.toSet()
                CheckboxOptionGrid(
                    options = communicationCards.map { it.first },
                    selected = selectedCommunicationStyles,
                    onToggle = { option ->
                        val next = if (option in selectedCommunicationStyles) {
                            selectedCommunicationStyles - option
                        } else {
                            selectedCommunicationStyles + option
                        }
                        val nextList = next.ifEmpty { setOf("Gentle") }.toList()
                        onCompanionChange(companion.copy(communicationStyles = nextList, communicationStyle = nextList.first()))
                    },
                    descriptions = communicationCards.toMap(),
                )
            }

            SectionHeader("Support Focus")
            GlassCard {
                CheckboxOptionGrid(
                    options = supportOptions,
                    selected = companion.supportFocus,
                    onToggle = { option ->
                        val checked = option in companion.supportFocus
                        val next = if (checked) companion.supportFocus - option else companion.supportFocus + option
                        onCompanionChange(companion.copy(supportFocus = next))
                    }
                )
                SupportFocusContentSections(companion.supportFocus)
            }
            GlassCard {
                Text("Save Actions", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                PrimaryGradientButton("Save and Set Active", onClick = onSave)
                SecondarySoftButton("Save Companion", onClick = onSave)
                SecondarySoftButton("Cancel", onClick = onCancel)
            }
        }
    }
}

@Composable
private fun CompanionImageSquare(companion: CompanionProfile) {
    val imageBitmap = rememberProfileImage(companion.photoUri)
    GlassCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(172.dp)
                    .shadow(18.dp, LargeShape)
                    .clip(LargeShape)
                    .background(Brush.linearGradient(listOf(CardAccent, DeepPlumBlack, CardDark)))
                    .border(1.dp, AccentPink.copy(alpha = 0.36f), LargeShape),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    imageBitmap != null -> Image(
                        bitmap = imageBitmap,
                        contentDescription = "${companion.name} photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    companion.imageResId != null -> Image(
                        painter = painterResource(companion.imageResId),
                        contentDescription = "${companion.name} avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    else -> CompanionAvatar(size = 118.dp, label = companion.name.take(2), glow = true)
                }
            }
            Text(companion.name, color = TextDark, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            val traits = companion.personalityTraits.ifEmpty { companion.personalityTags }
            Text(
                companion.shortDescription,
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            SoftStatusChip(traits.joinToString(" / "), Lavender)
        }
    }
}

@Composable
private fun CharacterModeGrid(
    modes: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        modes.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (title, body) ->
                    CharacterModeCard(
                        title = title,
                        body = body,
                        selected = selected == title,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelected(title) },
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        CharacterModeCard(
            title = "Customize Companion",
            body = "Create your ideal companion with expanded personalization options.",
            selected = selected == "Customize Companion",
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSelected("Customize Companion") },
        )
    }
}

@Composable
private fun CharacterModeCard(
    title: String,
    body: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick),
        padding = 12.dp,
        background = if (selected) CardAccent.copy(alpha = 0.98f) else CardDark.copy(alpha = 0.92f),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(AccentPink.copy(alpha = 0.80f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SoftDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    val appearance = LocalAppAppearance.current
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = appearance.mutedText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(MediumShape)
                    .background(appearance.control)
                    .border(1.dp, appearance.accentStart.copy(alpha = 0.42f), MediumShape)
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selected.ifBlank { "Choose option" }, color = appearance.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("v", color = appearance.mutedText, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(appearance.control),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = appearance.text) },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckboxOptionGrid(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    descriptions: Map<String, String> = emptyMap(),
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { option ->
                    CheckboxOptionCard(
                        label = option,
        description = descriptions[option],
                        checked = option in selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onToggle(option) },
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CheckboxOptionCard(
    label: String,
    description: String?,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(if (description == null) 64.dp else 96.dp)
            .clip(MediumShape)
            .background(if (checked) CardAccent.copy(alpha = 0.98f) else CardDark.copy(alpha = 0.82f))
            .border(1.dp, if (checked) AccentPink.copy(alpha = 0.74f) else Color.White.copy(alpha = 0.12f), MediumShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (description != null) {
                Text(description, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SupportFocusContentSections(selectedFocus: Set<String>) {
    val content = mapOf(
        "Stress" to listOf("Breathing reset", "Grounding prompts", "Stress check-ins", "Calming reminders"),
        "Self-worth" to listOf("Affirmations", "Reflection exercises", "Confidence journaling", "Supportive companion messages"),
        "Confidence" to listOf("Small action prompts", "Self-belief exercises", "Progress reflection", "Encouragement"),
        "Grounding" to listOf("5-4-3-2-1 sensory prompt", "Breath reset", "Body scan", "Present moment check-in"),
        "Journaling" to listOf("Guided prompts", "Open writing", "Emotional release", "Save-to-journal flow"),
        "Reflection" to listOf("What am I feeling?", "What triggered this?", "What do I need right now?", "What can I release?"),
        "Relationships" to listOf("Communication prompts", "Boundary reflection", "Clarity questions", "Emotional pattern reflection"),
        "Creativity" to listOf("Idea prompts", "Motivation nudges", "Creative flow check-ins", "Project encouragement"),
    )
    val visible = selectedFocus.ifEmpty { setOf("Stress", "Grounding") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
        Text("Support content ready", color = TextSecondary, fontWeight = FontWeight.SemiBold)
        visible.forEach { focus ->
            GlassCard(background = CardAccent.copy(alpha = 0.48f), padding = 14.dp) {
                Text(focus, color = TextPrimary, fontWeight = FontWeight.Bold)
                content[focus].orEmpty().forEach { item ->
                    Text("- $item", color = TextMuted, fontSize = 13.sp)
                }
            }
        }
        Text(
            "These tools are supportive wellness prompts only and do not diagnose, treat, or replace professional care.",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun AvatarChoiceRow(
    options: List<Pair<String, Int?>>,
    selected: String,
    onSelected: (String, Int?) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        options.forEach { (name, image) ->
            Column(
                modifier = Modifier
                    .width(82.dp)
                    .clip(LargeShape)
                    .background(if (selected == name) CardAccent.copy(alpha = 0.96f) else CardDark.copy(alpha = 0.72f))
                    .border(1.dp, if (selected == name) AccentPink else Color.White.copy(alpha = 0.12f), LargeShape)
                    .clickable { onSelected(name, image) }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompanionAvatar(size = 54.dp, label = name.take(2), glow = selected == name, imageResId = image)
                Text(name, color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun VoiceOptionGroup(title: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Text(title, color = TextDark, fontWeight = FontWeight.SemiBold)
    MultiSelectChipGrid(options = options, selected = setOf(selected), onToggle = onSelected)
}

@Composable
private fun TraitPicker(options: List<String>, selected: List<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { option ->
                    Box(modifier = Modifier.weight(1f)) {
                        MoodChip(label = option, selected = option in selected, onClick = { onToggle(option) })
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (selected.isNotEmpty()) {
            Text("Chosen traits", color = TextMuted, fontSize = 13.sp)
            SupportFocusChips(selected.toSet())
        }
    }
}

@Composable
private fun RoleplayDetails(
    style: String,
    adultRoleplaySelected: Boolean,
    timerDuration: String,
    onTimerDurationChange: (String) -> Unit,
    workoutTimerState: String,
    onWorkoutTimerStateChange: (String) -> Unit,
    motivationStyle: String,
    onMotivationStyleChange: (String) -> Unit,
    pastedScript: String,
    onPastedScriptChange: (String) -> Unit,
    stopWord: String,
    onStopWordChange: (String) -> Unit,
    boundaries: String,
    onBoundariesChange: (String) -> Unit,
    bdsmIdentitySettings: BdsmIdentitySettings,
    onBdsmConsentChange: (Boolean) -> Unit,
    onAnatomicalLanguageAllowedChange: (Boolean) -> Unit,
    onPreferredAdultPhrasesChange: (String) -> Unit,
    onAdultProviderEnabledChange: (Boolean) -> Unit,
    onAdultProviderEndpointChange: (String) -> Unit,
    onAdultProviderModelChange: (String) -> Unit,
) {
    var detailOptions by remember(style) { mutableStateOf(setOf<String>()) }
    when {
        style == "Athletic Partner" -> {
            Text("Physical fitness trainer mode. The companion should ask for goals, weigh-ins, available equipment, workout level, reps, sets, rest time, and recovery needs before recommending a plan.", color = TextMuted, lineHeight = 20.sp)
            SoftDropdown("Motivation Style", motivationStyle, listOf("Soft encouragement", "Trainer energy", "Discipline mode", "Playful push"), onMotivationStyleChange)
        }
        style == "Monologue Practice" -> {
            Text("Practice speeches, acting lines, affirmations, presentations, and spoken delivery.", color = TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondarySoftButton("Upload Script", modifier = Modifier.weight(1f), onClick = {})
                SecondarySoftButton("Paste Script", modifier = Modifier.weight(1f), onClick = {})
            }
            RoundedInputField(pastedScript, onPastedScriptChange, "Paste script or monologue here", minLines = 4)
            CheckboxOptionGrid(
                listOf("Choose user lines", "Choose companion lines", "Line-by-line", "Full read-through", "Repeat after me", "Performance feedback"),
                detailOptions,
                { option -> detailOptions = if (option in detailOptions) detailOptions - option else detailOptions + option },
            )
        }
        adultRoleplaySelected -> {
            GlassCard(background = WarningPeach.copy(alpha = 0.18f), padding = 14.dp) {
                Text("BDSM Mode Enabled", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("This companion can support adult consent-based fantasy roleplay through chat. Boundaries, roles, and safewords will be established before roleplay begins.", color = TextMuted, fontSize = 14.sp, lineHeight = 20.sp)
            }
            ToggleRow("I understand this mode is for consenting adults only.", bdsmIdentitySettings.adultConsentConfirmed, onBdsmConsentChange)
                        ToggleRow("Allow anatomical words", bdsmIdentitySettings.anatomicalLanguageAllowed, onAnatomicalLanguageAllowedChange)
            Text("Allows adult language preferences only when this adult mode is selected and consent is confirmed.", color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
            RoundedInputField(
                value = bdsmIdentitySettings.preferredAdultPhrases,
                onValueChange = onPreferredAdultPhrasesChange,
                label = "Preferred adult phrases",
                minLines = 3,
            )
            Text("Saved privately with this companion and sent only when adult language preferences are enabled.", color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
            HorizontalDivider(color = TextMuted.copy(alpha = 0.18f))
            ToggleRow("Use self-hosted adult provider", bdsmIdentitySettings.adultProviderEnabled, onAdultProviderEnabledChange)
            Text("Routes adult-mode messages to your own OpenAI-compatible Llama or DeepSeek endpoint after the local safety filter passes.", color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
            RoundedInputField(
                value = bdsmIdentitySettings.adultProviderEndpoint,
                onValueChange = onAdultProviderEndpointChange,
                label = "Self-hosted adult provider URL",
                minLines = 1,
            )
            RoundedInputField(
                value = bdsmIdentitySettings.adultProviderModel,
                onValueChange = onAdultProviderModelChange,
                label = "Model name",
                minLines = 1,
            )
            MiniStateCard("Safety filter", "Blocks minors, non-consent, illegal content, and real harm before provider calls")
            MiniStateCard("Default stop word", bdsmIdentitySettings.defaultStopWord)
            MiniStateCard("Default pause word", bdsmIdentitySettings.defaultPauseWord)
        }
        else -> {
            Text("Supportive wellness guidance, grounding, reflection, and encouragement. This mode is for wellness support only and does not diagnose, treat, or replace professional care.", color = TextMuted, lineHeight = 20.sp)
            CheckboxOptionGrid(
                listOf("Gentle", "Direct", "Motivational", "Reflective"),
                detailOptions,
                { option -> detailOptions = if (option in detailOptions) detailOptions - option else detailOptions + option },
            )
        }
    }
}

@Composable
private fun MultiSelectChipGrid(options: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { option ->
                    Box(modifier = Modifier.weight(1f)) {
                        MoodChip(label = option, selected = option in selected, onClick = { onToggle(option) })
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SupportFocusChips(items: Set<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.ifEmpty { setOf("Stress", "Grounding") }.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(CardAccent.copy(alpha = 0.86f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), PillShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(item, color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalScreen(
    title: String,
    body: String,
    mood: String,
    storage: JournalStorage,
    askEveryTime: Boolean,
    videoJournalState: String,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onMoodChange: (String) -> Unit,
    onReflect: () -> Unit,
    onStorageSheet: () -> Unit,
    onStorageChange: (JournalStorage) -> Unit,
    onVideoStateChange: (String) -> Unit,
    onViewEntries: () -> Unit,
) {
    GradientBackground {
        ScreenScroll {
            SectionHeader("New entry", "Write, speak, or record what you want to save.")
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftStatusChip("Mood: $mood", RosePink)
                    SoftStatusChip("Storage: ${storage.label}", CalmBlue)
                    SmallCircleButton("mic") {}
                }
                RoundedInputField(title, onTitleChange, "Entry title")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("Calm", "Heavy", "Hopeful", "Anxious", "Tired").forEach { MoodChip(label = it, selected = mood == it, onClick = { onMoodChange(it) }) }
                }
                Spacer(Modifier.height(12.dp))
                RoundedInputField(body, onBodyChange, "Write what's on your heart...", minLines = 8)
                Spacer(Modifier.height(16.dp))
                GlassCard(background = Lavender.copy(alpha = 0.18f), padding = 14.dp) {
                    Text("Voice journal", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Record voice entry. Transcription can connect later.", color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondarySoftButton("Record Voice", modifier = Modifier.weight(1f), onClick = {})
                        SecondarySoftButton("Transcribe Later", modifier = Modifier.weight(1f), onClick = {})
                    }
                }
                GlassCard(background = CalmBlue.copy(alpha = 0.14f), padding = 14.dp) {
                    Text("Video journal", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Status: $videoJournalState", color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondarySoftButton("Start Recording", modifier = Modifier.weight(1f), onClick = { onVideoStateChange("Recording") })
                        SecondarySoftButton("Stop", modifier = Modifier.weight(1f), onClick = { onVideoStateChange("Stopped") })
                    }
                    PrimaryGradientButton("Save Video Entry", onClick = { onVideoStateChange("Saved video placeholder") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondarySoftButton("Device", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Device) })
                        SecondarySoftButton("Cloud", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Cloud) })
                        SecondarySoftButton("Both", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Both) })
                    }
                    Text("Model ready: videoEntryUri and users/{userId}/journal/video/ storage paths.", color = TextMuted, fontSize = 12.sp)
                }
                PrimaryGradientButton("Save Entry", onClick = { if (askEveryTime) onStorageSheet() })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Device", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Device) })
                    SecondarySoftButton("Cloud", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Cloud) })
                    SecondarySoftButton("Both", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Both) })
                }
                SecondarySoftButton("Ask Every Time", onClick = onStorageSheet)
                SecondarySoftButton("Ask Companion to Reflect", onClick = onReflect)
            }
            PrimaryGradientButton("View Saved Entries", onClick = onViewEntries)
        }
    }
}

@Composable
private fun SavedJournalEntriesScreen(storage: JournalStorage) {
    val entries = listOf(
        JournalEntry(
            date = "April 30, 2026",
            mood = "Calm",
            title = "Today I needed clarity",
            preview = "I noticed that I feel steadier when I name what I need first.",
            storageLocation = storage,
            entryType = JournalEntryType.TEXT,
        ),
        JournalEntry(
            date = "April 29, 2026",
            mood = "Heavy",
            title = "Voice note after work",
            preview = "A saved voice entry placeholder with later transcription support.",
            storageLocation = JournalStorage.Device,
            entryType = JournalEntryType.VOICE,
            voiceEntryUri = "local://voice-placeholder",
        ),
        JournalEntry(
            date = "April 28, 2026",
            mood = "Hopeful",
            title = "Video reflection",
            preview = "A saved video journal placeholder ready for device or cloud storage.",
            storageLocation = JournalStorage.Both,
            entryType = JournalEntryType.VIDEO,
            videoEntryUri = "local://video-placeholder",
            videoStoragePath = "users/{userId}/journal/video/video-placeholder",
        ),
    )

    GradientBackground {
        ScreenScroll {
            SectionHeader("Saved entries", "Written, voice, and video journal history.")
            entries.forEach { JournalEntryCard(it) }
        }
    }
}

@Composable
private fun WellnessBackButton(onBack: () -> Unit, label: String = "Back to Wellness") {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(CardAccent.copy(alpha = 0.92f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), PillShape)
                .clickable(onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun WellnessHubScreen(onNavigate: (AppRoute) -> Unit) {
    GradientBackground {
        ScreenScroll {
            SectionHeader("Wellness", "Choose the kind of support you need right now.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Reflection", "Sort through what you feel.", "journal", RosePink, Modifier.weight(1f)) { onNavigate(AppRoute.Reflection) }
                QuickActionCard("Grounding", "Come back to the present.", "safe", CalmBlue, Modifier.weight(1f)) { onNavigate(AppRoute.Grounding) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Check-In", "Name your mood and need.", "heart", Lavender, Modifier.weight(1f)) { onNavigate(AppRoute.CheckIn) }
                QuickActionCard("Breathing", "Use a soft nervous-system reset.", "reset", GoldAccent, Modifier.weight(1f)) { onNavigate(AppRoute.Breathing) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Affirmations", "Hold a kinder thought.", "safe", SuccessGreen, Modifier.weight(1f)) { onNavigate(AppRoute.Affirmations) }
                QuickActionCard("Fitness", "Move gently and track progress.", "heart", WarningPeach, Modifier.weight(1f)) { onNavigate(AppRoute.Fitness) }
            }
        }
    }
}
@Composable
private fun ReflectionScreen(onBack: () -> Unit, onSaveToJournal: () -> Unit) {
    val prompts = listOf(
        ReflectionPrompt("What am I feeling?", "Name the feeling without making it a problem to solve."),
        ReflectionPrompt("What triggered this?", "Look gently at the moment that changed your state."),
        ReflectionPrompt("What do I need right now?", "Choose comfort, clarity, courage, or rest."),
        ReflectionPrompt("What am I learning about myself?", "Let the pattern become information, not judgment."),
        ReflectionPrompt("What can I release today?", "Put one burden down for now."),
    )
    var selected by remember { mutableStateOf(prompts.first()) }
    var reflection by remember { mutableStateOf("") }

    GradientBackground {
        ScreenScroll {
            WellnessBackButton(onBack)
            SectionHeader("Reflection Space", "Let's understand what your feelings are trying to show you.")
            prompts.forEach { SelectableInfoCard(it.title, it.description, selected == it) { selected = it } }
            GlassCard {
                Text(selected.title, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                RoundedInputField(reflection, { reflection = it }, "Share what comes up...", minLines = 5)
                PrimaryGradientButton("Save to Journal", onClick = onSaveToJournal)
                SecondarySoftButton("Ask Companion", onClick = {})
            }
        }
    }
}

@Composable
private fun GroundingScreen(onBack: () -> Unit) {
    var active by remember { mutableStateOf(false) }
    var breathText by remember { mutableStateOf("Breathe in") }

    LaunchedEffect(active) {
        if (active) {
            val cycle = listOf("Breathe in", "Hold", "Breathe out")
            repeat(6) { index ->
                breathText = cycle[index % cycle.size]
                delay(1000)
            }
            active = false
            breathText = "Breathe in"
        }
    }

    GradientBackground {
        ScreenScroll {
            WellnessBackButton(onBack)
            SectionHeader("Body reset", "Come back to your body, one breath at a time.")
            GlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(if (active) 170.dp else 138.dp)
                            .shadow(18.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(RosePink, Lavender, CalmBlue))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(breathText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Text("60-second reset", color = TextMuted, modifier = Modifier.padding(top = 14.dp))
                    PrimaryGradientButton("Start 60-Second Reset", onClick = { active = true }, modifier = Modifier.padding(top = 14.dp))
                }
            }
            SectionHeader("Grounding Tools")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("5-4-3-2-1 Reset", "Name what you see, feel, hear, smell, and taste.", "reset", RosePink, Modifier.weight(1f)) {}
                QuickActionCard("Body Scan", "Notice where tension is sitting.", "body", Lavender, Modifier.weight(1f)) {}
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Soft Affirmation", "Give your mind something safe to hold.", "safe", CalmBlue, Modifier.weight(1f)) {}
                QuickActionCard("Release Thought", "Write one thought and let it pass.", "free", WarningPeach, Modifier.weight(1f)) {}
            }
            GlassCard {
                Text("You are safe in this moment.", color = TextDark, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("You do not have to solve everything right now.", color = TextMuted, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun FitnessScreen(
    onBack: () -> Unit,
    activeWorkoutTitle: String,
    timerRunning: Boolean,
    sessionSeconds: Int,
    minutesToday: Int,
    workoutsCompleted: Int,
    streakDays: Int,
    goalMinutes: Float,
    totalSeconds: Int,
    lastWorkoutTitle: String,
    onGoalChange: (Float) -> Unit,
    onSelectWorkout: (String) -> Unit,
    onStartActive: () -> Unit,
    onAddFiveMinutes: () -> Unit,
    onReset: () -> Unit,
    onCompleteWorkout: (Int) -> Unit,
    onProgress: () -> Unit,
) {
    val workouts = listOf(
        FreeWorkout("Soft Start Stretch", "Beginner", 8, "Mobility", listOf("Neck rolls", "Shoulder circles", "Hamstring stretch", "Deep breathing")),
        FreeWorkout("Low Impact Glow", "Beginner", 12, "Full body", listOf("Step touches", "Bodyweight squats", "Wall pushups", "Standing core twists")),
        FreeWorkout("Core Confidence", "Beginner", 10, "Core", listOf("Dead bugs", "Heel taps", "Bird dogs", "Plank hold")),
        FreeWorkout("Lower Body Reset", "All levels", 15, "Legs and glutes", listOf("Glute bridges", "Reverse lunges", "Calf raises", "Chair squats")),
        FreeWorkout("Walk and Breathe", "All levels", 20, "Cardio", listOf("Easy walk", "Posture check", "Breath pacing", "Cool down")),
        FreeWorkout("Calm Strength", "Intermediate", 18, "Strength", listOf("Squats", "Incline pushups", "Rows with towel", "Slow mountain climbers")),
    )
    val activeWorkout = workouts.firstOrNull { it.title == activeWorkoutTitle } ?: workouts.first()
    val progress = (minutesToday / goalMinutes.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val context = LocalContext.current
    var trainerVideoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var trainerVideoStatus by rememberSaveable { mutableStateOf("No trainer video recorded yet.") }
    val trainerVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            trainerVideoUri = uri?.toString()
            trainerVideoStatus = if (uri != null) {
                "Trainer video recorded and ready."
            } else {
                "Video recorded. Camera app did not return a shareable URI."
            }
        } else {
            trainerVideoStatus = "Recording canceled."
        }
    }

    fun recordTrainerVideo() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_DURATION_LIMIT, 90)
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            trainerVideoStatus = "No camera app is available for recording."
        } else {
            trainerVideoLauncher.launch(intent)
        }
    }

    fun shareTrainerProgress() {
        val uri = trainerVideoUri?.let { Uri.parse(it) }
        val shareIntent = if (uri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "LUNAKAI fitness progress: ${activeWorkout.title}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "LUNAKAI fitness progress: ${activeWorkout.title} - ${formatDuration(totalSeconds)} total movement.")
            }
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share fitness progress"))
    }

    GradientBackground {
        ScreenScroll {
            WellnessBackButton(onBack)
            SectionHeader("Fitness", "Track movement and choose free workouts.")

            GlassCard(background = CardAccent.copy(alpha = 0.94f)) {
                Text("Today tracker", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Active workout: ${activeWorkout.title}", color = TextMuted)
                Text(formatDuration(sessionSeconds), color = TextDark, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                SoftStatusChip(if (timerRunning) "Stopwatch running" else "Ready to start", if (timerRunning) SuccessGreen else GoldAccent)
                FitnessProgressBar(progress)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FitnessMetric("Minutes", minutesToday.toString(), Modifier.weight(1f))
                    FitnessMetric("Done", workoutsCompleted.toString(), Modifier.weight(1f))
                    FitnessMetric("Streak", "$streakDays d", Modifier.weight(1f))
                }
                Text("Daily movement goal: ${goalMinutes.toInt()} min", color = TextMuted)
                Slider(value = goalMinutes, onValueChange = onGoalChange, valueRange = 5f..90f)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondarySoftButton("Start Active", modifier = Modifier.weight(1f), onClick = onStartActive)
                    SecondarySoftButton("+5 min", modifier = Modifier.weight(1f), onClick = onAddFiveMinutes)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondarySoftButton("Progress", modifier = Modifier.weight(1f), onClick = onProgress)
                    SecondarySoftButton("Reset", modifier = Modifier.weight(1f), onClick = onReset)
                }
                PrimaryGradientButton("Mark Active Workout Complete") { onCompleteWorkout(activeWorkout.minutes) }
                MiniStateCard("Last completed", lastWorkoutTitle)
                MiniStateCard("Total timed movement", formatDuration(totalSeconds))
                Text("This tracker is for general wellness and motivation, not medical advice.", color = TextMuted, fontSize = 12.sp)
            }

            GlassCard {
                Text("AI trainer check-in", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Record a set, walk-through, or progress clip so a trainer mode can review form notes later and you can share progress.", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
                MiniStateCard("Video sample", trainerVideoStatus)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondarySoftButton("Record Video", modifier = Modifier.weight(1f), onClick = { recordTrainerVideo() })
                    SecondarySoftButton("Share Progress", modifier = Modifier.weight(1f), onClick = { shareTrainerProgress() })
                }
                SecondarySoftButton("Attach to Trainer Review", onClick = {
                    trainerVideoStatus = if (trainerVideoUri != null) {
                        "Video attached for the next trainer review."
                    } else {
                        "Record a video before attaching it to trainer review."
                    }
                })
            }

            SectionHeader("Free workouts", "No equipment needed. Choose what fits your energy.")
            workouts.forEach { workout ->
                FitnessWorkoutCard(
                    workout = workout,
                    selected = activeWorkout.title == workout.title,
                    onStart = { onSelectWorkout(workout.title) },
                )
            }
        }
    }
}

@Composable
private fun FitnessProgressBar(progress: Float) {
    val appearance = LocalAppAppearance.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(PillShape)
            .background(appearance.control),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(14.dp)
                .clip(PillShape)
                .background(Brush.horizontalGradient(listOf(appearance.accentStart, appearance.accentMiddle, appearance.accentEnd))),
        )
    }
}

@Composable
private fun FitnessMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val appearance = LocalAppAppearance.current
    Column(
        modifier = modifier
            .clip(MediumShape)
            .background(appearance.control)
            .border(1.dp, appearance.border, MediumShape)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = appearance.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = appearance.mutedText, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun FitnessWorkoutCard(workout: FreeWorkout, selected: Boolean, onStart: () -> Unit) {
    val appearance = LocalAppAppearance.current
    GlassCard(background = if (selected) appearance.elevatedCard else Color.Unspecified) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SoftIcon("fitness", if (selected) appearance.accentMiddle else GoldAccent)
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.title, color = appearance.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${workout.level} - ${workout.minutes} min - ${workout.focus}", color = appearance.mutedText, fontSize = 13.sp)
            }
            if (selected) SoftStatusChip("Active", appearance.accentMiddle)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            workout.moves.forEach { move -> SoftFeatureChip(move) }
        }
        SecondarySoftButton(if (selected) "Active Workout" else "Set Active", onClick = onStart)
    }
}

@Composable
private fun FitnessProgressScreen(
    onBack: () -> Unit,
    activeWorkoutTitle: String,
    timerRunning: Boolean,
    sessionSeconds: Int,
    minutesToday: Int,
    workoutsCompleted: Int,
    streakDays: Int,
    goalMinutes: Float,
    totalSeconds: Int,
    lastWorkoutTitle: String,
) {
    val progress = (minutesToday / goalMinutes.coerceAtLeast(1f)).coerceIn(0f, 1f)
    GradientBackground {
        ScreenScroll {
            WellnessBackButton(onBack)
            SectionHeader("Fitness Progress", "Your movement tracker and workout history summary.")
            GlassCard(background = CardAccent.copy(alpha = 0.94f)) {
                Text("Current session", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(activeWorkoutTitle, color = TextMuted)
                Text(formatDuration(sessionSeconds), color = TextDark, fontSize = 38.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                SoftStatusChip(if (timerRunning) "Workout in progress" else "No active timer", if (timerRunning) SuccessGreen else GoldAccent)
            }
            GlassCard {
                Text("Today", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                FitnessProgressBar(progress)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FitnessMetric("Minutes", minutesToday.toString(), Modifier.weight(1f))
                    FitnessMetric("Goal", goalMinutes.toInt().toString(), Modifier.weight(1f))
                    FitnessMetric("Done", workoutsCompleted.toString(), Modifier.weight(1f))
                }
                MiniStateCard("Goal progress", "${(progress * 100).toInt()}%")
                MiniStateCard("Current streak", "$streakDays day")
            }
            GlassCard {
                Text("History summary", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                MiniStateCard("Last completed", lastWorkoutTitle)
                MiniStateCard("Total timed movement", formatDuration(totalSeconds))
                MiniStateCard("Total completed workouts", workoutsCompleted.toString())
                Text("Future versions can sync this to Firestore under users/{userId}/fitnessProgress.", color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    fun two(value: Int) = value.toString().padStart(2, '0')
    return "${two(hours)}:${two(minutes)}:${two(seconds)}"
}

@Composable
private fun MemoryScreen() {
    val categories = listOf(
        MemoryCategory("Personal Preferences", "Names, favorites, comfort style, and boundaries.", true),
        MemoryCategory("Companion Style", "How your companion should speak and show up.", true),
        MemoryCategory("Emotional Patterns", "Recurring feelings, triggers, and support needs.", true),
        MemoryCategory("Journal Insights", "Themes from private journal entries.", false),
        MemoryCategory("Saved Reflections", "Reflections you choose to keep.", true),
    )

    GradientBackground {
        ScreenScroll {
            SectionHeader("Memory controls", "Control what your companion remembers about you.")
            GlassCard { Text("Memories help your companion personalize support, remember your preferences, and understand your journey.", color = TextMuted) }
            categories.forEach { MemoryCategoryCard(it) }
            GlassCard(background = WarningPeach.copy(alpha = 0.36f)) {
                Text("Clear Memory Fields", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Choose what you want your companion to forget.", color = TextMuted)
                SecondarySoftButton("Clear Companion Style", onClick = {})
                SecondarySoftButton("Clear Journal Insights", onClick = {})
                SecondarySoftButton("Clear Emotional Patterns", onClick = {})
                SecondarySoftButton("Clear All Memory", onClick = {})
            }
        }
    }
}

@Composable
private fun CompanionStateScreen(companion: CompanionProfile, onNavigate: (AppRoute) -> Unit) {
    val experience = calculateCompanionExperience(companion)
    val supportMode = "${experience.characterMode} - ${experience.primarySupportFocus}"
    val stateMessage = experience.stateMessage

    GradientBackground {
        ScreenScroll {
            SectionHeader("Active companion", "Your companion adjusts to how you need support.")
            GlassCard(background = CardAccent.copy(alpha = 0.96f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CompanionAvatar(
                        size = 150.dp,
                        label = companion.name.take(2),
                        glow = true,
                        photoUri = companion.photoUri,
                        imageResId = companion.imageResId,
                    )
                    Text(companion.name, color = TextDark, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    SoftStatusChip("Currently: $supportMode", AccentPink)
                    Text(stateMessage, color = TextMuted, textAlign = TextAlign.Center, lineHeight = 22.sp, modifier = Modifier.padding(top = 12.dp))
                }
            }
            PrimaryGradientButton("View My Companions", onClick = { onNavigate(AppRoute.Companions) })
            GlassCard {
                MiniStateCard("Tone", companion.communicationStyle)
                MiniStateCard("Voice", companion.voice)
                MiniStateCard("Identity", companion.gender)
                MiniStateCard("Mock profile", if (companion.isMock) "Built-in ${companion.imageResName}" else "Custom")
                MiniStateCard("Image source", if (companion.photoUri != null) "Uploaded photo" else companion.imageResName.ifBlank { "Orb placeholder" })
                Text("Support focus", color = TextMuted)
                SupportFocusChips(companion.supportFocus)
            }
            SecondarySoftButton("Change Personality", modifier = Modifier.fillMaxWidth(), onClick = { onNavigate(AppRoute.Personality) })
            PrimaryGradientButton("Start Chat", onClick = { onNavigate(AppRoute.TextChat) })
            SecondarySoftButton("Open Live Call", onClick = { onNavigate(AppRoute.LiveCompanionCall) })
        }
    }
}

@Composable
private fun PreferencesScreen(
    companion: CompanionProfile,
    defaultJournalStorage: JournalStorage,
    defaultChatStorage: ChatStorage,
    askStorageEveryTime: Boolean,
    hasAcceptedDisclaimer: Boolean,
    hideDisclaimerOnLaunch: Boolean,
    showDisclaimerOnLaunch: Boolean,
    onStorageChange: (JournalStorage) -> Unit,
    onChatStorageChange: (ChatStorage) -> Unit,
    onAskEveryTimeChange: (Boolean) -> Unit,
    onHideDisclaimerChange: (Boolean) -> Unit,
    onShowDisclaimerChange: (Boolean) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onProfileInfo: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    var voiceJournalEnabled by remember { mutableStateOf(context.prefBoolean("settings_voice_journal_enabled", true)) }
    var memoryEnabled by remember { mutableStateOf(context.prefBoolean("settings_memory_enabled", true)) }
    var appLock by remember { mutableStateOf(context.prefBoolean("settings_app_lock", false)) }
    var hidePreviews by remember { mutableStateOf(context.prefBoolean("settings_hide_previews", true)) }
    var cloudSync by remember { mutableStateOf(context.prefBoolean("settings_cloud_sync", false)) }
    var videoJournalEnabled by remember { mutableStateOf(context.prefBoolean("settings_video_journal_enabled", true)) }
    var groundingEnabled by remember { mutableStateOf(context.prefBoolean("settings_grounding_enabled", true)) }
    var breathingEnabled by remember { mutableStateOf(context.prefBoolean("settings_breathing_enabled", true)) }
    var affirmationsEnabled by remember { mutableStateOf(context.prefBoolean("settings_affirmations_enabled", true)) }
    var reflectionPromptsEnabled by remember { mutableStateOf(context.prefBoolean("settings_reflection_prompts_enabled", true)) }
    var checkInRemindersEnabled by remember { mutableStateOf(context.prefBoolean("settings_check_in_reminders_enabled", false)) }
    var defaultWellnessTool by remember { mutableStateOf(context.prefString("settings_default_wellness_tool", "Grounding")) }
    var checkInStyle by remember { mutableStateOf(context.prefString("settings_check_in_style", "Gentle")) }
    var reminderFrequency by remember { mutableStateOf(context.prefString("settings_reminder_frequency", "Evening")) }
    var companionNotificationsEnabled by remember { mutableStateOf(context.prefBoolean("settings_companion_notifications_enabled", true)) }
    var notificationStyle by remember { mutableStateOf(context.prefString("settings_notification_style", "Calculated from character preferences")) }
    var notificationUrgency by remember { mutableStateOf(context.prefString("settings_notification_urgency", "Calculated from character preferences")) }
    var wakeAssistantId by remember {
        mutableStateOf(
            context.prefString("voice_wake_assistant_id", wakeAssistantIdFor(companion))
                .takeIf { it == "luna" || it == "kai" }
                ?: wakeAssistantIdFor(companion),
        )
    }
    fun wakeSettingKey(name: String) = "voice_wake_${wakeAssistantId}_$name"
    val wakePhrase = wakePhraseFor(wakeAssistantId)
    var wakePhraseEnabled by remember(wakeAssistantId) { mutableStateOf(context.prefBoolean(wakeSettingKey("wakePhraseEnabled"), false)) }
    var wakeAudioSource by remember(wakeAssistantId) { mutableStateOf(context.prefString(wakeSettingKey("audioSource"), "Phone microphone only when selected")) }
    val accountEmail = currentFirebaseEmail() ?: "Not available"
    val wakeAudioOptions = remember(context) { availableAudioInputSources(context) }
    var openSettingsSection by remember { mutableStateOf<String?>(null) }
    fun toggleSection(section: String) {
        openSettingsSection = if (openSettingsSection == section) null else section
    }

    LaunchedEffect(wakeAudioOptions) {
        if (wakeAudioSource !in wakeAudioOptions) wakeAudioSource = wakeAudioOptions.first()
    }

    LaunchedEffect(
        voiceJournalEnabled,
        memoryEnabled,
        appLock,
        hidePreviews,
        cloudSync,
        videoJournalEnabled,
        groundingEnabled,
        breathingEnabled,
        affirmationsEnabled,
        reflectionPromptsEnabled,
        checkInRemindersEnabled,
        defaultWellnessTool,
        checkInStyle,
        reminderFrequency,
        companionNotificationsEnabled,
        notificationStyle,
        notificationUrgency,
        wakeAssistantId,
        wakePhraseEnabled,
        wakeAudioSource,
    ) {
        context.savePref("settings_voice_journal_enabled", voiceJournalEnabled)
        context.savePref("settings_memory_enabled", memoryEnabled)
        context.savePref("settings_app_lock", appLock)
        context.savePref("settings_hide_previews", hidePreviews)
        context.savePref("settings_cloud_sync", cloudSync)
        context.savePref("settings_video_journal_enabled", videoJournalEnabled)
        context.savePref("settings_grounding_enabled", groundingEnabled)
        context.savePref("settings_breathing_enabled", breathingEnabled)
        context.savePref("settings_affirmations_enabled", affirmationsEnabled)
        context.savePref("settings_reflection_prompts_enabled", reflectionPromptsEnabled)
        context.savePref("settings_check_in_reminders_enabled", checkInRemindersEnabled)
        context.savePref("settings_default_wellness_tool", defaultWellnessTool)
        context.savePref("settings_check_in_style", checkInStyle)
        context.savePref("settings_reminder_frequency", reminderFrequency)
        context.savePref("settings_companion_notifications_enabled", companionNotificationsEnabled)
        context.savePref("settings_notification_style", notificationStyle)
        context.savePref("settings_notification_urgency", notificationUrgency)
        context.savePref("voice_wake_assistant_id", wakeAssistantId)
        context.savePref(wakeSettingKey("wakePhraseEnabled"), wakePhraseEnabled)
        context.savePref(wakeSettingKey("audioSource"), wakeAudioSource)
    }

    GradientBackground {
        ScreenScroll {
            SectionHeader("Settings", "Tap a category to view or change its options.")

            SettingsAccordionSection(
                title = "Account",
                subtitle = "Signed in as: $accountEmail",
                expanded = openSettingsSection == "Account",
                onToggle = { toggleSection("Account") },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondarySoftButton("Profile Info", modifier = Modifier.weight(1f), onClick = onProfileInfo)
                    SecondarySoftButton("Sign Out", modifier = Modifier.weight(1f), onClick = onSignOut)
                }
                SecondarySoftButton("Manage Account", onClick = { onNavigate(AppRoute.AccountSettings) })
            }

            SettingsAccordionSection(
                title = "Active Companion",
                subtitle = "Current companion: ${companion.name}",
                expanded = openSettingsSection == "Active Companion",
                onToggle = { toggleSection("Active Companion") },
            ) {
                MiniStateCard("Current companion", companion.name)
                MiniStateCard("Voice", companion.voice)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Change", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Companions) })
                    SecondarySoftButton("Create New", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Personality) })
                }
                SecondarySoftButton("Manage Saved Companions", onClick = { onNavigate(AppRoute.Companions) })
                SecondarySoftButton("View Companion State", onClick = { onNavigate(AppRoute.CompanionState) })
            }

            SettingsAccordionSection(
                title = "Companion Customization",
                subtitle = "${companion.gender} · ${companion.communicationStyle} · ${companion.avatarType}",
                expanded = openSettingsSection == "Companion Customization",
                onToggle = { toggleSection("Companion Customization") },
            ) {
                MiniStateCard("Companion photo", companion.avatarType)
                MiniStateCard("Identity", companion.gender)
                MiniStateCard("Roleplay", if (companion.roleplayEnabled) "Enabled" else "Disabled")
                MiniStateCard("Communication style", companion.communicationStyle)
                SecondarySoftButton("Edit Companion", onClick = { onNavigate(AppRoute.Personality) })
            }

            SettingsAccordionSection(
                title = "Journal Settings",
                subtitle = "Default save: ${defaultJournalStorage.label}",
                expanded = openSettingsSection == "Journal Settings",
                onToggle = { toggleSection("Journal Settings") },
            ) {
                MiniStateCard("Default save location", defaultJournalStorage.label)
                ToggleRow("Ask every time", askStorageEveryTime, onAskEveryTimeChange)
                ToggleRow("Voice journal enabled", voiceJournalEnabled) { voiceJournalEnabled = it }
                ToggleRow("Video journal enabled", videoJournalEnabled) { videoJournalEnabled = it }
                MiniStateCard("Saved entries location", "Journal Entries")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Device", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Device) })
                    SecondarySoftButton("Cloud", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Cloud) })
                    SecondarySoftButton("Both", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Both) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Entries", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.SavedJournalEntries) })
                    SecondarySoftButton("Export", modifier = Modifier.weight(1f), onClick = {})
                }
            }

            SettingsAccordionSection(
                title = "Chat Settings",
                subtitle = "Messages save: ${defaultChatStorage.label}",
                expanded = openSettingsSection == "Chat Settings",
                onToggle = { toggleSection("Chat Settings") },
            ) {
                Text("Choose where text chat messages are stored after Gemini replies.", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
                ChatStorage.entries.forEach { storage ->
                    SelectableInfoCard(
                        title = storage.label,
                        body = storage.body,
                        selected = defaultChatStorage == storage,
                    ) {
                        onChatStorageChange(storage)
                    }
                }
            }

            SettingsAccordionSection(
                title = "Wellness Settings",
                subtitle = "$defaultWellnessTool · reminders $reminderFrequency",
                expanded = openSettingsSection == "Wellness Settings",
                onToggle = { toggleSection("Wellness Settings") },
            ) {
                SoftDropdown(
                    label = "Default wellness tool",
                    selected = defaultWellnessTool,
                    options = listOf("Grounding", "Breathing", "Fitness", "Reflection", "Affirmations", "Check-In"),
                    onSelected = { defaultWellnessTool = it },
                )
                SoftDropdown(
                    label = "Check-in style",
                    selected = checkInStyle,
                    options = listOf("Gentle", "Direct", "Motivational", "Reflective"),
                    onSelected = { checkInStyle = it },
                )
                SoftDropdown(
                    label = "Reminder time",
                    selected = reminderFrequency,
                    options = listOf("Morning", "Afternoon", "Evening", "Night", "Off"),
                    onSelected = { reminderFrequency = it },
                )
                ToggleRow("Grounding tools enabled", groundingEnabled) { groundingEnabled = it }
                ToggleRow("Breathing reset enabled", breathingEnabled) { breathingEnabled = it }
                ToggleRow("Affirmations enabled", affirmationsEnabled) { affirmationsEnabled = it }
                ToggleRow("Reflection prompts enabled", reflectionPromptsEnabled) { reflectionPromptsEnabled = it }
                ToggleRow("Check-in reminders", checkInRemindersEnabled) { checkInRemindersEnabled = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondarySoftButton("Open Wellness", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Wellness) })
                    SecondarySoftButton("Crisis Help", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.CrisisResources) })
                }
                SecondarySoftButton("Wellness Disclaimer", onClick = { onNavigate(AppRoute.Disclaimer) })
            }

            SettingsAccordionSection(
                title = "Memory Settings",
                subtitle = if (memoryEnabled) "Memory enabled" else "Memory off",
                expanded = openSettingsSection == "Memory Settings",
                onToggle = { toggleSection("Memory Settings") },
            ) {
                ToggleRow("Memory enabled", memoryEnabled) { memoryEnabled = it }
                SecondarySoftButton("Manage Memories", onClick = { onNavigate(AppRoute.Memory) })
                SecondarySoftButton("Clear Selected Memory Fields", onClick = {})
                SecondarySoftButton("Clear All Memory", onClick = {})
            }

            SettingsAccordionSection(
                title = "Privacy Settings",
                subtitle = "App lock, previews, cloud sync, and data controls",
                expanded = openSettingsSection == "Privacy Settings",
                onToggle = { toggleSection("Privacy Settings") },
            ) {
                ToggleRow("App lock", appLock) { appLock = it }
                ToggleRow("Hide sensitive previews", hidePreviews) { hidePreviews = it }
                ToggleRow("Cloud sync", cloudSync) { cloudSync = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Export Data", modifier = Modifier.weight(1f), onClick = {})
                    SecondarySoftButton("Delete Data", modifier = Modifier.weight(1f), onClick = {})
                }
            }

            SettingsAccordionSection(
                title = "Companion Notifications",
                subtitle = "$notificationStyle · $notificationUrgency",
                expanded = openSettingsSection == "Companion Notifications",
                onToggle = { toggleSection("Companion Notifications") },
            ) {
                ToggleRow("Allow companion to notify me", companionNotificationsEnabled) { companionNotificationsEnabled = it }
                SoftDropdown(
                    label = "Style",
                    selected = notificationStyle,
                    options = listOf("Calculated from character preferences", "Soft and supportive", "Direct and clear", "Motivational", "Playful", "Protective"),
                    onSelected = { notificationStyle = it },
                )
                SoftDropdown(
                    label = "Urgency",
                    selected = notificationUrgency,
                    options = listOf("Calculated from character preferences", "Soft only", "Normal", "High urgency"),
                    onSelected = { notificationUrgency = it },
                )
                Text("LUNAKAI uses these choices in the background with the active companion's traits, support focus, communication style, and character mode so notifications feel personal without showing the calculation here.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
                SecondarySoftButton("Advanced Notification Settings", onClick = { onNavigate(AppRoute.NotificationSettings) })
            }

            SettingsAccordionSection(
                title = "Voice Wake Phrase",
                subtitle = "$wakePhrase - $wakeAudioSource",
                expanded = openSettingsSection == "Voice Wake Phrase",
                onToggle = { toggleSection("Voice Wake Phrase") },
            ) {
                SoftDropdown(
                    label = "Wake assistant",
                    selected = wakeAssistantName(wakeAssistantId),
                    options = listOf("Luna", "Kai"),
                    onSelected = { selected -> wakeAssistantId = selected.lowercase() },
                )
                ToggleRow("Enable $wakePhrase", wakePhraseEnabled) { wakePhraseEnabled = it }
                MiniStateCard("Wake phrase", wakePhrase)
                SoftDropdown(
                    label = "Audio input source",
                    selected = wakeAudioSource,
                    options = wakeAudioOptions,
                    onSelected = { wakeAudioSource = it },
                )
                Text("Only Luna and Kai can own wake phrases and Bluetooth wake routing. Custom companions keep their chat voice but do not register a wake phrase.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
                SecondarySoftButton("Voice Controls", onClick = { onNavigate(AppRoute.VoiceLiveSettings) })
            }

            SettingsAccordionSection(
                title = "Appearance",
                subtitle = "Theme, colors, cards, and avatar style",
                expanded = openSettingsSection == "Appearance",
                onToggle = { toggleSection("Appearance") },
            ) {
                MiniStateCard("Theme", "Soft luxury gradient")
                MiniStateCard("Accent style", "Rose and lavender")
                MiniStateCard("Card style", "Glassy rounded panels")
                MiniStateCard("Avatar/orb style", companion.avatarType)
                SecondarySoftButton("Open Appearance", onClick = { onNavigate(AppRoute.AppearanceSettings) })
            }

            SettingsAccordionSection(
                title = "Support & Safety",
                subtitle = "Disclaimer, crisis resources, and launch safety",
                expanded = openSettingsSection == "Support & Safety",
                onToggle = { toggleSection("Support & Safety") },
                background = WarningPeach.copy(alpha = 0.20f),
            ) {
                MiniStateCard("Disclaimer accepted", if (hasAcceptedDisclaimer) "Yes" else "No")
                ToggleRow("Show disclaimer on launch", showDisclaimerOnLaunch, onShowDisclaimerChange)
                ToggleRow("Do not show every time", hideDisclaimerOnLaunch, onHideDisclaimerChange)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Disclaimer", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Disclaimer) })
                    SecondarySoftButton("Crisis Resources", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.CrisisResources) })
                }
            }
        }
    }
}

@Composable
private fun SettingsAccordionSection(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    background: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassCard(padding = 0.dp, background = background) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = TextMuted, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    if (expanded) "Hide" else "Open",
                    color = DeepRose,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (expanded) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ProfileInfoScreen(
    profilePhotoUri: String?,
    displayName: String,
    email: String,
    birthday: String,
    preferredName: String,
    pronouns: String,
    location: String,
    aboutMe: String,
    wellnessPreferences: String,
    defaultCompanion: String,
    notificationPreferences: String,
    cloudSyncPreference: Boolean,
    onProfilePhotoChange: (String?) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onBirthdayChange: (String) -> Unit,
    onPreferredNameChange: (String) -> Unit,
    onPronounsChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onAboutMeChange: (String) -> Unit,
    onWellnessPreferencesChange: (String) -> Unit,
    onNotificationPreferencesChange: (String) -> Unit,
    onCloudSyncPreferenceChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onProfilePhotoChange(it.toString())
        }
    }

    GradientBackground {
        ScreenScroll {
            GlassCard(background = CardAccent.copy(alpha = 0.92f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    ProfilePhotoButton(size = 132.dp, photoUri = profilePhotoUri, onClick = { photoPicker.launch(arrayOf("image/*")) })
                    Text(displayName.ifBlank { "Your profile" }, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    Text(email, color = TextMuted, textAlign = TextAlign.Center)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Upload Photo", modifier = Modifier.weight(1f), onClick = { photoPicker.launch(arrayOf("image/*")) })
                    SecondarySoftButton("Remove Photo", modifier = Modifier.weight(1f), onClick = { onProfilePhotoChange(null) })
                }
            }
            GlassCard {
                Text("Personal information", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                RoundedInputField(displayName, onDisplayNameChange, "Display name")
                RoundedInputField(preferredName, onPreferredNameChange, "Preferred name")
                RoundedInputField(pronouns, onPronounsChange, "Pronouns optional")
                RoundedInputField(birthday, onBirthdayChange, "Birthday / age optional")
                RoundedInputField(location, onLocationChange, "Location optional")
                RoundedInputField(aboutMe, onAboutMeChange, "About me", minLines = 3)
            }
            GlassCard {
                Text("Preferences", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                RoundedInputField(wellnessPreferences, onWellnessPreferencesChange, "Wellness preferences", minLines = 2)
                MiniStateCard("Default companion", defaultCompanion)
                RoundedInputField(notificationPreferences, onNotificationPreferencesChange, "Notification preferences", minLines = 2)
                ToggleRow("Cloud sync preference", cloudSyncPreference, onCloudSyncPreferenceChange)
                Text("Firestore-ready path: users/{userId}/profile", color = TextMuted, fontSize = 12.sp)
            }
            PrimaryGradientButton("Save Profile", onClick = onSave)
        }
    }
}

@Composable
private fun CustomizeCompanionScreen(
    baseCompanion: CompanionProfile,
    onSave: (CompanionProfile, Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    val roleOptions = listOf("Best Friend", "Romantic Partner", "Assistant", "Motivator", "Wellness Coach", "Athletic Partner", "Creative Muse", "Practice Partner", "Fantasy Character", "Custom")
    val characteristicOptions = listOf("Supportive", "Protective", "Playful", "Funny", "Calm", "Confident", "Mysterious", "Romantic", "Motivational", "Honest", "Soft-spoken", "Bold", "Intellectual", "Creative", "Grounded", "Charming", "Loyal", "Patient", "Adventurous", "Disciplined")
    val traitOptions = listOf("Kind", "Sweet", "Ambitious", "Provider", "Jealous", "Clingy", "Rude", "Gentle", "Direct", "Flirty", "Soothing", "Deep", "Protective", "Playful", "Funny")
    val zodiacOptions = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces", "No sign preference")
    val energyOptions = listOf("Gentle", "Direct", "Deep", "Flirty", "Motivational", "Soothing", "Playful", "Protective", "Calm", "Confident")
    val interactionOptions = listOf("Daily check-ins", "Motivation", "Journaling support", "Workout timer partner", "Roleplay rehearsal", "Script practice", "Creative brainstorming", "Confidence building", "Relationship reflection", "Grounding support")

    var name by remember { mutableStateOf("") }
    var shortDescription by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Best Friend") }
    var backstory by remember { mutableStateOf("") }
    var vibe by remember { mutableStateOf("") }
    var characteristics by remember { mutableStateOf(setOf<String>()) }
    var traits by remember { mutableStateOf(setOf<String>()) }
    var traitMessage by remember { mutableStateOf<String?>(null) }
    var zodiac by remember { mutableStateOf("No sign preference") }
    var communicationEnergy by remember { mutableStateOf(setOf("Gentle")) }
    var interactionStyle by remember { mutableStateOf(setOf<String>()) }
    var aiPrompt by remember { mutableStateOf("") }
    var showAiHelper by remember { mutableStateOf(false) }
    var generatedBio by remember { mutableStateOf<String?>(null) }

    fun buildCompanion(): CompanionProfile {
        val finalName = name.ifBlank { generatedBio?.substringBefore(":")?.ifBlank { "Nova" } ?: "Nova" }
        val finalTraits = traits.ifEmpty { setOf("Kind", "Creative") }.toList()
        val styles = communicationEnergy.ifEmpty { setOf("Gentle") }.toList()
        return baseCompanion.copy(
            name = finalName,
            gender = "Custom",
            voice = if ("Deep" in styles || "Direct" in styles) "Neutral Calm" else "Soft Female",
            personalityTags = characteristics.ifEmpty { setOf("Supportive", "Creative") }.take(4),
            personalityTraits = finalTraits,
            communicationStyle = styles.first(),
            communicationStyles = styles,
            characterMode = role,
            shortDescription = shortDescription.ifBlank { generatedBio ?: "A custom companion shaped around your style and support needs." },
            supportFocus = interactionStyle.mapNotNull {
                when {
                    it.contains("Journal", ignoreCase = true) -> "Journaling"
                    it.contains("Ground", ignoreCase = true) -> "Grounding"
                    it.contains("Confidence", ignoreCase = true) -> "Confidence"
                    it.contains("Relationship", ignoreCase = true) -> "Relationships"
                    it.contains("Creative", ignoreCase = true) -> "Creativity"
                    else -> null
                }
            }.ifEmpty { listOf("Reflection", "Journaling") }.toSet(),
            backstory = backstory.ifBlank { null },
            zodiacSign = zodiac,
            characteristics = characteristics.toList(),
            aiGeneratedBio = generatedBio,
            isMock = false,
        )
    }

    GradientBackground {
        ScreenScroll {
            SectionHeader("Character concept", "Build a companion with your own style, energy, and personality.")
            GlassCard {
                RoundedInputField(name, { name = it }, "Character name")
                RoundedInputField(shortDescription, { shortDescription = it }, "Short description", minLines = 2)
                SoftDropdown("Character role", role, roleOptions) { role = it }
                RoundedInputField(backstory, { backstory = it }, "Backstory idea", minLines = 3)
                RoundedInputField(vibe, { vibe = it }, "Vibe / aesthetic", minLines = 2)
            }
            SectionHeader("Characteristics")
            GlassCard {
                CheckboxOptionGrid(characteristicOptions, characteristics, onToggle = { option ->
                    characteristics = if (option in characteristics) characteristics - option else characteristics + option
                })
            }
            SectionHeader("Personality traits", "Choose up to 4.")
            GlassCard {
                CheckboxOptionGrid(traitOptions, traits, onToggle = { option ->
                    if (option in traits) {
                        traits = traits - option
                        traitMessage = null
                    } else if (traits.size >= 4) {
                        traitMessage = "You can choose up to 4 personality traits."
                    } else {
                        traits = traits + option
                        traitMessage = null
                    }
                })
                traitMessage?.let { Text(it, color = WarningPeach, fontWeight = FontWeight.SemiBold) }
            }
            SectionHeader("Astrological energy")
            GlassCard {
                SoftDropdown("Companion zodiac energy", zodiac, zodiacOptions) { zodiac = it }
            }
            SectionHeader("Communication energy")
            GlassCard {
                CheckboxOptionGrid(energyOptions, communicationEnergy, onToggle = { option ->
                    communicationEnergy = if (option in communicationEnergy) communicationEnergy - option else communicationEnergy + option
                })
            }
            SectionHeader("Role / interaction style")
            GlassCard {
                CheckboxOptionGrid(interactionOptions, interactionStyle, onToggle = { option ->
                    interactionStyle = if (option in interactionStyle) interactionStyle - option else interactionStyle + option
                })
            }
            GlassCard(background = CardAccent.copy(alpha = 0.72f)) {
                Text("AI help me build this", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Tell me the kind of companion you want, and I'll help shape the personality, voice, style, and support focus.", color = TextMuted)
                SecondarySoftButton("Help Me Build My Companion", onClick = { showAiHelper = true })
                generatedBio?.let {
                    Text("Generated preview", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Text(it, color = TextMuted, lineHeight = 20.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondarySoftButton("Apply This Companion", modifier = Modifier.weight(1f), onClick = {
                            if (name.isBlank()) name = "Sol"
                            if (shortDescription.isBlank()) shortDescription = it
                        })
                        SecondarySoftButton("Regenerate", modifier = Modifier.weight(1f), onClick = { showAiHelper = true })
                    }
                }
            }
            GlassCard {
                PrimaryGradientButton("Save and Set Active", onClick = { onSave(buildCompanion(), true) })
                SecondarySoftButton("Save Companion", onClick = { onSave(buildCompanion(), false) })
                SecondarySoftButton("Cancel", onClick = onCancel)
            }
        }
    }

    if (showAiHelper) {
        AlertDialog(
            onDismissRequest = { showAiHelper = false },
            containerColor = CardDark,
            title = { Text("Help Me Build My Companion", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Describe your ideal companion...", color = TextMuted)
                    RoundedInputField(aiPrompt, { aiPrompt = it }, "Describe your ideal companion", minLines = 4)
                    PrimaryGradientButton("Generate Companion Idea") {
                        val prompt = aiPrompt.ifBlank { "calm, loyal, creative, emotionally supportive companion" }
                        generatedBio = "Sol: $prompt. Mode: $role. Traits: ${traits.ifEmpty { setOf("Kind", "Loyal") }.joinToString(", ")}. Style: ${communicationEnergy.joinToString(", ").ifBlank { "Gentle" }}. Voice: Neutral Calm. Opening: I'm here with you. Tell me where you want to begin."
                        showAiHelper = false
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAiHelper = false }) { Text("Edit More", color = DeepRose) } },
        )
    }
}

@Composable
private fun AccountSettingsScreen(onSignOut: () -> Unit) {
    val accountEmail = currentFirebaseEmail() ?: "Not available"
    GradientBackground {
        ScreenScroll {
            SectionHeader("Account", "Manage sign-in, account access, and sign out.")
            GlassCard {
                Text("Signed in as", color = TextMuted)
                Text(accountEmail, color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                SecondarySoftButton("Manage Account", onClick = {})
                SecondarySoftButton("Sign Out", onClick = onSignOut)
            }
            GlassCard {
                Text("Firebase Auth Ready", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("This screen is ready to connect to Firebase Auth for email, password, account deletion, and sign-out flows.", color = TextMuted)
            }
        }
    }
}

@Composable
private fun VoiceLiveSettingsScreen(
    companion: CompanionProfile,
    companions: List<CompanionProfile>,
    onSelectCompanion: (String) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onLiveSettings: () -> Unit,
) {
    val context = LocalContext.current
    fun settingKey(name: String) = "voice_${companion.id}_$name"
    var voiceReplies by remember(companion.id) { mutableStateOf(context.prefBoolean(settingKey("voiceReplies"), true)) }
    var microphone by remember(companion.id) { mutableStateOf(context.prefBoolean(settingKey("microphone"), true)) }
    var speaker by remember(companion.id) { mutableStateOf(context.prefBoolean(settingKey("speaker"), true)) }
    var voiceSpeed by remember(companion.id) { mutableStateOf(context.prefString(settingKey("voiceSpeed"), "Normal")) }
    var answerStyle by remember(companion.id) { mutableStateOf(context.prefString(settingKey("answerStyle"), "Answer with voice and text")) }
    var wakeAssistantId by remember(companion.id) {
        mutableStateOf(
            context.prefString("voice_wake_assistant_id", wakeAssistantIdFor(companion))
                .takeIf { it == "luna" || it == "kai" }
                ?: wakeAssistantIdFor(companion),
        )
    }
    fun wakeSettingKey(name: String) = "voice_wake_${wakeAssistantId}_$name"
    val wakePhrase = wakePhraseFor(wakeAssistantId)
    var wakePhraseEnabled by remember(wakeAssistantId) { mutableStateOf(context.prefBoolean(wakeSettingKey("wakePhraseEnabled"), false)) }
    var audioSource by remember(wakeAssistantId) { mutableStateOf(context.prefString(wakeSettingKey("audioSource"), "Phone microphone only when selected")) }
    var bluetoothAccess by remember(wakeAssistantId) { mutableStateOf(context.prefBoolean(wakeSettingKey("bluetoothAccess"), false)) }
    var voiceTrainingEnabled by remember(wakeAssistantId) { mutableStateOf(context.prefBoolean(wakeSettingKey("voiceTrainingEnabled"), false)) }
    var trainingStatus by remember(wakeAssistantId) { mutableStateOf(context.prefString(wakeSettingKey("trainingStatus"), "Not trained yet")) }
    var trainingSampleCount by remember(wakeAssistantId) { mutableStateOf(context.prefInt(wakeSettingKey("trainingSampleCount"), 0)) }
    var voiceSensitivity by remember(wakeAssistantId) { mutableStateOf(context.prefString(wakeSettingKey("voiceSensitivity"), "Balanced")) }
    val companionNames = companions.map { it.name }
    val voiceOptions = voiceOptionsFor(companion.gender)
    val audioInputOptions = remember(context) { availableAudioInputSources(context) }
    val voiceTrainingLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            trainingStatus = "Training canceled. Try again when ready."
            return@rememberLauncherForActivityResult
        }
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        if (spokenWakePhraseMatches(spoken, wakePhrase)) {
            val nextCount = (trainingSampleCount + 1).coerceAtMost(3)
            trainingSampleCount = nextCount
            voiceTrainingEnabled = true
            trainingStatus = if (nextCount >= 3) {
                "Trained with 3 wake phrase samples."
            } else {
                "Sample $nextCount of 3 captured."
            }
        } else {
            trainingStatus = "Try again. Heard: ${spoken.ifBlank { "nothing" }}."
        }
    }

    fun startVoiceTrainingSample() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, wakePhrase)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            trainingStatus = "Speech recognition is not available on this device."
        } else {
            voiceTrainingLauncher.launch(intent)
        }
    }

    LaunchedEffect(companion.id, companion.gender, companion.voice) {
        val correctedVoice = voiceForGender(companion.gender, companion.voice)
        if (correctedVoice != companion.voice) {
            onVoiceSelected(correctedVoice)
        }
    }

    LaunchedEffect(companion.id, audioInputOptions) {
        if (audioSource !in audioInputOptions) audioSource = audioInputOptions.first()
    }

    LaunchedEffect(
        companion.id,
        voiceReplies,
        microphone,
        speaker,
        voiceSpeed,
        answerStyle,
        wakeAssistantId,
        wakePhraseEnabled,
        audioSource,
        bluetoothAccess,
        voiceTrainingEnabled,
        trainingStatus,
        trainingSampleCount,
        voiceSensitivity,
    ) {
        context.savePref(settingKey("voiceReplies"), voiceReplies)
        context.savePref(settingKey("microphone"), microphone)
        context.savePref(settingKey("speaker"), speaker)
        context.savePref(settingKey("voiceSpeed"), voiceSpeed)
        context.savePref(settingKey("answerStyle"), answerStyle)
        context.savePref("voice_wake_assistant_id", wakeAssistantId)
        context.savePref(wakeSettingKey("wakePhraseEnabled"), wakePhraseEnabled)
        context.savePref(wakeSettingKey("audioSource"), audioSource)
        context.savePref(wakeSettingKey("bluetoothAccess"), bluetoothAccess)
        context.savePref(wakeSettingKey("voiceTrainingEnabled"), voiceTrainingEnabled)
        context.savePref(wakeSettingKey("trainingStatus"), trainingStatus)
        context.savePref(wakeSettingKey("trainingSampleCount"), trainingSampleCount)
        context.savePref(wakeSettingKey("voiceSensitivity"), voiceSensitivity)
    }

    GradientBackground {
        ScreenScroll {
            SectionHeader("Voice controls", "Choose companion voice, wake phrase, audio source, and voice training.")
            GlassCard {
                Text("Companion voice", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                SoftDropdown(
                    label = "Active companion",
                    selected = companion.name,
                    options = companionNames,
                    onSelected = { selectedName ->
                        companions.firstOrNull { it.name == selectedName }?.let { onSelectCompanion(it.id) }
                    },
                )
                SoftDropdown(
                    label = "${companion.name}'s voice",
                    selected = companion.voice,
                    options = voiceOptions,
                    onSelected = onVoiceSelected,
                )
                SoftDropdown(
                    label = "Voice speed",
                    selected = voiceSpeed,
                    options = listOf("Slow", "Normal", "Fast", "Custom Later"),
                    onSelected = { voiceSpeed = it },
                )
                ToggleRow("Enable voice replies", voiceReplies) { voiceReplies = it }
                ToggleRow("Enable microphone", microphone) { microphone = it }
                ToggleRow("Speaker output", speaker) { speaker = it }
            }
            GlassCard {
                Text("Wake phrase", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                SoftDropdown(
                    label = "Wake assistant",
                    selected = wakeAssistantName(wakeAssistantId),
                    options = listOf("Luna", "Kai"),
                    onSelected = { selected -> wakeAssistantId = selected.lowercase() },
                )
                ToggleRow("Listen for $wakePhrase", wakePhraseEnabled) { wakePhraseEnabled = it }
                MiniStateCard("Phrase", wakePhrase)
                Text("Wake phrase access is limited to Luna and Kai. Custom companions keep their own chat voice, but they do not register Bluetooth wake phrases.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            GlassCard {
                Text("Audio access", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                SoftDropdown(
                    label = "Audio input source",
                    selected = audioSource,
                    options = audioInputOptions,
                    onSelected = { audioSource = it },
                )
                ToggleRow("Allow Bluetooth audio devices", bluetoothAccess) { bluetoothAccess = it }
                MiniStateCard("Current route", audioSource)
                Text("Bluetooth wake routing is saved for ${wakeAssistantName(wakeAssistantId)} only. Turn on or pair Bluetooth audio first, then reopen this screen if it is not listed.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            GlassCard {
                Text("Voice training", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                ToggleRow("Recognize my voice", voiceTrainingEnabled) { voiceTrainingEnabled = it }
                MiniStateCard("Training phrase", wakePhrase)
                MiniStateCard("Samples", "$trainingSampleCount / 3")
                MiniStateCard("Training status", trainingStatus)
                SoftDropdown(
                    label = "Voice match sensitivity",
                    selected = voiceSensitivity,
                    options = listOf("Gentle", "Balanced", "Strict"),
                    onSelected = { voiceSensitivity = it },
                )
                SecondarySoftButton("Record Sample", onClick = { startVoiceTrainingSample() })
                SecondarySoftButton("Reset Training", onClick = {
                    trainingSampleCount = 0
                    voiceTrainingEnabled = false
                    trainingStatus = "Not trained yet"
                })
                Text("Training checks the spoken wake phrase through Android speech recognition and saves progress for Luna or Kai.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            GlassCard {
                Text("Answer behavior", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                SoftDropdown(
                    label = "How ${companion.name} answers",
                    selected = answerStyle,
                    options = listOf("Answer with voice and text", "Answer with voice only", "Answer with text only"),
                    onSelected = { answerStyle = it },
                )
            }
            GlassCard {
                Text("Video Chat settings", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Avatar type: ${companion.avatarType}", color = TextMuted)
                SecondarySoftButton("Open Video Chat", onClick = onLiveSettings)
                Text("Video Chat shows the companion visual and routes one active voice session at a time. Full photo-to-talking-avatar animation can be connected later.", color = TextMuted)
            }
        }
    }
}

@Composable
private fun JournalStorageSettingsScreen(
    defaultJournalStorage: JournalStorage,
    askStorageEveryTime: Boolean,
    onStorageChange: (JournalStorage) -> Unit,
    onAskEveryTimeChange: (Boolean) -> Unit,
) {
    GradientBackground {
        ScreenScroll {
            SectionHeader("Default save location", "Choose where entries are saved.")
            GlassCard {
                ToggleRow("Ask me every time", askStorageEveryTime, onAskEveryTimeChange)
                Text("When enabled, Save Entry opens a save-location sheet with Device, Cloud, or Both.", color = TextMuted)
            }
            JournalStorage.entries.forEach { storage ->
                SelectableInfoCard(storage.label, storage.body, selected = defaultJournalStorage == storage) {
                    onStorageChange(storage)
                }
            }
            GlassCard {
                Text("Storage structure", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Device: Room database later. Preferences: DataStore later. Cloud: users/{userId}/journalEntries/{entryId} in Firestore.", color = TextMuted)
            }
        }
    }
}

@Composable
private fun PrivacySettingsScreen() {
    var appLock by remember { mutableStateOf(false) }
    var hideSensitivePreviews by remember { mutableStateOf(true) }
    var cloudSync by remember { mutableStateOf(false) }

    GradientBackground {
        ScreenScroll {
            SectionHeader("Privacy controls", "Control app lock, sync, previews, and data export.")
            GlassCard {
                ToggleRow("App lock", appLock) { appLock = it }
                ToggleRow("Hide sensitive previews", hideSensitivePreviews) { hideSensitivePreviews = it }
                ToggleRow("Cloud sync", cloudSync) { cloudSync = it }
                SecondarySoftButton("Data export", onClick = {})
                SecondarySoftButton("Delete account data", onClick = {})
            }
        }
    }
}

@Composable
private fun NotificationSettingsScreen(companion: CompanionProfile) {
    var dailyPrompt by remember { mutableStateOf(true) }
    var checkIns by remember { mutableStateOf(true) }
    var journalReminders by remember { mutableStateOf(false) }
    var crisisNudges by remember { mutableStateOf(true) }
    var companionNotifications by remember { mutableStateOf(true) }
    var urgencyMode by remember { mutableStateOf("Calculated from character preferences") }
    var quietHours by remember { mutableStateOf(true) }
    var characterMode by remember(companion.id) { mutableStateOf(companion.characterMode) }
    var supportFocus by remember(companion.id) { mutableStateOf(companion.supportFocus.firstOrNull() ?: "Stress") }
    var notificationStyle by remember { mutableStateOf("Calculated from character preferences") }
    calculateCompanionExperience(
        companion = companion,
        characterModeOverride = characterMode,
        supportFocusOverride = supportFocus,
        urgencyOverride = urgencyMode,
    )

    GradientBackground {
        ScreenScroll {
            SectionHeader("Reminder controls", "Choose companion notifications, urgency, and gentle prompts.")
            GlassCard {
                Text("Companion Notifications", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                ToggleRow("Allow companion to notify me", companionNotifications) { companionNotifications = it }
                SoftDropdown(
                    label = "Character mode",
                    selected = characterMode,
                    options = characterModeOptions(),
                    onSelected = { characterMode = it },
                )
                SoftDropdown(
                    label = "Support focus",
                    selected = supportFocus,
                    options = supportFocusOptions(),
                    onSelected = { supportFocus = it },
                )
                SoftDropdown(
                    label = "Style",
                    selected = notificationStyle,
                    options = listOf("Calculated from character preferences", "Soft and supportive", "Direct and clear", "Motivational", "Playful", "Protective"),
                    onSelected = { notificationStyle = it },
                )
                SoftDropdown(
                    label = "Urgency",
                    selected = urgencyMode,
                    options = listOf("Calculated from character preferences", "Soft only", "Normal", "High urgency"),
                    onSelected = { urgencyMode = it },
                )
                Text("LUNAKAI keeps the deeper companion-experience calculation in the background, blending style, urgency, character mode, support focus, personality traits, and communication style.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            GlassCard {
                ToggleRow("Daily gentle prompt", dailyPrompt) { dailyPrompt = it }
                ToggleRow("Mood check-ins", checkIns) { checkIns = it }
                ToggleRow("Journal reminders", journalReminders) { journalReminders = it }
                ToggleRow("Supportive safety nudges", crisisNudges) { crisisNudges = it }
                ToggleRow("Quiet hours", quietHours) { quietHours = it }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsScreen(
    companion: CompanionProfile,
    mode: String,
    palette: String,
    onSaveAppearance: (String, String) -> Unit,
) {
    val appearance = LocalAppAppearance.current
    val paletteOptions = listOf("Rose Plum", "Lavender Dream", "Calm Blue", "Emerald Gold", "Peach Glow", "Moonlit Mono")
    var draftMode by remember(mode) { mutableStateOf(mode) }
    var draftPalette by remember(palette) { mutableStateOf(palette) }
    val draftAppearance = appAppearance(draftMode, draftPalette)
    GradientBackground {
        ScreenScroll {
            SectionHeader("Visual style", "Change only the app's appearance.")
            GlassCard {
                Text("Theme mode", color = appearance.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AppearanceChoiceButton(
                        label = "Dark",
                        selected = draftMode == "Dark",
                        modifier = Modifier.weight(1f),
                        onClick = { draftMode = "Dark" },
                    )
                    AppearanceChoiceButton(
                        label = "Light",
                        selected = draftMode == "Light",
                        modifier = Modifier.weight(1f),
                        onClick = { draftMode = "Light" },
                    )
                }
                Text("Choose a mode and palette, then tap Save Settings to apply it across the app.", color = appearance.mutedText, fontSize = 13.sp, lineHeight = 18.sp)
            }

            GlassCard {
                Text("Color palette", color = appearance.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                paletteOptions.forEach { option ->
                    PaletteOptionRow(
                        name = option,
                        selected = draftPalette == option,
                        preview = appAppearance(draftMode, option),
                        onClick = { draftPalette = option },
                    )
                }
            }

            GlassCard(background = draftAppearance.elevatedCard) {
                Text("Preview", color = draftAppearance.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Theme: $draftMode", color = draftAppearance.mutedText, fontWeight = FontWeight.SemiBold)
                Text("Palette: $draftPalette", color = draftAppearance.mutedText, fontWeight = FontWeight.SemiBold)
                Text("Companion orb style: ${companion.avatarType}", color = draftAppearance.mutedText)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(draftAppearance.accentStart, draftAppearance.accentMiddle, draftAppearance.accentEnd).forEach { color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(PillShape)
                                .background(color),
                        )
                    }
                }
                PrimaryGradientButton("Save Settings") {
                    onSaveAppearance(draftMode, draftPalette)
                }
            }
        }
    }
}

@Composable
private fun AppearanceChoiceButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val appearance = LocalAppAppearance.current
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(PillShape)
            .background(
                if (selected) {
                    Brush.horizontalGradient(listOf(appearance.accentStart, appearance.accentMiddle, appearance.accentEnd))
                } else {
                    Brush.horizontalGradient(listOf(appearance.control, appearance.elevatedCard))
                },
            )
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.42f) else appearance.border, PillShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PaletteOptionRow(
    name: String,
    selected: Boolean,
    preview: AppAppearance,
    onClick: () -> Unit,
) {
    val appearance = LocalAppAppearance.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MediumShape)
            .background(if (selected) appearance.elevatedCard else appearance.control)
            .border(1.dp, if (selected) preview.accentMiddle.copy(alpha = 0.72f) else appearance.border, MediumShape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(preview.accentStart, preview.accentMiddle, preview.accentEnd).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = appearance.text, fontWeight = FontWeight.Bold)
            Text(if (selected) "Active palette" else "Tap to apply", color = appearance.mutedText, fontSize = 12.sp)
        }
        if (selected) SoftStatusChip("Selected", preview.accentMiddle)
    }
}

@Composable
private fun DisclaimerScreen(
    hasAcceptedDisclaimer: Boolean,
    hideDisclaimerOnLaunch: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onHideChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onCrisis: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    GradientBackground {
        ScreenScroll {
            onBack?.let { WellnessBackButton(it) }
            Text("Before We Begin", color = TextDark, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("Your companion is here for support, reflection, journaling, and emotional wellness.", color = TextMuted, fontSize = 16.sp)
            GlassCard {
                Text(disclaimerCopy(), color = TextDark, fontSize = 15.sp, lineHeight = 21.sp)
            }
            CheckRow("I understand", hasAcceptedDisclaimer) { onAcceptedChange(!hasAcceptedDisclaimer) }
            CheckRow("Do not show this every time I open the app", hideDisclaimerOnLaunch) { onHideChange(!hideDisclaimerOnLaunch) }
            PrimaryGradientButton("I Understand", onClick = onContinue)
            SecondarySoftButton("View Crisis Resources", onClick = onCrisis)
        }
    }
}

@Composable
private fun CrisisResourcesScreen(onBack: () -> Unit) {
    GradientBackground {
        ScreenScroll {
            WellnessBackButton(onBack)
            SectionHeader("Immediate support", "Clear help, held gently.")
            GlassCard(background = WarningPeach.copy(alpha = 0.34f)) {
                Text("Emergency Help", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("If you are in immediate danger or need urgent help, call 911 right now.", color = TextDark, fontSize = 16.sp)
                PrimaryGradientButton("Call 911", onClick = {})
            }
            GlassCard {
                Text("Crisis and Suicide Support", color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("If you are in emotional distress, having thoughts of self-harm, or need immediate mental health support, call or text 988 to reach the Suicide & Crisis Lifeline.", color = TextMuted, fontSize = 16.sp, lineHeight = 22.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondarySoftButton("Call 988", modifier = Modifier.weight(1f), onClick = {})
                    SecondarySoftButton("Text 988", modifier = Modifier.weight(1f), onClick = {})
                }
            }
            GlassCard(background = CalmBlue.copy(alpha = 0.34f)) {
                Text(
                    "You are not alone in this struggle.\nThere are people ready to help and support you right now.\nPlease reach out.",
                    color = TextDark,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SecondarySoftButton("Back to App", onClick = onBack)
        }
    }
}

private fun disclaimerCopy(): String {
    return "LUNAKAI Wellness Companion is an AI-powered wellness and reflection app. It is designed to offer supportive conversation, journaling prompts, grounding tools, and general emotional wellness information.\n\n" +
        "This app is not a licensed therapist, doctor, counselor, emergency service, or crisis service. It does not diagnose, treat, cure, or prevent any mental health condition or medical condition.\n\n" +
        "The companion may share general wellness ideas inspired by publicly available emotional wellness, communication, mindfulness, and self-reflection concepts. This information is for educational and supportive purposes only and should not replace professional care.\n\n" +
        "If you feel like you may hurt yourself, hurt someone else, or you are in immediate danger, call emergency services right away. If you are in the United States and need emotional crisis support, call or text 988 or use the 988 Lifeline chat.\n\n" +
        "By continuing, you understand that this app is for support, journaling, reflection, and wellness guidance only."
}

@Composable
private fun StorageChoiceSheet(
    selected: JournalStorage,
    onSelected: (JournalStorage) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.18f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        GlassCard(
            modifier = Modifier
                .padding(18.dp)
                .clickable(enabled = false) {},
            background = CreamWhite.copy(alpha = 0.96f),
        ) {
            Text("Where do you want to save this entry?", color = TextDark, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            JournalStorage.entries.forEach { storage ->
                SelectableInfoCard(storage.label, storage.body, selected == storage) { onSelected(storage) }
            }
        }
    }
}

@Composable
private fun GradientBackground(content: @Composable () -> Unit) {
    val appearance = LocalAppAppearance.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(appearance.background)),
    ) {
        content()
    }
}

@Composable
private fun ScreenScroll(content: @Composable ColumnScope.() -> Unit) {
    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        scrollState.scrollTo(0)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing),
    ) {
        content()
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Dp = CardPadding,
    background: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appearance = LocalAppAppearance.current
    val cardBackground = if (background == Color.Unspecified) appearance.card else background
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, LargeShape)
            .clip(LargeShape)
            .background(cardBackground)
            .border(1.dp, appearance.border, LargeShape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

@Composable
private fun PrimaryGradientButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val appearance = LocalAppAppearance.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(8.dp, PillShape)
            .clip(PillShape)
            .background(Brush.horizontalGradient(listOf(appearance.accentStart, appearance.accentMiddle, appearance.accentEnd)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondarySoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val appearance = LocalAppAppearance.current
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(containerColor = appearance.secondaryButton, contentColor = appearance.text),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MoodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) {
        Brush.horizontalGradient(listOf(RosePink, Lavender))
    } else {
        Brush.horizontalGradient(listOf(CardDark.copy(alpha = 0.95f), CardAccent.copy(alpha = 0.86f)))
    }
    Box(
        modifier = Modifier
            .clip(LargeShape)
            .background(background)
            .border(1.dp, Color.White.copy(alpha = 0.62f), LargeShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Color.White else TextDark, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = modifier
            .height(132.dp)
            .clickable(onClick = onClick),
        padding = CardPadding,
        background = CardDark.copy(alpha = 0.94f),
    ) {
        SoftIcon(icon, color)
        Text(title, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CompanionAvatar(size: Dp, label: String, glow: Boolean, photoUri: String? = null, imageResId: Int? = null) {
    val imageBitmap = rememberProfileImage(photoUri)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(if (glow) 22.dp else 6.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(RosePink, Lavender, CalmBlue))),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "$label companion photo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.68f), CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else if (imageResId != null) {
            Image(
                painter = painterResource(imageResId),
                contentDescription = "$label mock companion portrait",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.68f), CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size * 0.72f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.24f))
                    .border(1.dp, Color.White.copy(alpha = 0.50f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(label.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value / 4).sp)
            }
        }
    }
}

@Composable
private fun ProfilePhotoButton(size: Dp, photoUri: String?, onClick: () -> Unit) {
    val imageBitmap = rememberProfileImage(photoUri)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(CardAccent, DeepPlumBlack)))
            .border(1.dp, AccentPink.copy(alpha = 0.62f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text("+", color = TextPrimary, fontSize = (size.value * 0.46f).sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun rememberProfileImage(photoUri: String?): ImageBitmap? {
    val context = LocalContext.current
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, photoUri) {
        value = if (photoUri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(photoUri)).use { stream ->
                        stream?.let { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                    }
                }.getOrNull()
            }
        }
    }
    return imageBitmap
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    val appearance = LocalAppAppearance.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = appearance.text, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) Text(subtitle, color = appearance.mutedText, fontSize = 15.sp)
    }
}

@Composable
private fun RoundedInputField(value: String, onValueChange: (String) -> Unit, label: String, minLines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = MediumShape,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = TextSecondary,
            unfocusedLabelColor = TextMuted,
            cursorColor = AccentPink,
            focusedBorderColor = AccentPink,
            unfocusedBorderColor = TextMuted.copy(alpha = 0.48f),
            focusedContainerColor = CardDark.copy(alpha = 0.62f),
            unfocusedContainerColor = CardDark.copy(alpha = 0.54f),
        ),
    )
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start) {
        val bubbleModifier = Modifier.fillMaxWidth(0.82f)
        if (message.isUser) {
            Box(
                modifier = bubbleModifier
                    .clip(RoundedCornerShape(24.dp, 24.dp, 6.dp, 24.dp))
                    .background(Brush.horizontalGradient(listOf(DeepRose, Lavender)))
                    .padding(16.dp),
            ) {
                Text(message.text, color = Color.White, fontSize = 15.sp)
            }
        } else {
            GlassCard(modifier = bubbleModifier, padding = 16.dp) {
                Text(message.text, color = TextDark, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ThinkingBubble(companionName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking-bubble")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(620), repeatMode = RepeatMode.Reverse),
        label = "thinking-dot",
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        GlassCard(modifier = Modifier.fillMaxWidth(0.58f), padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$companionName is thinking", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size((6 + index).dp)
                                .clip(CircleShape)
                                .background(DeepRose.copy(alpha = (pulse - index * 0.16f).coerceIn(0.25f, 1f))),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalEntryCard(entry: JournalEntry) {
    GlassCard {
        Text(entry.date, color = TextMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SoftStatusChip(entry.entryType.label, AccentPink)
            SoftStatusChip("Mood: ${entry.mood}", CalmBlue)
            SoftStatusChip(entry.storageLocation.label, Lavender)
        }
        Text(entry.title, color = TextDark, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(entry.preview, color = TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondarySoftButton("Edit", modifier = Modifier.weight(1f), onClick = {})
            SecondarySoftButton("Delete", modifier = Modifier.weight(1f), onClick = {})
        }
    }
}

@Composable
private fun MemoryCategoryCard(category: MemoryCategory) {
    var enabled by remember { mutableStateOf(category.enabled) }
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category.title, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(category.description, color = TextMuted)
            }
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
    }
}

@Composable
private fun SoftFeatureChip(text: String) {
    val appearance = LocalAppAppearance.current
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(appearance.secondaryButton)
            .border(1.dp, appearance.border, PillShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text, color = appearance.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SoftStatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(PillShape)
            .background(color.copy(alpha = 0.52f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text, color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IconCircleButton(iconResId: Int, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(CardAccent.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            tint = DeepRose,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun CheckInScreen(onBack: () -> Unit, onSaveToJournal: () -> Unit) {
    var mood by remember { mutableStateOf("Calm") }
    var note by remember { mutableStateOf("") }
    val needs = listOf("Comfort", "Clarity", "Courage", "Rest", "Grounding", "Motivation")
    var selectedNeed by remember { mutableStateOf("Comfort") }

    GradientBackground {
        ScreenScroll {
            WellnessBackButton(onBack)
            SectionHeader("Check-In", "Name your mood and choose what kind of support you need.")
            GlassCard {
                Text("How are you arriving right now?", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("Calm", "Heavy", "Anxious", "Hopeful", "Tired").forEach {
                        MoodChip(label = it, selected = mood == it, onClick = { mood = it })
                    }
                }
                Text("Support need", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                CheckboxOptionGrid(options = needs, selected = setOf(selectedNeed), onToggle = { selectedNeed = it })
                RoundedInputField(note, { note = it }, "What should your companion know?", minLines = 4)
                PrimaryGradientButton("Save Check-In", onClick = onSaveToJournal)
                Text("Saved check-ins can later sync to users/{userId}/checkIns.", color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AffirmationsScreen(onBack: () -> Unit) {
    val affirmations = listOf(
        "You are not behind. You are becoming at your own pace.",
        "You can move gently and still move forward.",
        "Your needs are real, and they deserve room.",
        "You do not have to solve everything tonight.",
    )
    var selected by remember { mutableStateOf(affirmations.first()) }

    GradientBackground {
        ScreenScroll {
            WellnessBackButton(onBack)
            SectionHeader("Affirmations", "Give your mind something safe to hold.")
            GlassCard(background = CardAccent.copy(alpha = 0.94f)) {
                Text(selected, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                Text("Soft reminder", color = TextMuted, modifier = Modifier.padding(top = 8.dp))
            }
            affirmations.forEach { line ->
                SelectableInfoCard(line, "Tap to hold this affirmation today.", selected == line) {
                    selected = line
                }
            }
            SecondarySoftButton("Save Affirmation to Journal", onClick = {})
        }
    }
}

@Composable
private fun BreathingScreen(onBack: () -> Unit) {
    var active by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("Ready") }

    LaunchedEffect(active) {
        if (active) {
            repeat(3) {
                listOf("Breathe in", "Hold", "Breathe out", "Rest").forEach { nextPhase ->
                    phase = nextPhase
                    delay(900)
                }
            }
            phase = "Ready"
            active = false
        }
    }

    GradientBackground {
        ScreenScroll {
            WellnessBackButton(onBack)
            SectionHeader("Breathing", "A calm reset you can start anytime.")
            GlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val size by animateFloatAsState(if (active && phase == "Breathe in") 1.18f else 1f, label = "breath-size")
                    Box(
                        modifier = Modifier
                            .size((164 * size).dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(AccentPink, AccentPurple, CalmBlue)))
                            .border(2.dp, Color.White.copy(alpha = 0.24f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(phase, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Text("60-second breath placeholder", color = TextMuted, modifier = Modifier.padding(top = 14.dp))
                    PrimaryGradientButton(if (active) "Breathing..." else "Start Breathing Reset", onClick = { active = true }, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}

private enum class ScriptSpeaker {
    USER,
    COMPANION,
    UNASSIGNED,
}

private data class ScriptLine(
    val id: String,
    val text: String,
    val speaker: ScriptSpeaker,
)

@Composable
private fun SmallCircleButton(label: String, onClick: () -> Unit) {
    val iconRes = when (label.lowercase()) {
        "mic" -> R.drawable.ic_mic
        "send" -> R.drawable.ic_chat_bubble
        else -> R.drawable.ic_sparkle
    }
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(CardAccent.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = DeepRose,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SoftIcon(label: String, color: Color, compact: Boolean = false) {
    val iconRes = when {
        label.contains("journal", ignoreCase = true) -> R.drawable.ic_journal
        label.contains("chat", ignoreCase = true) -> R.drawable.ic_chat_bubble
        label.contains("memory", ignoreCase = true) -> R.drawable.ic_memory
        label.contains("setting", ignoreCase = true) -> R.drawable.ic_settings
        label.contains("crisis", ignoreCase = true) || label.contains("warning", ignoreCase = true) -> R.drawable.ic_warning
        label.contains("live", ignoreCase = true) || label.contains("video", ignoreCase = true) -> R.drawable.ic_video
        label.contains("home", ignoreCase = true) -> R.drawable.ic_home
        label.contains("companion", ignoreCase = true) || label.contains("profile", ignoreCase = true) -> R.drawable.ic_profile
        label.contains("OK", ignoreCase = true) -> R.drawable.ic_sparkle
        else -> R.drawable.ic_heart
    }
    Box(
        modifier = Modifier
            .size(if (compact) 26.dp else 42.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.26f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(if (compact) 15.dp else 22.dp),
        )
    }
}

@Composable
private fun SoftMenuIcon(label: String) {
    val icon = when {
        label == "Home" -> R.drawable.ic_home
        label == "Chat" -> R.drawable.ic_chat_bubble
        label == "Companions" -> R.drawable.ic_profile
        label == "Companion Builder" -> R.drawable.ic_sparkle
        label == "Wellness" -> R.drawable.ic_heart
        label == "Journal" -> R.drawable.ic_journal
        label == "Memory" -> R.drawable.ic_memory
        label == "Companion State" -> R.drawable.ic_sparkle
        label == "Settings" -> R.drawable.ic_settings
        label == "Crisis Resources" -> R.drawable.ic_warning
        label == "Disclaimer" -> R.drawable.ic_warning
        else -> R.drawable.ic_heart
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(CardAccent.copy(alpha = 0.84f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = DeepRose,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SelectableInfoCard(title: String, body: String, selected: Boolean, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.clickable(onClick = onClick), background = if (selected) CardAccent.copy(alpha = 0.98f) else CardWhite) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SoftIcon(if (selected) "OK" else ".", if (selected) DeepRose else Lavender)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(body, color = TextMuted)
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(title: String, body: String, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.clickable(onClick = onClick), padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SoftIcon(title, Lavender)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(body, color = TextMuted, fontSize = 13.sp)
            }
            Text(">", color = DeepRose, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WrapChips(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { option ->
                    Box(modifier = Modifier.weight(1f)) {
                        MoodChip(label = option, selected = selected == option, onClick = { onSelected(option) })
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: () -> Unit) {
    GlassCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextDark, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Checkbox(checked = checked, onCheckedChange = { onChange() })
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = TextDark, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MiniStateCard(title: String, value: String) {
    val appearance = LocalAppAppearance.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            color = appearance.mutedText,
            modifier = Modifier.weight(0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            color = appearance.text,
            modifier = Modifier.weight(1.1f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
    }
}
