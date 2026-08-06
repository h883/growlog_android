package com.example.sns_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.FocusRoom
import com.example.sns_v1.model.Group
import com.example.sns_v1.model.GroupMember
import com.example.sns_v1.model.Post
import com.example.sns_v1.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupDetailViewModel(private val groupId: String) : ViewModel() {

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 集中ルーム。メンバーの共同の森を出すのに使う */
    private val _focusRoom = MutableStateFlow<FocusRoom?>(null)
    val focusRoom: StateFlow<FocusRoom?> = _focusRoom.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = TokenManager.getIdToken()
                val group = ApiClient.instance.getGroup(token, groupId).getOrElse { e ->
                    _error.value = e.message
                    return@launch
                }
                _group.value = group

                // 非公開グループに未参加なら、中身は取りに行かない
                if (group.canViewContent) {
                    ApiClient.instance.getGroupPosts(token, groupId).onSuccess { _posts.value = it }
                    ApiClient.instance.getGroupMembers(token, groupId).onSuccess { _members.value = it }
                    // 集中ルームはメンバーだけが見られる。403 は無視して森を出さない
                    ApiClient.instance.getFocusRoom(token, groupId)
                        .onSuccess { _focusRoom.value = it }
                } else {
                    _posts.value = emptyList()
                    _members.value = emptyList()
                    _focusRoom.value = null
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun join() {
        viewModelScope.launch {
            _error.value = null
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.joinGroup(token, groupId)
                    .onSuccess { load() }
                    .onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun leave() {
        viewModelScope.launch {
            _error.value = null
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.leaveGroup(token, groupId)
                    .onSuccess { load() }
                    .onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun invite(userName: String) {
        val normalized = userName.trim().removePrefix("@")
        if (normalized.isBlank()) return
        viewModelScope.launch {
            _error.value = null
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.inviteToGroup(token, groupId, normalized)
                    .onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun post(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _isPosting.value = true
            _error.value = null
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.createGroupPost(token, groupId, content.trim())
                    .onSuccess { load() }
                    .onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isPosting.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
