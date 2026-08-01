package com.example.sns_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.Post
import com.example.sns_v1.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ホームのタブに対応するフィード種別。[apiValue] が null のときは新着順 */
enum class Feed(val apiValue: String?) {
    POPULAR("popular"),
    FOLLOWING("following"),
    LATEST(null)
}

class PostsViewModel : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _feed = MutableStateFlow(Feed.POPULAR)
    val feed: StateFlow<Feed> = _feed.asStateFlow()

    init {
        loadPosts()
    }

    fun selectFeed(feed: Feed) {
        if (_feed.value == feed) return
        _feed.value = feed
        _posts.value = emptyList()
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val requested = _feed.value
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getPosts(token, feed = requested.apiValue)
                    .onSuccess { result ->
                        // 読み込み中にタブが切り替わっていたら、その結果は捨てる
                        if (_feed.value == requested) _posts.value = result.posts
                    }
                    .onFailure { e -> if (_feed.value == requested) _error.value = e.message }
            } catch (e: Exception) {
                if (_feed.value == requested) _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleSave(postId: String) {
        _posts.value = _posts.value.map {
            if (it.postId == postId) it.copy(isSaved = !it.isSaved) else it
        }
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.toggleSave(token, postId).onFailure { revertSave(postId) }
            } catch (_: Exception) {
                revertSave(postId)
            }
        }
    }

    private fun revertSave(postId: String) {
        _posts.value = _posts.value.map {
            if (it.postId == postId) it.copy(isSaved = !it.isSaved) else it
        }
    }

    fun toggleReaction(postId: String) {
        _posts.value = _posts.value.map { post ->
            if (post.postId == postId) {
                val nowReacted = !post.myReaction
                post.copy(
                    myReaction = nowReacted,
                    reactionCount = if (nowReacted) post.reactionCount + 1 else maxOf(0, post.reactionCount - 1)
                )
            } else post
        }
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.toggleReaction(token, postId).onFailure {
                    revertReaction(postId)
                }
            } catch (_: Exception) {
                revertReaction(postId)
            }
        }
    }

    private fun revertReaction(postId: String) {
        _posts.value = _posts.value.map { post ->
            if (post.postId == postId) {
                val reverted = !post.myReaction
                post.copy(
                    myReaction = reverted,
                    reactionCount = if (reverted) post.reactionCount + 1 else maxOf(0, post.reactionCount - 1)
                )
            } else post
        }
    }

    fun prependPost(post: Post) {
        _posts.value = listOf(post) + _posts.value
    }
}
