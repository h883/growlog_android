package com.example.sns_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sns_v1.AppState
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.FocusMode
import com.example.sns_v1.model.FocusSession
import com.example.sns_v1.model.FocusVisibility
import com.example.sns_v1.model.Garden
import com.example.sns_v1.model.GardenTheme
import com.example.sns_v1.model.Goal
import com.example.sns_v1.network.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 完了直後に振り返り画面へ渡す内容 */
data class FocusResult(
    val activityTitle: String,
    val actualDurationSeconds: Int,
    val breakCount: Int,
    val gardenTheme: GardenTheme = GardenTheme.PLANT,
    val gardenStage: Int = 0
)

class FocusViewModel : ViewModel() {

    /** 自分の進行中セッション */
    private val _mySession = MutableStateFlow<FocusSession?>(null)
    val mySession: StateFlow<FocusSession?> = _mySession.asStateFlow()

    /** 他の人の集中状況 */
    private val _others = MutableStateFlow<List<FocusSession>>(emptyList())
    val others: StateFlow<List<FocusSession>> = _others.asStateFlow()

    /** 表示用の経過秒。1秒ごとに自前で進め、取得のたびにサーバー値へ合わせ直す */
    private val _elapsed = MutableStateFlow(0)
    val elapsed: StateFlow<Int> = _elapsed.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _result = MutableStateFlow<FocusResult?>(null)
    val result: StateFlow<FocusResult?> = _result.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 開始ダイアログの初期選択。前回選んだテーマを覚えておく */
    private val _lastTheme = MutableStateFlow(GardenTheme.PLANT)
    val lastTheme: StateFlow<GardenTheme> = _lastTheme.asStateFlow()

    /** 今週の庭 */
    private val _garden = MutableStateFlow(Garden())
    val garden: StateFlow<Garden> = _garden.asStateFlow()

    private var tickJob: Job? = null

    init {
        load()
        loadGoals()
        loadGarden()
    }

    fun loadGarden() {
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getGarden(token).onSuccess { garden ->
                    _garden.value = garden
                    // 前回育てたものを次回の初期選択にする
                    garden.plants.lastOrNull()?.let { _lastTheme.value = it.gardenTheme }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getActiveFocusSessions(token)
                    .onSuccess { sessions ->
                        val mine = sessions.firstOrNull { it.userId == AppState.userId }
                        _mySession.value = mine
                        _others.value = sessions.filterNot { it.userId == AppState.userId }
                        _elapsed.value = mine?.elapsedSeconds ?: 0
                        restartTicker()
                    }
                    .onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadGoals() {
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.getMyGoals(token)
                    .onSuccess { goals -> _goals.value = goals.filterNot { it.isAchieved } }
            } catch (_: Exception) {
            }
        }
    }

    /** 休憩中は時間を進めない */
    private fun restartTicker() {
        tickJob?.cancel()
        val session = _mySession.value ?: return
        if (session.status != "active") return

        tickJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsed.value = _elapsed.value + 1
            }
        }
    }

    fun start(
        activityTitle: String,
        goalId: String?,
        plannedMinutes: Int?,
        visibility: FocusVisibility,
        mode: FocusMode,
        gardenTheme: GardenTheme = GardenTheme.PLANT,
        groupId: String? = null
    ) {
        viewModelScope.launch {
            _error.value = null
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.startFocusSession(
                    idToken = token,
                    activityTitle = activityTitle,
                    goalId = goalId,
                    groupId = groupId,
                    plannedDurationSeconds = plannedMinutes?.let { it * 60 },
                    visibility = visibility,
                    mode = mode,
                    gardenTheme = gardenTheme
                ).onSuccess {
                    _lastTheme.value = gardenTheme
                    load()
                }
                    .onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun pause() = sessionAction { token, id -> ApiClient.instance.pauseFocusSession(token, id) }

    fun resume() = sessionAction { token, id -> ApiClient.instance.resumeFocusSession(token, id) }

    private fun sessionAction(action: suspend (String, String) -> Result<*>) {
        val session = _mySession.value ?: return
        viewModelScope.launch {
            _error.value = null
            try {
                val token = TokenManager.getIdToken()
                action(token, session.focusSessionId)
                    .onSuccess { load() }
                    .onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /** 終了。完了なら振り返り画面へ渡す結果を作る */
    fun finish(completed: Boolean, endReason: String? = null) {
        val session = _mySession.value ?: return
        viewModelScope.launch {
            _error.value = null
            try {
                val token = TokenManager.getIdToken()
                val response = if (completed) {
                    ApiClient.instance.completeFocusSession(token, session.focusSessionId)
                } else {
                    ApiClient.instance.cancelFocusSession(token, session.focusSessionId, endReason)
                }
                response.onSuccess { json ->
                    tickJob?.cancel()
                    // 途中終了でも、育った分は結果として見せる
                    _result.value = FocusResult(
                        activityTitle = json.optString("activityTitle", session.activityTitle),
                        actualDurationSeconds = json.optInt("actualDurationSeconds", _elapsed.value),
                        breakCount = json.optInt("breakCount", session.breakCount),
                        gardenTheme = GardenTheme.from(json.optString("gardenTheme")),
                        gardenStage = json.optInt("gardenStage", session.gardenStage)
                    )
                    _mySession.value = null
                    _elapsed.value = 0
                    load()
                    loadGarden()
                }.onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /** 振り返りを投稿として残す（仕様書 8） */
    fun postReflection(result: FocusResult, text: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                val content = buildString {
                    appendLine("集中セッション完了：${result.activityTitle}")
                    appendLine("集中時間 ${com.example.sns_v1.model.formatDurationJa(result.actualDurationSeconds)}・休憩${result.breakCount}回")
                    if (text.isNotBlank()) {
                        appendLine()
                        append(text.trim())
                    }
                }
                ApiClient.instance.createPost(
                    idToken = token,
                    content = content.trim(),
                    postType = "progress"
                ).onSuccess {
                    _result.value = null
                    onDone()
                }.onFailure { e -> _error.value = e.message }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun dismissResult() {
        _result.value = null
    }

    fun cheer(session: FocusSession) = react(session, "cheer")

    /** そっと水をあげる。相手には終了後にまとめて届く */
    fun water(session: FocusSession) = react(session, "water")

    private fun react(session: FocusSession, type: String) {
        viewModelScope.launch {
            try {
                val token = TokenManager.getIdToken()
                ApiClient.instance.sendFocusReaction(token, session.focusSessionId, type)
                    .onSuccess { load() }
            } catch (_: Exception) {
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        tickJob?.cancel()
        super.onCleared()
    }
}
