package com.example.sns_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.Notification
import com.example.sns_v1.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val unreadCount get() = _notifications.value.count { !it.isRead }

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getNotifications(token).onSuccess {
                    _notifications.value = it
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.markNotificationsRead(token).onSuccess {
                    _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun respondToGroupAction(notification: Notification, accept: Boolean) {
        val groupId = notification.groupId ?: return
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                val result = when (notification.type) {
                    "group_join_request" -> notification.groupJoinRequestId?.let {
                        ApiClient.instance.decideGroupJoinRequest(token, groupId, it, accept)
                    }
                    "group_invitation" -> notification.groupInvitationId?.let {
                        ApiClient.instance.decideGroupInvitation(token, groupId, it, accept)
                    }
                    else -> null
                }
                result?.onSuccess { loadNotifications() }
            } catch (_: Exception) {
            }
        }
    }
}
