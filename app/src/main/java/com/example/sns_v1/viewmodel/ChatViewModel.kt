package com.example.sns_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sns_v1.AppState
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.Message
import com.example.sns_v1.model.UserSummary
import com.example.sns_v1.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val conversationId: String) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _partner = MutableStateFlow<UserSummary?>(null)
    val partner: StateFlow<UserSummary?> = _partner.asStateFlow()

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
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getMessages(token, conversationId)
                    .onSuccess { thread ->
                        _messages.value = thread.messages
                        _partner.value = thread.partner
                    }
                    .onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 送信前に自分の吹き出しを出しておき、失敗したら取り消す */
    fun send(content: String) {
        val text = content.trim()
        if (text.isBlank()) return

        val pendingId = "pending-${System.currentTimeMillis()}"
        _messages.value = _messages.value + Message(
            messageId = pendingId,
            senderId = AppState.userId,
            content = text,
            isMine = true,
            isRead = false,
            createdAt = "送信中"
        )

        viewModelScope.launch {
            _isSending.value = true
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.sendMessage(token, conversationId, text)
                    .onSuccess { messageId ->
                        _messages.value = _messages.value.map {
                            if (it.messageId == pendingId)
                                it.copy(messageId = messageId, createdAt = "たった今")
                            else it
                        }
                    }
                    .onFailure { e ->
                        _messages.value = _messages.value.filterNot { it.messageId == pendingId }
                        _error.value = "送信に失敗しました: ${e.message}"
                    }
            } catch (e: Exception) {
                _messages.value = _messages.value.filterNot { it.messageId == pendingId }
                _error.value = e.message
            } finally {
                _isSending.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
