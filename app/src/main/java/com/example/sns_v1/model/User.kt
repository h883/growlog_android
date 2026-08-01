package com.example.sns_v1.model

data class User(
    val userId: String = "",
    val displayName: String = "",
    val userName: String = "",
    val biography: String = "",
    val followingCount: Int = 0,
    val followerCount: Int = 0,
    val activeDays: Int = 0,
    val activeGoals: Int = 0,
    val achievedGoals: Int = 0
)
