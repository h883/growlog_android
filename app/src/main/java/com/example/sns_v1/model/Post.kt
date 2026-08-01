package com.example.sns_v1.model

data class Post(
    val postId: String,
    val userId: String,
    val displayName: String,
    val userName: String,
    val authorImageUrl: String? = null,
    val content: String,
    val postType: String = "progress",
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    /** グループ投稿のときだけ入る */
    val groupId: String? = null,
    val isPinned: Boolean = false,
    val progress: Int? = null,
    val goalTitle: String? = null,
    val reactionCount: Int = 0,
    val commentCount: Int = 0,
    val myReaction: Boolean = false,
    val isSaved: Boolean = false,
    val createdAt: String = ""
)
