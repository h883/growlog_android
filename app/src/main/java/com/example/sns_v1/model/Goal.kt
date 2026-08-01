package com.example.sns_v1.model

data class Goal(
    val goalId: String,
    val title: String,
    val description: String,
    val progress: Int,
    val startDate: String,
    val endDate: String? = null,
    val isAchieved: Boolean = false,
    val activityCount: Int = 0
)
