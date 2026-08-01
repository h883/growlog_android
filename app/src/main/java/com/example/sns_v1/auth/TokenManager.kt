package com.example.sns_v1.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object TokenManager {
    suspend fun getIdToken(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw Exception("Not logged in")
        return suspendCancellableCoroutine { cont ->
            user.getIdToken(false)
                .addOnSuccessListener { result ->
                    val token = result.token
                    if (token != null) cont.resume(token)
                    else cont.resumeWithException(Exception("Token is null"))
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }
}
