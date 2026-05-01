package com.fancie.aicompanion

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
private val CardDark = Color(0xFF1A1A22)
private val CardAccent = Color(0xFF2A1626)
private val AccentPink = Color(0xFFE85AAE)
private val AccentRose = Color(0xFFD9488B)
private val AccentPurple = Color(0xFF8E5CFF)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB8B8C2)
private val BackgroundTop = AppBlack
private val BackgroundMiddle = SoftBlack
private val BackgroundBottom = DeepPlumBlack
private val RosePink = AccentPink
private val DeepRose = AccentRose
private val Lavender = AccentPurple
private val SoftPurple = Color(0xFFB66DFF)
private val CreamWhite = CardDark
private val TextDark = TextPrimary
private val TextMuted = TextSecondary
private val CardWhite = Color(0xE61A1A22)
private val GoldAccent = Color(0xFFE8C77A)
private val CalmBlue = Color(0xFF5DA8FF)
private val SuccessGreen = Color(0xFF66D19E)
private val WarningPeach = Color(0xFFE66A77)

private val MediumShape = RoundedCornerShape(20.dp)
private val LargeShape = RoundedCornerShape(28.dp)
private val ExtraLargeShape = RoundedCornerShape(36.dp)
private val PillShape = RoundedCornerShape(50)

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
    Chat("Chats", "Texting with you"),
    Wellness("Wellness", "Support, grounding, and reflection."),
    LiveCompanionCall("Live Companion Call", "Talk, text, or sit with your companion in real time."),
    CompanionState("Companion State", "See your companion's current support mode."),
    Companions("My Companions", "Create, save, and choose who supports you."),
    Personality("Companion Builder", "Shape identity, voice, look, and support style."),
    Journal("Journal", "A private space to release, reflect, and remember."),
    Reflection("Reflection", "Understand what your feelings are trying to show you."),
    Grounding("Grounding", "Come back to your body, one breath at a time."),
    Memory("Memory", "Control what your companion remembers about you."),
    Preferences("Settings", "Manage your companion, privacy, storage, and support settings."),
    AccountSettings("Account Settings", "Manage sign-in, account access, and sign out."),
    VoiceLiveSettings("Voice & Live Settings", "Choose voice, microphone, speaker, and live avatar options."),
    JournalStorageSettings("Journal Storage", "Choose where entries are saved."),
    PrivacySettings("Privacy Settings", "Control app lock, sync, previews, and data export."),
    NotificationSettings("Notifications", "Choose reminders, check-ins, and gentle prompts."),
    AppearanceSettings("Appearance", "Theme, accents, and companion orb style."),
    Disclaimer("Wellness Disclaimer", "Before we begin."),
    CrisisResources("Crisis Resources", "Support when things feel urgent."),
}

private enum class JournalStorage(val label: String, val body: String) {
    Device("Device only", "Stored locally only. Not synced across devices."),
    Cloud("Cloud only", "Stored in Firestore under your account."),
    Both("Device + Cloud", "Stored locally and in Firestore."),
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
    val supportFocus: Set<String>,
    val shortDescription: String = "Safe, supportive, and personal.",
    val roleplayEnabled: Boolean = true,
    val roleplayStyle: String = "Supportive scene partner",
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
)

private data class ChatMessage(
    val sender: String,
    val text: String,
    val isUser: Boolean = false,
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

    val companions = remember {
        mutableStateListOf(
            CompanionProfile(
                id = "luna",
                name = "Luna",
                gender = "Female",
                voice = "Soft Female",
                personalityTags = listOf("Soft", "Gentle", "Reflective", "Supportive"),
                personalityTraits = listOf("Kind", "Sweet", "Playful"),
                communicationStyle = "Deep",
                supportFocus = setOf("Stress", "Journaling", "Grounding"),
                shortDescription = "Warm, calming, emotionally supportive.",
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
                supportFocus = setOf("Confidence", "Daily motivation", "Creativity"),
                shortDescription = "Protective, steady, and reassuring.",
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

    LaunchedEffect(Unit) {
        delay(850)
        if (route == AppRoute.Splash) route = AppRoute.Welcome
    }

    if (!signedIn) {
        when (route) {
            AppRoute.Splash -> SplashScreen()
            AppRoute.Welcome -> WelcomeScreen(
                onGetStarted = {
                    route = if (!hasAcceptedDisclaimer || (showDisclaimerOnLaunch && !hideDisclaimerOnLaunch)) {
                        AppRoute.Disclaimer
                    } else {
                        AppRoute.Login
                    }
                },
                onLogin = { route = AppRoute.Login },
            )
            AppRoute.Disclaimer -> DisclaimerScreen(
                hasAcceptedDisclaimer = hasAcceptedDisclaimer,
                hideDisclaimerOnLaunch = hideDisclaimerOnLaunch,
                onAcceptedChange = { hasAcceptedDisclaimer = it },
                onHideChange = { hideDisclaimerOnLaunch = it },
                onContinue = {
                    hasAcceptedDisclaimer = true
                    route = AppRoute.Login
                },
                onCrisis = { route = AppRoute.CrisisResources },
            )
            AppRoute.CrisisResources -> CrisisResourcesScreen(onBack = { route = AppRoute.Disclaimer })
            else -> LoginScreen(onSignedIn = {
                signedIn = true
                route = AppRoute.Home
            })
        }
        return
    }

    AppShell(
        route = route,
        activeCompanion = activeCompanion,
        onNavigate = { route = it },
        onSignOut = {
            signedIn = false
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
                    onNavigate = { route = it },
                    onSelectCompanion = { id ->
                        activeCompanionId = id
                        companions.forEachIndexed { index, companion ->
                            companions[index] = companion.copy(isActive = companion.id == id)
                        }
                    },
                )
                AppRoute.Wellness -> WellnessHubScreen(onNavigate = { route = it })
                AppRoute.Chat -> CompanionChatScreen(
                    companion = activeCompanion,
                    onNavigate = { route = it },
                )
                AppRoute.LiveCompanionCall -> LiveCompanionCallScreen(
                    companion = activeCompanion,
                    onNavigate = { route = it },
                )
                AppRoute.CompanionState -> CompanionStateScreen(
                    companion = activeCompanion,
                    onNavigate = { route = it },
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
                    onCreate = { route = AppRoute.Personality },
                    onEdit = { id ->
                        activeCompanionId = id
                        route = AppRoute.Personality
                    },
                    onChat = {
                        activeCompanionId = it
                        route = AppRoute.Chat
                    },
                    onLive = {
                        activeCompanionId = it
                        route = AppRoute.LiveCompanionCall
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
                    onSave = { route = AppRoute.Companions },
                )
                AppRoute.Journal -> JournalScreen(
                    title = journalTitle,
                    body = journalBody,
                    mood = selectedJournalMood,
                    storage = defaultJournalStorage,
                    askEveryTime = askStorageEveryTime,
                    onTitleChange = { journalTitle = it },
                    onBodyChange = { journalBody = it },
                    onMoodChange = { selectedJournalMood = it },
                    onReflect = { route = AppRoute.Reflection },
                    onStorageSheet = { showJournalStorageSheet = true },
                    onStorageChange = { defaultJournalStorage = it },
                )
                AppRoute.Reflection -> ReflectionScreen(onBack = { route = AppRoute.Wellness }, onSaveToJournal = { route = AppRoute.Journal })
                AppRoute.Grounding -> GroundingScreen(onBack = { route = AppRoute.Wellness })
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
                    onNavigate = { route = it },
                    onSignOut = {
                        signedIn = false
                        route = AppRoute.Login
                    },
                )
                AppRoute.AccountSettings -> AccountSettingsScreen(onSignOut = {
                    signedIn = false
                    route = AppRoute.Login
                })
                AppRoute.VoiceLiveSettings -> VoiceLiveSettingsScreen(
                    companion = activeCompanion,
                    onLiveSettings = { route = AppRoute.LiveCompanionCall },
                )
                AppRoute.JournalStorageSettings -> JournalStorageSettingsScreen(
                    defaultJournalStorage = defaultJournalStorage,
                    askStorageEveryTime = askStorageEveryTime,
                    onStorageChange = { defaultJournalStorage = it },
                    onAskEveryTimeChange = { askStorageEveryTime = it },
                )
                AppRoute.PrivacySettings -> PrivacySettingsScreen()
                AppRoute.NotificationSettings -> NotificationSettingsScreen()
                AppRoute.AppearanceSettings -> AppearanceSettingsScreen(companion = activeCompanion)
                AppRoute.Disclaimer -> DisclaimerScreen(
                    hasAcceptedDisclaimer = hasAcceptedDisclaimer,
                    hideDisclaimerOnLaunch = hideDisclaimerOnLaunch,
                    onAcceptedChange = { hasAcceptedDisclaimer = it },
                    onHideChange = { hideDisclaimerOnLaunch = it },
                    onContinue = { route = AppRoute.Preferences },
                    onCrisis = { route = AppRoute.CrisisResources },
                    onBack = { route = AppRoute.Wellness },
                )
                AppRoute.CrisisResources -> CrisisResourcesScreen(onBack = { route = AppRoute.Wellness })
                AppRoute.Splash, AppRoute.Welcome, AppRoute.Login -> HomeDashboardScreen(
                    companion = activeCompanion,
                    companions = companions,
                    selectedMood = selectedMood,
                    onMoodSelected = { selectedMood = it },
                    onNavigate = { route = it },
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

@Composable
private fun AppShell(
    route: AppRoute,
    activeCompanion: CompanionProfile,
    onNavigate: (AppRoute) -> Unit,
    onSignOut: () -> Unit,
    onJournalStorage: () -> Unit,
    onJournalStorageSelected: (JournalStorage) -> Unit,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SoftDrawer(
                currentRoute = route,
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
                    onMenu = { scope.launch { drawerState.open() } },
                    onNavigate = onNavigate,
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
private fun SoftDrawer(currentRoute: AppRoute, onNavigate: (AppRoute) -> Unit) {
    val items = listOf(
        "Home" to AppRoute.Home,
        "Companion Builder" to AppRoute.Personality,
        "Wellness" to AppRoute.Wellness,
        "Journal" to AppRoute.Journal,
        "Chats" to AppRoute.Chat,
        "Live Call" to AppRoute.LiveCompanionCall,
        "Companion State" to AppRoute.CompanionState,
        "Settings" to AppRoute.Preferences,
        "Crisis Resources" to AppRoute.CrisisResources,
        "Disclaimer" to AppRoute.Disclaimer,
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
                    CompanionAvatar(size = 58.dp, label = "FA", glow = true)
                    Column {
                        Text("Fancie AI Companion", color = TextDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Private companion space", color = TextMuted, fontSize = 13.sp)
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
    onMenu: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSignOut: () -> Unit,
    onJournalStorage: () -> Unit,
    onJournalStorageSelected: (JournalStorage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val hideOverflow = route in listOf(
        AppRoute.Preferences,
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
                IconCircleButton(R.drawable.ic_settings, "Settings", onClick = { onNavigate(AppRoute.Preferences) })
                return@Row
            }
            if (route == AppRoute.Chat || route == AppRoute.LiveCompanionCall) {
                CompanionAvatar(size = 42.dp, label = companion.name.take(2), glow = false, photoUri = companion.photoUri, imageResId = companion.imageResId)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (route == AppRoute.Chat) companion.name else route.title,
                    color = TextDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (route == AppRoute.Chat) "Texting with you" else route.subtitle,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!hideOverflow) {
                Box {
                    IconCircleButton(R.drawable.ic_more_vert, "More options", onClick = { expanded = true })
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        overflowItems(route).forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    expanded = false
                                    when (item) {
                                        "Change Companion", "Manage My Companions" -> onNavigate(AppRoute.Companions)
                                        "Open Settings" -> onNavigate(AppRoute.Preferences)
                                        "Voice Settings" -> onNavigate(AppRoute.VoiceLiveSettings)
                                        "Memory Settings" -> onNavigate(AppRoute.Memory)
                                        "Privacy Settings" -> onNavigate(AppRoute.PrivacySettings)
                                        "View Disclaimer" -> onNavigate(AppRoute.Disclaimer)
                                        "View Companion Live", "Live Mode" -> onNavigate(AppRoute.LiveCompanionCall)
                                        "Save Chat to Journal" -> onNavigate(AppRoute.Journal)
                                        "Save Call Notes to Journal" -> onNavigate(AppRoute.Journal)
                                        "Companion Appearance" -> onNavigate(AppRoute.AppearanceSettings)
                                        "End Session" -> onNavigate(AppRoute.Home)
                                        "Storage Location" -> onJournalStorage()
                                        "Save on Device" -> onJournalStorageSelected(JournalStorage.Device)
                                        "Save to Cloud" -> onJournalStorageSelected(JournalStorage.Cloud)
                                        "Save to Both" -> onJournalStorageSelected(JournalStorage.Both)
                                        "Create Companion" -> onNavigate(AppRoute.Personality)
                                        "View Crisis Resources" -> onNavigate(AppRoute.CrisisResources)
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
        AppRoute.Chat -> listOf("View Companion Live", "Change Companion", "Voice Settings", "Memory Settings", "Clear Chat", "Save Chat to Journal")
        AppRoute.LiveCompanionCall -> listOf("Change Companion", "Voice Settings", "Companion Appearance", "Save Call Notes to Journal", "Turn Captions On/Off", "Privacy Settings", "End Session")
        AppRoute.Journal -> listOf("Storage Location", "Save on Device", "Save to Cloud", "Save to Both", "Export Journal", "Clear Draft", "Privacy Settings")
        AppRoute.Companions -> listOf("Create Companion", "Sort Companions", "Show Active Only", "Delete Companion")
        AppRoute.Preferences -> listOf("Reset Settings", "Export Data", "Delete Account Data", "View Disclaimer", "View Crisis Resources")
        else -> listOf("Open Settings", "View Disclaimer", "Sign Out")
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
    var email by remember { mutableStateOf("user@email.com") }
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
            preview = if (savedCompanion.id == companion.id) {
                "I'm here with you. Tell me what's on your heart."
            } else if (savedCompanion.id == "kai") {
                "Take a breath. We'll slow this down and figure it out together."
            } else {
                "Your saved companion is ready when you are."
            },
            time = if (index == 0) "19:07" else savedCompanion.lastUsedDate,
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

            SectionHeader("Saved Companions")
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
                            }
                        },
                    )
                }
            }

            SectionHeader("Chats")
            GlassCard(background = CardDark.copy(alpha = 0.94f)) {
                Text("Continue where you left off", color = TextDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("${companion.name} is ready for chat, live support, or line rehearsal.", color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryGradientButton("New Chat", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Chat) })
                    SecondarySoftButton("Companion Live", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.LiveCompanionCall) })
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                chatPreviews.forEach { preview ->
                    ChatPreviewRow(
                        preview = preview,
                        active = preview.companionId == companion.id,
                        onClick = {
                            onSelectCompanion(preview.companionId)
                            onNavigate(AppRoute.Chat)
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
private fun CompanionChatScreen(companion: CompanionProfile, onNavigate: (AppRoute) -> Unit) {
    val messages = remember {
        mutableStateListOf(
            ChatMessage(companion.name, "I'm here with you. What's been sitting on your heart today?"),
            ChatMessage("You", "I feel overwhelmed.", isUser = true),
            ChatMessage(companion.name, "That makes sense. Let's slow this down together. Is this more about pressure, uncertainty, or feeling unsupported?"),
        )
    }
    var input by remember { mutableStateOf("") }

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
                    )
                    SmallCircleButton("âž¤") {
                        if (input.isNotBlank()) {
                            messages.add(ChatMessage("You", input.trim(), isUser = true))
                            messages.add(ChatMessage(companion.name, "I hear you. You do not have to carry everything at once. Let's choose the next gentle step."))
                            input = ""
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
            SectionHeader("Wellness", "Support, grounding, reflection, and safety resources.")
            GlassCard {
                Text("Choose what kind of support you need right now.", color = TextDark, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("You do not have to carry everything at once. Pick one soft place to begin.", color = TextMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Grounding", "Breathing, reset tools, and body calming.", "calm", CalmBlue, Modifier.weight(1f)) { onNavigate(AppRoute.Grounding) }
                QuickActionCard("Reflection", "Understand patterns and emotional needs.", "spark", Lavender, Modifier.weight(1f)) { onNavigate(AppRoute.Reflection) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard("Check In", "Name your mood and what you need.", "mood", RosePink, Modifier.weight(1f)) { onNavigate(AppRoute.Journal) }
                QuickActionCard("Affirmations", "Give your mind something safe to hold.", "safe", SuccessGreen, Modifier.weight(1f)) { onNavigate(AppRoute.Grounding) }
            }
            GlassCard {
                Text("Self-worth prompt", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("What would feel supportive to hear from someone who truly cares about you?", color = TextMuted)
                SecondarySoftButton("Write in Journal", onClick = { onNavigate(AppRoute.Journal) })
            }
            GlassCard(background = WarningPeach.copy(alpha = 0.30f)) {
                Text("Support & Safety", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Wellness information is supportive only. Crisis resources are here when things feel urgent.", color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondarySoftButton("Disclaimer", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Disclaimer) })
                    SecondarySoftButton("Crisis Resources", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.CrisisResources) })
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
            SectionHeader("My Companions", "Create and save more than one AI companion.")
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
            SectionHeader("Companion Builder", "Create a companion that feels safe, supportive, and personal to you.")
            CompanionImageSquare(companion)
            GlassCard {
                Text("Start from scratch", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Begin fresh, or use Luna or Kai as a polished starter and adjust the details.", color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondarySoftButton(
                        "Luna",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onCompanionChange(
                                companion.copy(
                                    name = "Luna",
                                    gender = "Female",
                                    voice = "Soft Female",
                                    personalityTags = listOf("Soft", "Gentle", "Reflective", "Supportive"),
                                    personalityTraits = listOf("Kind", "Sweet", "Playful"),
                                    communicationStyle = "Deep",
                                    characterMode = "Best Friend",
                                    roleplayStyle = "Wellness Coach",
                                    shortDescription = "Warm, calming, emotionally supportive.",
                                    photoUri = null,
                                    imageResName = "luna_mock",
                                    imageResId = R.drawable.luna_mock,
                                ),
                            )
                        },
                    )
                    SecondarySoftButton(
                        "Kai",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onCompanionChange(
                                companion.copy(
                                    name = "Kai",
                                    gender = "Male",
                                    voice = "Calm Male",
                                    personalityTags = listOf("Calm", "Protective", "Honest", "Grounded"),
                                    personalityTraits = listOf("Protective", "Ambitious", "Kind"),
                                    communicationStyle = "Direct",
                                    characterMode = "Assistant",
                                    roleplayStyle = "Athletic Partner",
                                    shortDescription = "Protective, steady, and reassuring.",
                                    photoUri = null,
                                    imageResName = "kai_mock",
                                    imageResId = R.drawable.kai_mock,
                                ),
                            )
                        },
                    )
                }
                SecondarySoftButton(
                    "Create New",
                    onClick = {
                        onCompanionChange(
                            companion.copy(
                                name = "New Companion",
                                gender = "Custom",
                                voice = "Neutral Calm",
                                personalityTags = listOf("Supportive"),
                                personalityTraits = listOf("Kind"),
                                characterMode = "Best Friend",
                                roleplayStyle = "Wellness Coach",
                                shortDescription = "A custom companion ready to shape.",
                                photoUri = null,
                                imageResName = "",
                                imageResId = null,
                                avatarType = "Glowing Orb",
                            ),
                        )
                    },
                )
            }
            SectionHeader("Basic Info")
            GlassCard {
                RoundedInputField(companion.name, { onCompanionChange(companion.copy(name = it)) }, "Companion name")
                Text("Gender", color = TextDark, fontWeight = FontWeight.SemiBold)
                WrapChips(genderOptions, companion.gender) { gender ->
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
                Text("Photo/avatar type: ${companion.avatarType}", color = TextDark, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Upload Photo", modifier = Modifier.weight(1f), onClick = { photoPicker.launch(arrayOf("image/*")) })
                    SecondarySoftButton("Choose Avatar", modifier = Modifier.weight(1f), onClick = { onCompanionChange(companion.copy(avatarType = "Choose Avatar", photoUri = null)) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Use Glowing Orb", modifier = Modifier.weight(1f), onClick = { onCompanionChange(companion.copy(avatarType = "Glowing Orb", photoUri = null, imageResName = "", imageResId = null)) })
                    SecondarySoftButton("Remove Photo", modifier = Modifier.weight(1f), onClick = { onCompanionChange(companion.copy(avatarType = "Glowing Orb", photoUri = null, photoStoragePath = null, imageResName = "", imageResId = null)) })
                }
                Text("Female Avatars", color = TextDark, fontWeight = FontWeight.SemiBold)
                AvatarChoiceRow(
                    options = listOf("Luna" to R.drawable.luna_mock, "Amara" to null, "Nova" to null, "Selene" to null),
                    selected = companion.avatarType,
                    onSelected = { name, image ->
                        onCompanionChange(companion.copy(avatarType = name, photoUri = null, imageResName = name.lowercase(), imageResId = image))
                    },
                )
                Text("Male Avatars", color = TextDark, fontWeight = FontWeight.SemiBold)
                AvatarChoiceRow(
                    options = listOf("Kai" to R.drawable.kai_mock, "Atlas" to null, "Rome" to null, "Saint" to null),
                    selected = companion.avatarType,
                    onSelected = { name, image ->
                        onCompanionChange(companion.copy(avatarType = name, photoUri = null, imageResName = name.lowercase(), imageResId = image))
                    },
                )
                Text(
                    if (companion.photoUri == null) {
                        "Upload a photo to replace the initials circle across Home, Chat, Live Call, and My Companions."
                    } else {
                        "Photo selected. The companion circles now use this image."
                    },
                    color = TextMuted,
                )
                Text("Firebase Storage later: photoUri and photoStoragePath are already modeled.", color = TextMuted, fontSize = 13.sp)
            }
            SectionHeader("Voice")
            GlassCard {
                VoiceOptionGroup("Female Voice Options", femaleVoiceOptions, companion.voice) { onCompanionChange(companion.copy(voice = it)) }
                VoiceOptionGroup("Male Voice Options", maleVoiceOptions, companion.voice) { onCompanionChange(companion.copy(voice = it)) }
                VoiceOptionGroup("Neutral / Custom Voice Options", neutralVoiceOptions, companion.voice) { onCompanionChange(companion.copy(voice = it)) }
            }

            SectionHeader("Personality Traits", "Choose up to 4 traits.")
            GlassCard {
                TraitPicker(
                    options = personalityTraitOptions,
                    selected = companion.personalityTraits,
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

            SectionHeader("Character Mode")
            characterModes.forEach { (title, body) ->
                SelectableInfoCard(title, body, companion.characterMode == title) {
                    onCompanionChange(companion.copy(characterMode = title))
                }
            }

            SectionHeader("Roleplay Settings")
            GlassCard {
                ToggleRow("Roleplay enabled", companion.roleplayEnabled) { onCompanionChange(companion.copy(roleplayEnabled = it)) }
                MultiSelectChipGrid(
                    options = roleplayStyles,
                    selected = setOf(companion.roleplayStyle),
                    onToggle = { onCompanionChange(companion.copy(roleplayStyle = it)) },
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
            communicationCards.forEach { (title, body) ->
                SelectableInfoCard(title, body, companion.communicationStyle == title) {
                    onCompanionChange(companion.copy(communicationStyle = title))
                }
            }

            SectionHeader("Support Focus")
            GlassCard {
                MultiSelectChipGrid(
                    options = supportOptions,
                    selected = companion.supportFocus,
                    onToggle = { option ->
                        val checked = option in companion.supportFocus
                        val next = if (checked) companion.supportFocus - option else companion.supportFocus + option
                        onCompanionChange(companion.copy(supportFocus = next))
                    }
                )
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
            SoftStatusChip(traits.joinToString(" â€¢ "), Lavender)
        }
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
    when (style) {
        "Athletic Partner" -> {
            Text("Workout encouragement, timing, motivation, and accountability.", color = TextMuted)
            Text("Workout Timer: $workoutTimerState", color = TextDark, fontWeight = FontWeight.SemiBold)
            VoiceOptionGroup("Timer Duration", listOf("30 seconds", "1 minute", "5 minutes", "10 minutes", "15 minutes", "Custom"), timerDuration, onTimerDurationChange)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondarySoftButton("Start Timer", modifier = Modifier.weight(1f), onClick = { onWorkoutTimerStateChange("Running") })
                SecondarySoftButton("Pause Timer", modifier = Modifier.weight(1f), onClick = { onWorkoutTimerStateChange("Paused") })
            }
            SecondarySoftButton("Reset Timer", onClick = { onWorkoutTimerStateChange("Ready") })
            VoiceOptionGroup("Motivation Style", listOf("Soft encouragement", "Trainer energy", "Discipline mode", "Playful push"), motivationStyle, onMotivationStyleChange)
        }
        "Monologue Practice" -> {
            Text("Practice speeches, acting lines, affirmations, presentations, and spoken delivery.", color = TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondarySoftButton("Upload Script", modifier = Modifier.weight(1f), onClick = {})
                SecondarySoftButton("Paste Script", modifier = Modifier.weight(1f), onClick = {})
            }
            RoundedInputField(pastedScript, onPastedScriptChange, "Paste script or monologue here", minLines = 4)
            MultiSelectChipGrid(listOf("Choose user lines", "Choose companion lines", "Line-by-line", "Full read-through", "Repeat after me", "Performance feedback"), emptySet()) {}
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
            MultiSelectChipGrid(listOf("Assign companion lines", "Assign user lines", "Rehearse in chat", "Start scene rehearsal", "Consent and boundaries"), emptySet()) {}
            RoundedInputField(stopWord, onStopWordChange, "Stop word / pause word")
            RoundedInputField(boundaries, onBoundariesChange, "Consent and boundaries", minLines = 3)
        }
        else -> {
            Text("Supportive wellness guidance, grounding, reflection, and encouragement.", color = TextMuted)
            MultiSelectChipGrid(listOf("Gentle", "Direct", "Motivational", "Reflective"), emptySet()) {}
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
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onMoodChange: (String) -> Unit,
    onReflect: () -> Unit,
    onStorageSheet: () -> Unit,
    onStorageChange: (JournalStorage) -> Unit,
) {
    val entries = listOf(
        JournalEntry("April 30, 2026", "Calm", "Today I needed clarity", "I noticed that I feel steadier when I name what I need first.", storage),
        JournalEntry("April 29, 2026", "Heavy", "Letting the pressure speak", "The day was full, but I wrote down the next small thing.", JournalStorage.Device),
    )

    GradientBackground {
        ScreenScroll {
            SectionHeader("Your Journal", "A private space to release, reflect, and remember.")
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
                    SecondarySoftButton("Record Voice Entry", onClick = {})
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
            SectionHeader("Saved Entries")
            entries.forEach { JournalEntryCard(it) }
        }
    }
}

@Composable
private fun WellnessBackButton(onBack: () -> Unit) {
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
            Text("â† Wellness", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
            SectionHeader("Grounding", "Come back to your body, one breath at a time.")
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
            SectionHeader("Memory", "Control what your companion remembers about you.")
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
    val supportMode = if (companion.id == "kai") "Steady Protection Mode" else "Gentle Support Mode"
    val stateMessage = if (companion.id == "kai") {
        "I'm grounded with you. We can slow down, name what's real, and choose the next strong step."
    } else {
        "I'm tuned into softness today. We can move slowly and make space for what you feel."
    }

    GradientBackground {
        ScreenScroll {
            SectionHeader("Companion State", "Your companion adjusts to how you need support.")
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
            PrimaryGradientButton("Start Chat", onClick = { onNavigate(AppRoute.Chat) })
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
    onSignOut: () -> Unit,
) {
    var voiceJournalEnabled by remember { mutableStateOf(true) }
    var memoryEnabled by remember { mutableStateOf(true) }
    var appLock by remember { mutableStateOf(false) }
    var hidePreviews by remember { mutableStateOf(true) }
    var cloudSync by remember { mutableStateOf(false) }

    GradientBackground {
        ScreenScroll {
            SectionHeader("Settings", "Manage your companion, privacy, storage, and support settings.")

            GlassCard {
                Text("Account", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Signed in as: user@email.com", color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondarySoftButton("Manage Account", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.AccountSettings) })
                    SecondarySoftButton("Sign Out", modifier = Modifier.weight(1f), onClick = onSignOut)
                }
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
                MiniStateCard("Live companion", "Voice, avatar, and video placeholders ready")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Edit Companion", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Personality) })
                    SecondarySoftButton("Voice & Live", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.VoiceLiveSettings) })
                }
            }

            GlassCard {
                Text("Journal Settings", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                MiniStateCard("Default save location", defaultJournalStorage.label)
                ToggleRow("Ask every time", askStorageEveryTime, onAskEveryTimeChange)
                ToggleRow("Voice journal enabled", voiceJournalEnabled) { voiceJournalEnabled = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Device", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Device) })
                    SecondarySoftButton("Cloud", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Cloud) })
                    SecondarySoftButton("Both", modifier = Modifier.weight(1f), onClick = { onStorageChange(JournalStorage.Both) })
                }
                SecondarySoftButton("Open Journal Storage", onClick = { onNavigate(AppRoute.JournalStorageSettings) })
            }

            GlassCard {
                Text("Wellness Settings", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                MiniStateCard("Grounding preferences", "Breathing reset and body scan")
                MiniStateCard("Affirmations", "Soft supportive prompts")
                MiniStateCard("Reflection prompts", "Emotional patterns and needs")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondarySoftButton("Grounding", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Grounding) })
                    SecondarySoftButton("Reflection", modifier = Modifier.weight(1f), onClick = { onNavigate(AppRoute.Reflection) })
                }
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
private fun AccountSettingsScreen(onSignOut: () -> Unit) {
    GradientBackground {
        ScreenScroll {
            SectionHeader("Account Settings", "Manage sign-in, account access, and sign out.")
            GlassCard {
                Text("Signed in as", color = TextMuted)
                Text("user@email.com", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
private fun VoiceLiveSettingsScreen(companion: CompanionProfile, onLiveSettings: () -> Unit) {
    var voiceReplies by remember { mutableStateOf(true) }
    var microphone by remember { mutableStateOf(true) }
    var speaker by remember { mutableStateOf(true) }
    var voiceSpeed by remember { mutableStateOf(50f) }

    GradientBackground {
        ScreenScroll {
            SectionHeader("Voice & Live Settings", "Choose voice, microphone, speaker, and live avatar options.")
            GlassCard {
                Text("Companion voice", color = TextDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                MiniStateCard("Companion", companion.name)
                MiniStateCard("Gender", companion.gender)
                MiniStateCard("Voice", companion.voice)
                Text("Voice speed: ${voiceSpeed.toInt()}%", color = TextMuted)
                Slider(value = voiceSpeed, onValueChange = { voiceSpeed = it }, valueRange = 0f..100f)
                ToggleRow("Enable voice replies", voiceReplies) { voiceReplies = it }
                ToggleRow("Enable microphone", microphone) { microphone = it }
                ToggleRow("Speaker output", speaker) { speaker = it }
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
            SectionHeader("Journal Storage", "Choose where entries are saved.")
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
            SectionHeader("Privacy Settings", "Control app lock, sync, previews, and data export.")
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
private fun NotificationSettingsScreen() {
    var dailyPrompt by remember { mutableStateOf(true) }
    var checkIns by remember { mutableStateOf(true) }
    var journalReminders by remember { mutableStateOf(false) }
    var crisisNudges by remember { mutableStateOf(true) }

    GradientBackground {
        ScreenScroll {
            SectionHeader("Notifications", "Choose reminders, check-ins, and gentle prompts.")
            GlassCard {
                ToggleRow("Daily gentle prompt", dailyPrompt) { dailyPrompt = it }
                ToggleRow("Mood check-ins", checkIns) { checkIns = it }
                ToggleRow("Journal reminders", journalReminders) { journalReminders = it }
                ToggleRow("Supportive safety nudges", crisisNudges) { crisisNudges = it }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsScreen(companion: CompanionProfile) {
    GradientBackground {
        ScreenScroll {
            SectionHeader("Appearance", "Theme, accent style, and companion orb style.")
            GlassCard {
                MiniStateCard("Theme", "Soft luxury gradient")
                MiniStateCard("Accent style", "Rose and lavender")
                MiniStateCard("Companion orb style", companion.avatarType)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("Soft Pink", "Lavender", "Calm Blue", "Gold Accent").forEach { SoftFeatureChip(it) }
                }
            }
        }
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
            SectionHeader("Crisis Resources", "Clear help, held gently.")
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundTop, BackgroundMiddle, BackgroundBottom))),
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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        content()
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    background: Color = CardWhite,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, LargeShape)
            .clip(LargeShape)
            .background(background)
            .border(1.dp, Color.White.copy(alpha = 0.14f), LargeShape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

@Composable
private fun PrimaryGradientButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(8.dp, PillShape)
            .clip(PillShape)
            .background(Brush.horizontalGradient(listOf(DeepRose, Lavender, SoftPurple)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondarySoftButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(containerColor = CardAccent.copy(alpha = 0.92f), contentColor = TextPrimary),
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
    GlassCard(modifier = modifier.clickable(onClick = onClick), padding = 16.dp, background = CardDark.copy(alpha = 0.94f)) {
        SoftIcon(icon, color)
        Text(title, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Text(subtitle, color = TextMuted, fontSize = 13.sp)
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = TextDark, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) Text(subtitle, color = TextMuted, fontSize = 15.sp)
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
private fun JournalEntryCard(entry: JournalEntry) {
    GlassCard {
        Text(entry.date, color = TextMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
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
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(CardAccent.copy(alpha = 0.90f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), PillShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
private fun SmallCircleButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(CardAccent.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = DeepRose, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun SoftIcon(label: String, color: Color, compact: Boolean = false) {
    Box(
        modifier = Modifier
            .size(if (compact) 26.dp else 42.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.26f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.take(4), color = color, fontWeight = FontWeight.Bold, fontSize = if (compact) 9.sp else 10.sp, maxLines = 1)
    }
}

@Composable
private fun SoftMenuIcon(label: String) {
    val icon = when {
        label.contains("Companion Builder") -> "CB"
        label.contains("Companion State") -> "CS"
        label.contains("Crisis") -> "911"
        label.contains("Live") -> "LC"
        else -> label.take(1).uppercase()
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(CardAccent.copy(alpha = 0.84f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(icon, color = DeepRose, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
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
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, color = TextDark, fontWeight = FontWeight.SemiBold)
    }
}
