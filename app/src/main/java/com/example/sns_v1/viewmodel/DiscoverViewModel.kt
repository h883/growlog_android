package com.example.sns_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.DiscoverData
import com.example.sns_v1.model.SearchResult
import com.example.sns_v1.network.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 350L

class DiscoverViewModel : ViewModel() {

    private val _discover = MutableStateFlow(DiscoverData())
    val discover: StateFlow<DiscoverData> = _discover.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow(SearchResult())
    val results: StateFlow<SearchResult> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadDiscover()
    }

    fun loadDiscover() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getDiscover(token).onSuccess { _discover.value = it }
            } catch (_: Exception) {
                // 初期表示は失敗しても空のまま。検索は独立して使える
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 入力のたびに叩かないよう、最後の入力から一定時間おいて検索する */
    fun onQueryChange(text: String) {
        _query.value = text
        searchJob?.cancel()

        if (text.isBlank()) {
            _results.value = SearchResult()
            _isLoading.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _isLoading.value = true
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.search(token, text).onSuccess { _results.value = it }
            } catch (_: Exception) {
                _results.value = SearchResult()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** タグをタップしたときは、その場で検索欄に入れて即検索する */
    fun searchTag(tag: String) = onQueryChange(tag)

    fun toggleSave(postId: String) {
        _results.value = _results.value.copy(
            posts = _results.value.posts.map {
                if (it.postId == postId) it.copy(isSaved = !it.isSaved) else it
            }
        )
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
        _results.value = _results.value.copy(
            posts = _results.value.posts.map {
                if (it.postId == postId) it.copy(isSaved = !it.isSaved) else it
            }
        )
    }

    fun toggleReaction(postId: String) {
        _results.value = _results.value.copy(
            posts = _results.value.posts.map { post ->
                if (post.postId == postId) {
                    val nowReacted = !post.myReaction
                    post.copy(
                        myReaction = nowReacted,
                        reactionCount = if (nowReacted) post.reactionCount + 1
                                        else maxOf(0, post.reactionCount - 1)
                    )
                } else post
            }
        )
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.toggleReaction(token, postId).onFailure { revertReaction(postId) }
            } catch (_: Exception) {
                revertReaction(postId)
            }
        }
    }

    private fun revertReaction(postId: String) {
        _results.value = _results.value.copy(
            posts = _results.value.posts.map { post ->
                if (post.postId == postId) {
                    val reverted = !post.myReaction
                    post.copy(
                        myReaction = reverted,
                        reactionCount = if (reverted) post.reactionCount + 1
                                        else maxOf(0, post.reactionCount - 1)
                    )
                } else post
            }
        )
    }
}
