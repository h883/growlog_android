package com.example.sns_v1.model

data class Notification(
    val notificationId: String,
    val type: String,
    val actorDisplayName: String,
    val actorUserName: String,
    val actorImageUrl: String? = null,
    val postId: String?,
    val postContent: String?,
    val commentContent: String?,
    val groupId: String? = null,
    val groupName: String? = null,
    val groupJoinRequestId: String? = null,
    val groupInvitationId: String? = null,
    val isRead: Boolean,
    val createdAt: String
)
