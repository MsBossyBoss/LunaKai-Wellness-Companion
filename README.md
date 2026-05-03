# LUNAKAI Wellness Companion

LUNAKAI Wellness Companion is an Android app built with Kotlin and Jetpack Compose for private AI companion chats, voice-style companion experiences, journaling, wellness support, and customizable Luna/Kai-style companions.

## Features

- Kotlin + Jetpack Compose Android UI
- Firebase Authentication sign-in
- Firestore-backed companion chat history under each signed-in user
- Gemini-powered text chat and live companion integrations
- Stable per-companion chat IDs
- Portrait-only app experience
- Local/private chat storage options
- Companion customization for voice, personality, support focus, roleplay settings, and avatar assets
- Luna/Kai companion assets and custom companion support
- Journal, wellness, memory, profile, and preference screens

## Privacy Notes

Chat history is designed to save under the signed-in Firebase user path:

```text
users/{uid}/chats/{chatId}/messages/{messageId}
```

Firebase configuration files, local screenshots, build outputs, and local Android Studio files should not be committed to public source control.

## Local Setup

1. Open the project in Android Studio.
2. Add your Firebase Android config at:

```text
app/google-services.json
```

3. Make sure Firebase Authentication, Firestore, Storage, and Firebase AI/Gemini setup are enabled for your Firebase project.
4. Build and run the app from Android Studio, or run:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

## Important

This app includes wellness support features, but it is not a therapist, doctor, emergency service, or crisis service.
