package com.example.sns_v1.model

data class Comment(
    val commentId: String,
    val userId: String,
    val displayName: String,
    val userName: String,
    val userImageUrl: String? = null,
    val content: String,
    val createdAt: String
)
