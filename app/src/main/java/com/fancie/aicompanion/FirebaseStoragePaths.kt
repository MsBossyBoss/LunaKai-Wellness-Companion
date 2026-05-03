package com.fancie.aicompanion

import com.google.firebase.auth.FirebaseAuth

object FirebaseStoragePaths {
    fun profile(fileName: String): String = userPath("profile", fileName)

    fun companion(fileName: String): String = userPath("companions", fileName)

    fun journalVoice(fileName: String): String = userPath("journal/voice", fileName)

    fun journalVideo(fileName: String): String = userPath("journal/video", fileName)

    fun capture(fileName: String): String = userPath("captures", fileName)

    fun script(fileName: String): String = userPath("scripts", fileName)

    private fun userPath(folder: String, fileName: String): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: error("A signed-in Firebase Auth user is required before uploading to Storage.")
        return "users/$uid/$folder/${fileName.safeStorageName()}"
    }

    private fun String.safeStorageName(): String {
        return trim()
            .ifBlank { "upload" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
