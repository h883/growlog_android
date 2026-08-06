package com.example.sns_v1.model

/** DM の会話一覧に出す1行分 */
data class Conversation(
    val conversationId: String,
    val partnerUserId: String,
    val partnerDisplayName: String,
    val partnerUserName: String,
    val partnerImageUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Int = 0,
    /** 相手がいま集中していれば入る */
    val partnerFocus: FocusPresence? = null
)

data class Message(
    val messageId: String,
    val senderId: String,
    val content: String,
    val isMine: Boolean,
    val isRead: Boolean,
    val createdAt: String
)

data class ChatThread(
    val conversationId: String,
    val partner: UserSummary? = null,
    val messages: List<Message> = emptyList()
)
