package com.example.sns_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.Goal
import com.example.sns_v1.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 投稿に紐付ける目標の選択肢を持つだけの ViewModel */
class CreatePostViewModel : ViewModel() {

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getMyGoals(token).onSuccess { goals ->
                    // 達成済みは選択肢に出さない
                    _goals.value = goals.filterNot { it.isAchieved }
                }
            } catch (_: Exception) {
                // 目標が取れなくても本文だけの投稿はできる
            }
        }
    }
}
