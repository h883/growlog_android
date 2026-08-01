package com.example.sns_v1.model

/** 公開範囲。仕様書 3 章に対応 */
enum class GroupVisibility(val value: String, val label: String, val description: String) {
    PUBLIC("public", "公開", "誰でも閲覧・参加できます"),
    APPROVAL("approval", "承認制", "参加には管理者の承認が必要です"),
    PRIVATE("private", "非公開", "招待された人だけが参加できます");

    companion object {
        fun from(value: String?): GroupVisibility =
            entries.firstOrNull { it.value == value } ?: PUBLIC
    }
}

data class Group(
    val groupId: String,
    val groupSlug: String,
    val groupName: String,
    val description: String = "",
    val visibility: GroupVisibility = GroupVisibility.PUBLIC,
    val joinType: String = "open",
    val memberCount: Int = 1,
    val maximumMembers: Int = 500,
    val iconImageUrl: String? = null,
    val coverImageUrl: String? = null,
    /** owner / admin / moderator / member。未参加なら null */
    val myRole: String? = null,
    val isMember: Boolean = false,
    val isOwner: Boolean = false,
    /** 投稿やメンバー一覧を見られるか（非公開グループの外部ユーザーは false） */
    val canViewContent: Boolean = true
)

data class GroupMember(
    val userId: String,
    val displayName: String,
    val userName: String,
    val profileImageUrl: String? = null,
    val role: String,
    val joinedAt: String
) {
    val roleLabel: String
        get() = when (role) {
            "owner" -> "オーナー"
            "admin" -> "管理者"
            "moderator" -> "モデレーター"
            else -> "メンバー"
        }
}
