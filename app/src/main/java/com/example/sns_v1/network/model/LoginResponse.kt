package com.example.sns_v1.network.model

data class LoginResponse(
    val userId: String,
    val userName: String,
    val displayName: String,
    val isNewUser: Boolean
)
