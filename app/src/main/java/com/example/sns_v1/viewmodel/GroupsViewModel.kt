package com.example.sns_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.Group
import com.example.sns_v1.model.GroupVisibility
import com.example.sns_v1.network.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 350L

class GroupsViewModel : ViewModel() {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** true なら自分が参加しているグループだけを出す */
    private val _mineOnly = MutableStateFlow(false)
    val mineOnly: StateFlow<Boolean> = _mineOnly.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var searchJob: Job? = null

    init {
        load()
    }

    fun load() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getGroups(token, _query.value, _mineOnly.value)
                    .onSuccess { _groups.value = it }
                    .onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onQueryChange(text: String) {
        _query.value = text
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            load()
        }
    }

    fun setMineOnly(value: Boolean) {
        if (_mineOnly.value == value) return
        _mineOnly.value = value
        load()
    }

    fun createGroup(
        groupName: String,
        groupSlug: String,
        description: String,
        visibility: GroupVisibility,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _error.value = null
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.createGroup(token, groupName, groupSlug, description, visibility)
                    .onSuccess { groupId ->
                        load()
                        onCreated(groupId)
                    }
                    .onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
