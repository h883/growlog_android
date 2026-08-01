package com.example.sns_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.Post
import com.example.sns_v1.model.UserProfile
import com.example.sns_v1.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfileViewModel(private val userName: String) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getUserProfile(token, userName).onSuccess { _profile.value = it }
                ApiClient.instance.getUserPosts(token, userName).onSuccess { _posts.value = it }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** DM を開く。会話が無ければ作られ、その conversationId を [onReady] に渡す */
    fun openConversation(onReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.openConversation(token, userName).onSuccess(onReady)
            } catch (_: Exception) {
            }
        }
    }

    fun toggleSave(postId: String) {
        flipSave(postId)
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.toggleSave(token, postId).onFailure { flipSave(postId) }
            } catch (_: Exception) {
                flipSave(postId)
            }
        }
    }

    private fun flipSave(postId: String) {
        _posts.value = _posts.value.map {
            if (it.postId == postId) it.copy(isSaved = !it.isSaved) else it
        }
    }

    fun toggleReaction(postId: String) {
        flipReaction(postId)
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.toggleReaction(token, postId).onFailure { flipReaction(postId) }
            } catch (_: Exception) {
                flipReaction(postId)
            }
        }
    }

    private fun flipReaction(postId: String) {
        _posts.value = _posts.value.map { post ->
            if (post.postId == postId) {
                val nowReacted = !post.myReaction
                post.copy(
                    myReaction = nowReacted,
                    reactionCount = if (nowReacted) post.reactionCount + 1
                                    else maxOf(0, post.reactionCount - 1)
                )
            } else post
        }
    }

    fun toggleFollow() {
        val current = _profile.value ?: return
        _profile.value = current.copy(
            isFollowing = !current.isFollowing,
            followerCount = if (!current.isFollowing) current.followerCount + 1
                           else maxOf(0, current.followerCount - 1)
        )
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.toggleFollow(token, userName).onFailure {
                    _profile.value = current
                }
            } catch (_: Exception) {
                _profile.value = current
            }
        }
    }
}
