package com.example.sns_v1.model

data class UserProfile(
    val userId: String,
    val displayName: String,
    val userName: String,
    val biography: String,
    val profileImageUrl: String? = null,
    val followerCount: Int,
    val followingCount: Int,
    val isFollowing: Boolean,
    val postCount: Int,
    val goalCount: Int
)
