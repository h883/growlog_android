package com.example.sns_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sns_v1.AppState
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.Goal
import com.example.sns_v1.model.Post
import com.example.sns_v1.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileData(
    val displayName: String,
    val userName: String,
    val biography: String,
    val profileImageUrl: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
)

class ProfileViewModel : ViewModel() {

    private val _profile = MutableStateFlow(
        ProfileData(AppState.displayName, AppState.userName, "")
    )
    val profile: StateFlow<ProfileData> = _profile.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _savedPosts = MutableStateFlow<List<Post>>(emptyList())
    val savedPosts: StateFlow<List<Post>> = _savedPosts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadProfile()
    }

    /**
     * アバター画像をアップロードし、プロフィールに紐付ける。
     * KV は書き込み直後に読めないことがあるので、返ってきた URL で即座に表示を差し替える。
     */
    fun updateAvatar(bytes: ByteArray) {
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            _errorMessage.value = null
            try {
                val token = TokenManager.getIdToken()
                val upload = ApiClient.instance.uploadAvatar(token, bytes, "image/jpeg")
                    .getOrElse { e ->
                        _errorMessage.value = "画像のアップロードに失敗しました: ${e.message}"
                        return@launch
                    }
                ApiClient.instance.updateProfile(token, profileImageKey = upload.imageKey)
                    .onSuccess {
                        _profile.value = _profile.value.copy(profileImageUrl = upload.imageUrl)
                    }
                    .onFailure { e ->
                        _errorMessage.value = "プロフィールの更新に失敗しました: ${e.message}"
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun updateGoalProgress(goalId: String, progress: Int) {
        val clamped = progress.coerceIn(0, 100)
        patchGoal(goalId, { it.copy(progress = clamped) }) { token ->
            ApiClient.instance.updateGoal(token, goalId, progress = clamped)
        }
    }

    fun setGoalAchieved(goalId: String, achieved: Boolean) {
        patchGoal(goalId, { it.copy(isAchieved = achieved) }) { token ->
            ApiClient.instance.updateGoal(token, goalId, isAchieved = achieved)
        }
    }

    /** 先に画面へ反映し、失敗したらサーバの値を取り直して整合させる */
    private fun patchGoal(
        goalId: String,
        apply: (Goal) -> Goal,
        request: suspend (String) -> Result<Unit>
    ) {
        _goals.value = _goals.value.map { if (it.goalId == goalId) apply(it) else it }
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                request(token).onFailure { e ->
                    _errorMessage.value = "目標の更新に失敗しました: ${e.message}"
                    loadProfile()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                loadProfile()
            }
        }
    }

    fun createGoal(title: String, description: String, startDate: String, endDate: String?) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.createGoal(token, title, description, startDate, endDate)
                    .onSuccess { loadProfile() }
                    .onFailure { e -> _errorMessage.value = "目標の作成に失敗しました: ${e.message}" }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun saveProfile(displayName: String, biography: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.updateProfile(token, displayName = displayName, biography = biography)
                    .onSuccess {
                        _profile.value = _profile.value.copy(
                            displayName = displayName,
                            biography = biography
                        )
                        AppState.displayName = displayName
                        onDone()
                    }
                    .onFailure { e -> _errorMessage.value = "保存に失敗しました: ${e.message}" }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    /** 保存タブから外したものは、その場でリストから消す */
    fun toggleSave(postId: String) {
        val wasSaved = _savedPosts.value.any { it.postId == postId }
        _posts.value = _posts.value.map {
            if (it.postId == postId) it.copy(isSaved = !it.isSaved) else it
        }
        if (wasSaved) {
            _savedPosts.value = _savedPosts.value.filterNot { it.postId == postId }
        }

        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.toggleSave(token, postId).onFailure { loadProfile() }
            } catch (_: Exception) {
                loadProfile()
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getMe(token).onSuccess { json ->
                    _profile.value = ProfileData(
                        displayName = json.optString("display_name", AppState.displayName),
                        userName = json.optString("user_name", AppState.userName),
                        biography = json.optString("biography", ""),
                        profileImageUrl = if (json.isNull("profileImageUrl")) null
                                          else json.optString("profileImageUrl").ifBlank { null },
                        followerCount = json.optInt("followerCount", 0),
                        followingCount = json.optInt("followingCount", 0),
                    )
                }
                ApiClient.instance.getMyGoals(token).onSuccess { goals ->
                    _goals.value = goals
                }
                ApiClient.instance.getMyPosts(token).onSuccess { posts ->
                    _posts.value = posts
                }
                ApiClient.instance.getSavedPosts(token).onSuccess { saved ->
                    _savedPosts.value = saved
                }
            } catch (_: Exception) {
                // keep AppState fallback already set in initial value
            } finally {
                _isLoading.value = false
            }
        }
    }
}
