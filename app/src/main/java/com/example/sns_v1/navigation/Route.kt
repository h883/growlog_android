package com.example.sns_v1.navigation

sealed class Route(val route: String) {
    object Login : Route("login")
    object Home : Route("home")
    object Discover : Route("discover")
    object CreatePost : Route("create_post")
    object Notifications : Route("notifications")
    object Profile : Route("profile")
    object EditProfile : Route("profile/edit")
    object Settings : Route("settings")
    object Messages : Route("messages")
    object Groups : Route("groups")
    object GroupDetail : Route("groups/{groupId}") {
        fun createRoute(groupId: String) = "groups/$groupId"
    }
    object Chat : Route("messages/{conversationId}") {
        fun createRoute(conversationId: String) = "messages/$conversationId"
    }
    object PostDetail : Route("posts/{postId}") {
        fun createRoute(postId: String) = "posts/$postId"
    }
    object UserProfile : Route("users/{userName}") {
        fun createRoute(userName: String) = "users/$userName"
    }
}
