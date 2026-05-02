package com.fancie.aicompanion

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.fancie.aicompanion.ui.theme.FancieAICompanionTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

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
                "Fancie gentle support",
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
    Splash("Fancie AI Companion", "Preparing your companion..."),
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
    VoiceLiveSettings("Voice & Live Settings", "Choose voice, microphone, speaker, and live avatar options."),
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

private enum class JournalEntryType(val label: String) {
    TEXT("Written"),
    VOICE("Voice"),
    VIDEO("Video"),
}

private enum class LiveMode {
    TEXT,
    VOICE,
    LIVE_AVATAR,
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

private data class CompanionProfile(
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
)

private data class ChatMessage(
    val sender: String,
    val text: String,
    val isUser: Boolean = false,
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

@Composable
private fun FancieApp() {
    var route by remember { mutableStateOf(AppRoute.Home) }
    var signedIn by remember { mutableStateOf(true) }
    var hasAcceptedDisclaimer by remember { mutableStateOf(true) }
    var hideDisclaimerOnLaunch by remember { mutableStateOf(false) }
    var showDisclaimerOnLaunch by remember { mutableStateOf(true) }
    var defaultJournalStorage by remember { mutableStateOf(JournalStorage.Device) }
    var askStorageEveryTime by remember { mutableStateOf(false) }
    var showJournalStorageSheet by remember { mutableStateOf(false) }
    var selectedMood by remember { mutableStateOf("Calm") }
    var selectedJournalMood by remember { mutableStateOf("Calm") }
    var journalTitle by remember { mutableStateOf("Today I needed clarity") }
    var journalBody by remember { mutableStateOf("What do you need to get out of your heart today?") }
    var videoJournalState by remember { mutableStateOf("Ready") }
    var profilePhotoUri by remember { mutableStateOf<String?>(null) }
    var displayName by remember { mutableStateOf("Fancie") }
    var preferredName by remember { mutableStateOf("") }
    var pronouns by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var aboutMe by remember { mutableStateOf("") }
    var wellnessPreferences by remember { mutableStateOf("Grounding, journaling, calm reminders") }
    var notificationPreferences by remember { mutableStateOf("Daily gentle check-ins") }
    var cloudSyncPreference by remember { mutableStateOf(false) }
    var appearanceMode by remember { mutableStateOf("Dark") }
    var appearancePalette by remember { mutableStateOf("Rose Plum") }
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

    LaunchedEffect(fitnessTimerRunning) {
        while (fitnessTimerRunning) {
            delay(1000)
            fitnessSessionSeconds += 1
        }
    }

    val companions = remember {
        mutableStateListOf(
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
    }
    var activeCompanionId by remember { mutableStateOf("luna") }
    val activeCompanion = companions.firstOrNull { it.id == activeCompanionId } ?: companions.first()
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

    LaunchedEffect(Unit) {
        delay(850)
        if (route == AppRoute.Splash) route = AppRoute.Welcome
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
            else -> LoginScreen(onSignedIn = {
                signedIn = true
                routeHistory.clear()
                route = AppRoute.Home
            })
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
            onSignOut = {
                signedIn = false
                routeHistory.clear()
                route = AppRoute.Login
            },
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
                    onSelectCompanion = { id ->
                        activeCompanionId = id
                        companions.forEachIndexed { index, companion ->
                            companions[index] = companion.copy(isActive = companion.id == id)
                        }
                    },
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
                    onVideo = { navigateTo(AppRoute.LiveCompanion) },
                )
                AppRoute.TextChat -> CompanionChatScreen(
                    companion = activeCompanion,
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
                    onSetActive = { id ->
                        activeCompanionId = id
                        companions.forEachIndexed { index, companion ->
                            companions[index] = companion.copy(isActive = companion.id == id)
                        }
                    },
                    onCreate = { navigateTo(AppRoute.Personality) },
                    onEdit = { id ->
                        activeCompanionId = id
                        navigateTo(AppRoute.Personality)
                    },
                    onChat = {
                        activeCompanionId = it
                        navigateTo(AppRoute.TextChat)
                    },
                    onLive = {
                        activeCompanionId = it
                        navigateTo(AppRoute.LiveCompanionCall)
                    },
                    onDelete = { id ->
                        if (companions.size > 1) {
                            companions.removeAll { it.id == id }
                            if (activeCompanionId == id) activeCompanionId = companions.first().id
                        }
                    },
                )
                AppRoute.Personality -> PersonalityBuilderScreen(
                    companion = activeCompanion,
                    onCompanionChange = { changed ->
                        val index = companions.indexOfFirst { it.id == changed.id }
                        if (index >= 0) companions[index] = changed
                    },
                    onSave = { navigateTo(AppRoute.Companions) },
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
                            isMock = false,
                            isActive = setActive,
                            lastUsedDate = "Today",
                        )
                        if (setActive) {
                            companions.forEachIndexed { index, saved -> companions[index] = saved.copy(isActive = false) }
                            activeCompanionId = newCompanion.id
                        }
                        companions.add(newCompanion)
                        navigateTo(AppRoute.Companions)
                    },
                )
                AppRoute.FemaleAvatars -> AvatarSelectionScreen(
                    title = "Female Avatars",
                    options = femaleAvatarOptions(),
                    selected = activeCompanion.avatarType,
                    onBack = { goBack(AppRoute.Personality) },
                    onSelected = { option ->
                        val changed = activeCompanion.copy(
                            avatarType = option.name,
                            imageResName = option.resName,
                            imageResId = option.resId,
                            photoUri = null,
                        )
                        val index = companions.indexOfFirst { it.id == activeCompanion.id }
                        if (index >= 0) companions[index] = changed
                        goBack(AppRoute.Personality)
                    },
                )
                AppRoute.MaleAvatars -> AvatarSelectionScreen(
                    title = "Male Avatars",
                    options = maleAvatarOptions(),
                    selected = activeCompanion.avatarType,
                    onBack = { goBack(AppRoute.Personality) },
                    onSelected = { option ->
                        val changed = activeCompanion.copy(
                            avatarType = option.name,
                            imageResName = option.resName,
                            imageResId = option.resId,
                            photoUri = null,
                        )
                        val index = companions.indexOfFirst { it.id == activeCompanion.id }
                        if (index >= 0) companions[index] = changed
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
                    askStorageEveryTime = askStorageEveryTime,
                    hasAcceptedDisclaimer = hasAcceptedDisclaimer,
                    hideDisclaimerOnLaunch = hideDisclaimerOnLaunch,
                    showDisclaimerOnLaunch = showDisclaimerOnLaunch,
                    onStorageChange = { defaultJournalStorage = it },
                    onAskEveryTimeChange = { askStorageEveryTime = it },
                    onHideDisclaimerChange = { hideDisclaimerOnLaunch = it },
                    onShowDisclaimerChange = { showDisclaimerOnLaunch = it },
                    onNavigate = { navigateTo(it) },
                    onProfileInfo = { navigateTo(AppRoute.ProfileInfo) },
                    onSignOut = {
                        signedIn = false
                        routeHistory.clear()
                        route = AppRoute.Login
                    },
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
                AppRoute.AccountSettings -> AccountSettingsScreen(onSignOut = {
                    signedIn = false
                    routeHistory.clear()
                    route = AppRoute.Login
                })
                AppRoute.VoiceLiveSettings -> VoiceLiveSettingsScreen(
                    companion = activeCompanion,
                    companions = companions,
                    onSelectCompanion = { id ->
                        activeCompanionId = id
                        companions.forEachIndexed { index, saved ->
                            companions[index] = saved.copy(isActive = saved.id == id)
                        }
                    },
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
        "Memory" to AppRoute.Memory,
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
                        Text("Fancie AI Companion", color = TextDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
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
                                        "Memory Settings" -> onNavigate(AppRoute.Memory)
                                        "Privacy Settings" -> onNavigate(AppRoute.PrivacySettings)
                                        "View Disclaimer" -> onNavigate(AppRoute.Disclaimer)
                                        "Video" -> onNavigate(AppRoute.LiveCompanion)
                                        "Phone" -> onNavigate(AppRoute.LiveCompanionCall)
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
        AppRoute.TextChat -> listOf("Video", "Phone", "Change Companion", "Voice Settings", "Memory Settings", "Clear Chat", "Save Chat to Journal")
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
        item == "Video" -> R.drawable.ic_video
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
        val firebaseAuthClass = Class.forName("com.google.firebase.auth.FirebaseAuth")
        val instance = firebaseAuthClass.getMethod("getInstance").invoke(null)
        val currentUser = firebaseAuthClass.getMethod("getCurrentUser").invoke(instance) ?: return null
        currentUser.javaClass.getMethod("getEmail").invoke(currentUser) as? String
    }.getOrNull()
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
            Text("Fancie AI Companion", color = TextDark, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
            GlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CompanionAvatar(size = 118.dp, label = "AI", glow = true)
                    Spacer(Modifier.height(18.dp))
                    Text("Luxury wellness meets AI companion.", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Text(
                        "A private, gentle space for thoughts, rehearsal, grounding, and emotional support.",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
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
private fun LoginScreen(onSignedIn: () -> Unit) {
    var email by remember { mutableStateOf("preview@fancie.app") }
    var password by remember { mutableStateOf("") }

    GradientBackground {
        ScreenScroll {
            Text("Fancie AI Companion", color = TextDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Welcome back, love.", color = TextDark, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text("Your companion space is waiting for you.", color = TextMuted, fontSize = 16.sp)
            GlassCard {
                RoundedInputField(value = email, onValueChange = { email = it }, label = "Email")
                Spacer(Modifier.height(12.dp))
                RoundedInputField(value = password, onValueChange = { password = it }, label = "Password")
                Spacer(Modifier.height(18.dp))
                PrimaryGradientButton("Sign In", onClick = onSignedIn)
                SecondarySoftButton("Create Account", onClick = onSignedIn)
                TextButton(onClick = {}) { Text("Forgot password?", color = DeepRose) }
            }
            Text("Your thoughts stay in your private companion space.", color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
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
) {
    val chatPreviews = companions.mapIndexed { index, savedCompanion ->
        ChatPreview(
            companionId = savedCompanion.id,
            companionName = savedCompanion.name,
            preview = if (savedCompanion.id == "luna") {
                "I'm here with you. Tell me what's been sitting on your heart."
            } else if (savedCompanion.id == "kai") {
                "Take a breath. We'll slow this down and figure it out together."
            } else {
                "Your saved companion is ready when you are."
            },
            time = when (savedCompanion.id) {
                "luna" -> "19:07"
                "kai" -> "18:42"
                else -> savedCompanion.lastUsedDate
            },
            unreadCount = if (index == 0) 2 else 0,
            photoUri = savedCompanion.photoUri,
            imageResId = savedCompanion.imageResId,
        )
    }
    val visibleSlots: List<CompanionProfile?> = companions.map { it } + List(maxOf(0, 5 - companions.size)) { null }
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
                                onNavigate(AppRoute.Personality)
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
            ConnectOptionCard("Video", "Video-style companion mode.", R.drawable.ic_video, AccentPurple, Modifier.fillMaxWidth(), onVideo)
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
private fun CompanionChatScreen(companion: CompanionProfile, onNavigate: (AppRoute) -> Unit) {
    val scope = rememberCoroutineScope()
    val experience = remember(companion.id, companion.characterMode, companion.supportFocus, companion.communicationStyle, companion.personalityTraits) {
        calculateCompanionExperience(companion)
    }
    val messages = remember(companion.id, experience.chatReply) {
        mutableStateListOf(
            ChatMessage(companion.name, "I'm here with you. What's been sitting on your heart today?"),
            ChatMessage("You", "I feel overwhelmed.", isUser = true),
            ChatMessage(companion.name, experience.chatReply),
        )
    }
    var input by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    GradientBackground {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondarySoftButton("See Companion Live", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.LiveCompanionCall) })
                SecondarySoftButton("Voice Settings", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Preferences) })
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                messages.forEach { ChatBubble(it) }
                if (isTyping) ThinkingBubble(companion.name)
            }
            GlassCard(padding = 10.dp) {
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
                        if (input.isNotBlank()) {
                            messages.add(ChatMessage("You", input.trim(), isUser = true))
                            isTyping = true
                            input = ""
                            scope.launch {
                                delay(900)
                                messages.add(ChatMessage(companion.name, experience.chatReply))
                                isTyping = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WellnessHubScreen(onNavigate: (AppRoute) -> Unit) {
    GradientBackground {
        ScreenScroll {
            SectionHeader("Choose a wellness tool", "Support, grounding, reflection, and safety resources.")
            Row(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
                QuickActionCard("Grounding", "Breathing and body calming.", "calm", CalmBlue, Modifier.weight(1f)) { onNavigate(AppRoute.Grounding) }
                QuickActionCard("Reflection", "Understand emotional needs.", "spark", Lavender, Modifier.weight(1f)) { onNavigate(AppRoute.Reflection) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
                QuickActionCard("Check In", "Name your mood and what you need.", "mood", RosePink, Modifier.weight(1f)) { onNavigate(AppRoute.CheckIn) }
                QuickActionCard("Affirmations", "Give your mind something safe to hold.", "safe", SuccessGreen, Modifier.weight(1f)) { onNavigate(AppRoute.Affirmations) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
                QuickActionCard("Breathing", "Use a guided breath reset.", "breath", AccentPurple, Modifier.weight(1f)) { onNavigate(AppRoute.Breathing) }
                QuickActionCard("Crisis Resources", "Clear support for urgent moments.", "help", WarningPeach, Modifier.weight(1f)) { onNavigate(AppRoute.CrisisResources) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
                QuickActionCard("Fitness", "Track movement and start free workouts.", "fitness", GoldAccent, Modifier.weight(1f)) { onNavigate(AppRoute.Fitness) }
                QuickActionCard("Wellness Disclaimer", "Supportive guidance, not therapy.", "safe", WarningPeach, Modifier.weight(1f)) { onNavigate(AppRoute.Disclaimer) }
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
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage(companion.name, "I'm listening. Tell me what's been sitting on your heart."),
            ChatMessage(companion.name, "You do not have to solve everything at once."),
        )
    }
    var textInput by remember { mutableStateOf("") }
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    ModeChip("Text Chat", state.selectedMode == LiveMode.TEXT) { state = state.copy(selectedMode = LiveMode.TEXT); showTextChat = true }
                    ModeChip("Voice Chat", state.selectedMode == LiveMode.VOICE) { state = state.copy(selectedMode = LiveMode.VOICE, callStatus = "Voice mode ready") }
                    ModeChip("Live Avatar", state.selectedMode == LiveMode.LIVE_AVATAR) { state = state.copy(selectedMode = LiveMode.LIVE_AVATAR, isCompanionSpeaking = true, currentCaption = "I'm here with you in live avatar mode.") }
                    ModeChip("Video Mode", state.selectedMode == LiveMode.VIDEO_STYLE) { state = state.copy(selectedMode = LiveMode.VIDEO_STYLE, currentCaption = "Video companion mode is being prepared.") }
                }

                if (state.selectedMode == LiveMode.VIDEO_STYLE) {
                    GlassCard(background = CardDark.copy(alpha = 0.94f)) {
                        Text("Video companion mode is being prepared.", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Your companion will be able to speak, listen, and respond through a live visual experience.", color = TextMuted)
                    }
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
                            val next = !state.isMicOn
                            state = state.copy(
                                isMicOn = next,
                                isUserSpeaking = next,
                                callStatus = if (next) "Listening..." else "Mic muted",
                                currentCaption = if (next) "I'm listening. Tell me what feels loud inside." else "Mic muted. You can still type to me.",
                            )
                        }
                        CallControlButton("Speaker", R.drawable.ic_speaker, state.isSpeakerOn, CalmBlue) {
                            state = state.copy(isSpeakerOn = !state.isSpeakerOn)
                        }
                        CallControlButton("Switch", R.drawable.ic_swap, false, GoldAccent) { onNavigate(AppRoute.Companions) }
                        CallControlButton("Capture", R.drawable.ic_capture, showCaptureSheet, SuccessGreen) { showCaptureSheet = true }
                        CallControlButton("End", R.drawable.ic_call_end, false, WarningPeach) { onNavigate(AppRoute.Home) }
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
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            chatMessages.forEach { ChatBubble(it) }
                        }
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
                                    if (textInput.isNotBlank()) {
                                        chatMessages.add(ChatMessage("You", textInput.trim(), isUser = true))
                                        chatMessages.add(ChatMessage(companion.name, "That sounds heavy. Let's slow it down together."))
                                        state = state.copy(currentCaption = "That sounds heavy. Let's slow it down together.", isCompanionSpeaking = true)
                                        textInput = ""
                                    }
                                },
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
                        "Text, talk, or sit with your companion in a premium live space. Voice, avatar movement, and video-style features are prepared as UI-ready placeholders.",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
            PrimaryGradientButton("Open Live Companion Call", onClick = { onNavigate(AppRoute.LiveCompanionCall) })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Text Chat", "Continue the conversation by typing.", "chat", RosePink, Modifier.weight(1f)) { onNavigate(AppRoute.TextChat) }
                QuickActionCard("Voice Chat", "Microphone-ready companion mode.", "voice", CalmBlue, Modifier.weight(1f)) { onNavigate(AppRoute.LiveCompanionCall) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Live Avatar", "A visual companion presence.", "avatar", AccentPurple, Modifier.weight(1f)) { onNavigate(AppRoute.LiveCompanionCall) }
                QuickActionCard("Video-Style", "Upcoming live visual experience.", "video", GoldAccent, Modifier.weight(1f)) { onNavigate(AppRoute.LiveCompanionCall) }
            }
            GlassCard {
                Text("Video companion mode is being prepared.", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Your companion will be able to speak, listen, and respond through a live visual experience. This is a polished placeholder, not an error.", color = TextMuted, lineHeight = 21.sp)
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
            SectionHeader(title, "Choose a built-in portrait for your companion.")
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
                                Text(option.description, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
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
    val supportOptions = listOf("Stress", "Self-worth", "Confidence", "Grounding", "Journaling", "Reflection", "Relationships", "Creativity")
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
                RoundedInputField(companion.name, { onCompanionChange(companion.copy(name = it)) }, "Companion name")
                SoftDropdown("Gender", companion.gender, genderOptions) { gender ->
                    val defaultVoice = when (gender) {
                        "Male" -> if (companion.voice in femaleVoiceOptions) "Calm Male" else companion.voice
                        "Female" -> if (companion.voice in maleVoiceOptions) "Soft Female" else companion.voice
                        else -> companion.voice
                    }
                    onCompanionChange(companion.copy(gender = gender, voice = defaultVoice))
                }
                RoundedInputField(companion.shortDescription, { onCompanionChange(companion.copy(shortDescription = it)) }, "Short description", minLines = 2)
            }

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
                Text(
                    if (companion.photoUri == null) {
                        "Upload a photo to replace the companion circle across Home, Chat, Live Call, and saved companion areas."
                    } else {
                        "Photo selected. The companion circles now use this image."
                    },
                    color = TextMuted,
                )
                Text("Firebase Storage later: photoUri and photoStoragePath are already modeled.", color = TextMuted, fontSize = 13.sp)
            }
            SectionHeader("Voice")
            GlassCard {
                Text("Visible voice options respond to the selected gender, but stay flexible for custom identity choices.", color = TextMuted)
                SoftDropdown("Companion Voice", companion.voice, allowedVoiceOptions) { voice ->
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
                        onCompanionChange(companion.copy(roleplayStyles = nextList, roleplayStyle = nextList.first()))
                    },
                )
                RoleplayDetails(
                    style = companion.roleplayStyle,
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
                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
) {
    var detailOptions by remember(style) { mutableStateOf(setOf<String>()) }
    when (style) {
        "Athletic Partner" -> {
            Text("Workout encouragement, timing, motivation, and accountability.", color = TextMuted)
            Text("Workout Timer: $workoutTimerState", color = TextDark, fontWeight = FontWeight.SemiBold)
            SoftDropdown("Timer Duration", timerDuration, listOf("30 seconds", "1 minute", "5 minutes", "10 minutes", "15 minutes", "Custom"), onTimerDurationChange)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondarySoftButton("Start Timer", modifier = Modifier.weight(1f), onClick = { onWorkoutTimerStateChange("Running") })
                SecondarySoftButton("Pause Timer", modifier = Modifier.weight(1f), onClick = { onWorkoutTimerStateChange("Paused") })
            }
            SecondarySoftButton("Reset Timer", onClick = { onWorkoutTimerStateChange("Ready") })
            SoftDropdown("Motivation Style", motivationStyle, listOf("Soft encouragement", "Trainer energy", "Discipline mode", "Playful push"), onMotivationStyleChange)
        }
        "Monologue Practice" -> {
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
        "BDSM" -> {
            Text("Adult roleplay script rehearsal and consent-aware character interaction.", color = TextMuted)
            GlassCard(background = WarningPeach.copy(alpha = 0.18f), padding = 14.dp) {
                Text("This roleplay mode is for consenting adults, script practice, and fantasy-based conversation. You can pause, stop, or change boundaries at any time.", color = TextDark, fontSize = 14.sp, lineHeight = 20.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondarySoftButton("Upload Script", modifier = Modifier.weight(1f), onClick = {})
                SecondarySoftButton("Paste Script", modifier = Modifier.weight(1f), onClick = {})
            }
            RoundedInputField(pastedScript, onPastedScriptChange, "Paste script for rehearsal", minLines = 4)
            CheckboxOptionGrid(
                listOf("Assign companion lines", "Assign user lines", "Rehearse in chat", "Start scene rehearsal", "Consent and boundaries"),
                detailOptions,
                { option -> detailOptions = if (option in detailOptions) detailOptions - option else detailOptions + option },
            )
            RoundedInputField(stopWord, onStopWordChange, "Stop word / pause word")
            RoundedInputField(boundaries, onBoundariesChange, "Consent and boundaries", minLines = 3)
        }
        else -> {
            Text("Supportive wellness guidance, grounding, reflection, and encouragement.", color = TextMuted)
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
                    Text("Model ready: videoEntryUri and videoStoragePath.", color = TextMuted, fontSize = 12.sp)
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
            videoStoragePath = "users/{userId}/journalVideos/video-placeholder",
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondarySoftButton("Change Personality", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Personality) })
                SecondarySoftButton("Manage Memory", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Memory) })
            }
            PrimaryGradientButton("Start Chat", onClick = { onNavigate(AppRoute.TextChat) })
            SecondarySoftButton("Open Live Call", onClick = { onNavigate(AppRoute.LiveCompanionCall) })
        }
    }
}

@Composable
private fun PreferencesScreen(
    companion: CompanionProfile,
    defaultJournalStorage: JournalStorage,
    askStorageEveryTime: Boolean,
    hasAcceptedDisclaimer: Boolean,
    hideDisclaimerOnLaunch: Boolean,
    showDisclaimerOnLaunch: Boolean,
    onStorageChange: (JournalStorage) -> Unit,
    onAskEveryTimeChange: (Boolean) -> Unit,
    onHideDisclaimerChange: (Boolean) -> Unit,
    onShowDisclaimerChange: (Boolean) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onProfileInfo: () -> Unit,
    onSignOut: () -> Unit,
) {
    var voiceJournalEnabled by remember { mutableStateOf(true) }
    var memoryEnabled by remember { mutableStateOf(true) }
    var appLock by remember { mutableStateOf(false) }
    var hidePreviews by remember { mutableStateOf(true) }
    var cloudSync by remember { mutableStateOf(false) }
    var videoJournalEnabled by remember { mutableStateOf(true) }
    var groundingEnabled by remember { mutableStateOf(true) }
    var breathingEnabled by remember { mutableStateOf(true) }
    var affirmationsEnabled by remember { mutableStateOf(true) }
    var reflectionPromptsEnabled by remember { mutableStateOf(true) }
    var checkInRemindersEnabled by remember { mutableStateOf(false) }
    var defaultWellnessTool by remember { mutableStateOf("Grounding") }
    var checkInStyle by remember { mutableStateOf("Gentle") }
    var reminderFrequency by remember { mutableStateOf("Evening") }
    var companionNotificationsEnabled by remember { mutableStateOf(true) }
    var notificationStyle by remember { mutableStateOf("Calculated from character preferences") }
    var notificationUrgency by remember { mutableStateOf("Calculated from character preferences") }
    var wakePhraseEnabled by remember { mutableStateOf(false) }
    var wakeAudioSource by remember { mutableStateOf("Phone microphone only when selected") }
    var wakeOnlineResources by remember { mutableStateOf(true) }
    val accountEmail = currentFirebaseEmail() ?: "Not available"

    GradientBackground {
        ScreenScroll {
            SectionHeader("Account and app controls", "Manage your companion, privacy, storage, and support settings.")

            GlassCard {
                Text("Account", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Signed in as: $accountEmail", color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondarySoftButton("Profile Info", modifier = Modifier.weight(1f), onClick = onProfileInfo)
                    SecondarySoftButton("Sign Out", modifier = Modifier.weight(1f), onClick = onSignOut)
                }
                SecondarySoftButton("Manage Account", onClick = { onNavigate(AppRoute.AccountSettings) })
            }

            GlassCard {
                Text("Active Companion", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                MiniStateCard("Current companion", companion.name)
                MiniStateCard("Voice", companion.voice)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Change", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Companions) })
                    SecondarySoftButton("Create New", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Personality) })
                }
                SecondarySoftButton("Manage Saved Companions", onClick = { onNavigate(AppRoute.Companions) })
                SecondarySoftButton("View Companion State", onClick = { onNavigate(AppRoute.CompanionState) })
            }

            GlassCard {
                Text("Companion Customization", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                MiniStateCard("Companion photo", companion.avatarType)
                MiniStateCard("Identity", companion.gender)
                MiniStateCard("Roleplay", if (companion.roleplayEnabled) "Enabled" else "Disabled")
                MiniStateCard("Communication style", companion.communicationStyle)
                SecondarySoftButton("Edit Companion", onClick = { onNavigate(AppRoute.Personality) })
            }

            GlassCard {
                Text("Journal Settings", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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

            GlassCard {
                Text("Wellness Settings", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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

            GlassCard {
                Text("Memory Settings", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                ToggleRow("Memory enabled", memoryEnabled) { memoryEnabled = it }
                SecondarySoftButton("Manage Memories", onClick = { onNavigate(AppRoute.Memory) })
                SecondarySoftButton("Clear Selected Memory Fields", onClick = {})
                SecondarySoftButton("Clear All Memory", onClick = {})
            }

            GlassCard {
                Text("Privacy Settings", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                ToggleRow("App lock", appLock) { appLock = it }
                ToggleRow("Hide sensitive previews", hidePreviews) { hidePreviews = it }
                ToggleRow("Cloud sync", cloudSync) { cloudSync = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Export Data", modifier = Modifier.weight(1f), onClick = {})
                    SecondarySoftButton("Delete Data", modifier = Modifier.weight(1f), onClick = {})
                }
            }

            GlassCard {
                Text("Companion Notifications", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                Text("Fancie uses these choices in the background with the active companion's traits, support focus, communication style, and character mode so notifications feel personal without showing the calculation here.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
                SecondarySoftButton("Advanced Notification Settings", onClick = { onNavigate(AppRoute.NotificationSettings) })
            }

            GlassCard {
                Text("Voice Wake Phrase", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                ToggleRow("Enable Hey ${companion.name}", wakePhraseEnabled) { wakePhraseEnabled = it }
                ToggleRow("Use AI and online resources", wakeOnlineResources) { wakeOnlineResources = it }
                MiniStateCard("Wake phrase", "Hey ${companion.name}")
                SoftDropdown(
                    label = "Audio input source",
                    selected = wakeAudioSource,
                    options = listOf("Phone microphone only when selected", "Any connected Bluetooth audio", "Bluetooth headset", "Car Bluetooth", "Smart speaker", "Selected audio device only"),
                    onSelected = { wakeAudioSource = it },
                )
                Text("${companion.name} can be set up to answer everyday requests like reminders, questions, planning help, and online-resource lookups once speech and web assistance are connected.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
                SecondarySoftButton("Voice Controls", onClick = { onNavigate(AppRoute.VoiceLiveSettings) })
            }

            GlassCard {
                Text("Appearance", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                MiniStateCard("Theme", "Soft luxury gradient")
                MiniStateCard("Accent style", "Rose and lavender")
                MiniStateCard("Card style", "Glassy rounded panels")
                MiniStateCard("Avatar/orb style", companion.avatarType)
                SecondarySoftButton("Open Appearance", onClick = { onNavigate(AppRoute.AppearanceSettings) })
            }

            GlassCard(background = WarningPeach.copy(alpha = 0.28f)) {
                Text("Support & Safety", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
    var voiceReplies by remember { mutableStateOf(true) }
    var microphone by remember { mutableStateOf(true) }
    var speaker by remember { mutableStateOf(true) }
    var voiceSpeed by remember { mutableStateOf("Normal") }
    var wakePhraseEnabled by remember { mutableStateOf(false) }
    var answerStyle by remember { mutableStateOf("Answer with voice and text") }
    var audioSource by remember { mutableStateOf("Phone microphone only when selected") }
    var bluetoothAccess by remember { mutableStateOf(false) }
    var onlineResources by remember { mutableStateOf(true) }
    var voiceTrainingEnabled by remember { mutableStateOf(false) }
    var trainingStatus by remember { mutableStateOf("Not trained yet") }
    var voiceSensitivity by remember { mutableStateOf("Balanced") }
    val companionNames = companions.map { it.name }
    val voiceOptions = voiceOptionsFor(companion.gender)

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
                ToggleRow("Listen for Hey ${companion.name}", wakePhraseEnabled) { wakePhraseEnabled = it }
                ToggleRow("Use AI and online resources", onlineResources) { onlineResources = it }
                MiniStateCard("Phrase", "Hey ${companion.name}")
                Text("${companion.name} can assist with everyday needs like reminders, planning, questions, wellness prompts, and online-resource lookups once AI assistance is connected.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            GlassCard {
                Text("Audio access", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                SoftDropdown(
                    label = "Audio input source",
                    selected = audioSource,
                    options = listOf("Phone microphone only when selected", "Any connected Bluetooth audio", "Bluetooth headset", "Car Bluetooth", "Smart speaker", "Selected audio device only"),
                    onSelected = { audioSource = it },
                )
                ToggleRow("Allow Bluetooth audio devices", bluetoothAccess) { bluetoothAccess = it }
                MiniStateCard("Current route", audioSource)
                Text("${companion.name} will only listen through the selected audio source when this feature is enabled. Bluetooth support is UI-ready for Android audio routing later.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            GlassCard {
                Text("Voice training", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                ToggleRow("Recognize my voice", voiceTrainingEnabled) { voiceTrainingEnabled = it }
                MiniStateCard("Training phrase", "Hey ${companion.name}")
                MiniStateCard("Training status", trainingStatus)
                SoftDropdown(
                    label = "Voice match sensitivity",
                    selected = voiceSensitivity,
                    options = listOf("Gentle", "Balanced", "Strict"),
                    onSelected = { voiceSensitivity = it },
                )
                SecondarySoftButton("Record Sample: Hey ${companion.name}", onClick = { trainingStatus = "Sample captured. Add two more later for stronger recognition." })
                SecondarySoftButton("Reset Voice Training", onClick = { trainingStatus = "Not trained yet" })
                Text("Voice training is a placeholder flow until speech recognition and voice matching are connected.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            GlassCard {
                Text("Answer behavior", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                SoftDropdown(
                    label = "How ${companion.name} answers",
                    selected = answerStyle,
                    options = listOf("Answer with voice and text", "Answer with voice only", "Answer with text only"),
                    onSelected = { answerStyle = it },
                )
                MiniStateCard("AI resource access", if (onlineResources) "Enabled for future setup" else "Off")
            }
            GlassCard {
                Text("Live avatar settings", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Avatar type: ${companion.avatarType}", color = TextMuted)
                SecondarySoftButton("Open Live Companion Call", onClick = onLiveSettings)
                Text("Video companion mode is prepared as a premium placeholder until live visual technology is connected.", color = TextMuted)
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
                Text("Fancie keeps the deeper companion-experience calculation in the background, blending style, urgency, character mode, support focus, personality traits, and communication style.", color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
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
    return "Fancie AI Companion is an AI-powered wellness and reflection app. It is designed to offer supportive conversation, journaling prompts, grounding tools, and general emotional wellness information.\n\n" +
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
        Text(title, color = appearance.mutedText, modifier = Modifier.weight(1f))
        Text(value, color = appearance.text, fontWeight = FontWeight.SemiBold)
    }
}
