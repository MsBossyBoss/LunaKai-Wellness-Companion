package com.fancie.aicompanion

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseChatRepository(
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
) {
    suspend fun saveExchange(
        companionId: String,
        companionName: String,
        userText: String,
        companionText: String,
        mode: String = "text",
        chatId: String = stableChatIdForCompanion(companionId),
    ): Result<Unit> = runCatching {
        val auth = authProvider()
        val firestore = firestoreProvider()
        val user = signedInUser(auth)
        val chatRef = firestore
            .collection("users")
            .document(user.uid)
            .collection("chats")
            .document(chatId)
        val userMessageRef = chatRef.collection("messages").document()
        val companionMessageRef = chatRef.collection("messages").document()

        Log.d("FancieFirestore", "Writing chat summary: ${chatRef.path}")
        Log.d("FancieFirestore", "Writing user message: ${userMessageRef.path}")
        Log.d("FancieFirestore", "Writing companion message: ${companionMessageRef.path}")

        firestore.runBatch { batch ->
            batch.set(chatRef, mapOf(
                "companionId" to companionId,
                "companionName" to companionName,
                "lastMessage" to companionText,
                "mode" to mode,
                "updatedAt" to System.currentTimeMillis(),
            ), SetOptions.merge())
            batch.set(userMessageRef, mapOf(
                "id" to userMessageRef.id,
                "chatId" to chatId,
                "companionId" to companionId,
                "sender" to ChatMessage.SENDER_USER,
                "text" to userText,
                "createdAt" to System.currentTimeMillis(),
                "mode" to mode,
            ))
            batch.set(companionMessageRef, mapOf(
                "id" to companionMessageRef.id,
                "chatId" to chatId,
                "sender" to ChatMessage.SENDER_COMPANION,
                "companionId" to companionId,
                "text" to companionText,
                "createdAt" to System.currentTimeMillis(),
                "mode" to mode,
            ))
        }.awaitTask()
    }

    private fun signedInUser(auth: FirebaseAuth): FirebaseUser {
        val user = auth.currentUser
        if (user == null) {
            Log.w("FancieFirestore", "User must be signed in before saving chat.")
            error("User must be signed in before saving chat.")
        }
        return user
    }
}

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}
