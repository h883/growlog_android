package com.example.sns_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sns_v1.AppState
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.Comment
import com.example.sns_v1.model.Post
import com.example.sns_v1.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 投稿1件とそのコメントをまとめて扱う。旧 CommentsViewModel の役割を含む */
class PostDetailViewModel(private val postId: String) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getPost(token, postId)
                    .onSuccess { _post.value = it }
                    .onFailure { e -> _error.value = e.message }
                ApiClient.instance.getComments(token, postId).onSuccess { _comments.value = it }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleReaction() {
        val current = _post.value ?: return
        flipReaction()
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.toggleReaction(token, current.postId).onFailure { flipReaction() }
            } catch (_: Exception) {
                flipReaction()
            }
        }
    }

    private fun flipReaction() {
        _post.value = _post.value?.let { post ->
            val nowReacted = !post.myReaction
            post.copy(
                myReaction = nowReacted,
                reactionCount = if (nowReacted) post.reactionCount + 1
                                else maxOf(0, post.reactionCount - 1)
            )
        }
    }

    fun toggleSave() {
        val current = _post.value ?: return
        flipSave()
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.toggleSave(token, current.postId).onFailure { flipSave() }
            } catch (_: Exception) {
                flipSave()
            }
        }
    }

    private fun flipSave() {
        _post.value = _post.value?.let { it.copy(isSaved = !it.isSaved) }
    }

    fun addComment(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.addComment(token, postId, content).onSuccess { commentId ->
                    _comments.value = _comments.value + Comment(
                        commentId = commentId,
                        userId = AppState.userId,
                        displayName = AppState.displayName,
                        userName = AppState.userName,
                        content = content,
                        createdAt = "たった今"
                    )
                    // ヘッダーのコメント数も合わせる
                    _post.value = _post.value?.let { it.copy(commentCount = it.commentCount + 1) }
                }.onFailure { e ->
                    _error.value = "コメントの送信に失敗しました: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isSending.value = false
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.deleteComment(token, postId, commentId).onSuccess {
                    _comments.value = _comments.value.filterNot { it.commentId == commentId }
                    _post.value = _post.value?.let {
                        it.copy(commentCount = maxOf(0, it.commentCount - 1))
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
