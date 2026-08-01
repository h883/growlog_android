package com.example.sns_v1.model

/** 検索結果やおすすめ一覧に出す、軽量なユーザー情報 */
data class UserSummary(
    val userId: String,
    val displayName: String,
    val userName: String,
    val biography: String = "",
    val profileImageUrl: String? = null,
    val followerCount: Int = 0
)

data class TrendingTag(val tag: String, val count: Int)

data class DiscoverData(
    val trendingTags: List<TrendingTag> = emptyList(),
    /** 目標が紐付いた最近の投稿（「今週の挑戦」として表示する） */
    val suggestedPosts: List<Post> = emptyList(),
    val suggestedUsers: List<UserSummary> = emptyList()
)

data class SearchResult(
    val posts: List<Post> = emptyList(),
    val users: List<UserSummary> = emptyList()
)
