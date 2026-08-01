package com.example.sns_v1.model

/** 公開範囲。仕様書 3 章 */
enum class FocusVisibility(val value: String, val label: String) {
    ALL("all", "全体公開"),
    FOLLOWERS("followers", "フォロワーのみ"),
    GROUP("group", "所属グループのみ"),
    PRIVATE("private", "自分のみ");

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.value == value } ?: FOLLOWERS
    }
}

/** 集中モード。仕様書 7 章。端末のアプリ制限はネイティブ実装時に追加する */
enum class FocusMode(val value: String, val label: String, val description: String) {
    LIGHT("light", "ライト", "アプリ内通知を止めて集中画面を表示します"),
    FOCUS("focus", "フォーカス", "途中解除できます（アプリ制限は今後対応）"),
    SERIOUS("serious", "本気モード", "終了時に確認画面と理由の記録があります");

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.value == value } ?: LIGHT
    }
}

data class FocusSession(
    val focusSessionId: String,
    val userId: String,
    val displayName: String,
    val userName: String,
    val profileImageUrl: String? = null,
    val activityTitle: String,
    val goalTitle: String? = null,
    val groupId: String? = null,
    /** active / paused / completed / cancelled */
    val status: String = "active",
    val visibility: FocusVisibility = FocusVisibility.FOLLOWERS,
    val focusMode: FocusMode = FocusMode.LIGHT,
    val plannedDurationSeconds: Int? = null,
    val actualDurationSeconds: Int? = null,
    /** サーバーが確定させた経過秒数。端末側はここから自前で加算する */
    val elapsedSeconds: Int = 0,
    val breakCount: Int = 0,
    val cheerCount: Int = 0,
    val myReactions: List<String> = emptyList()
) {
    val isPaused: Boolean get() = status == "paused"

    /** 予定時間に対する進捗。予定が無ければ null */
    fun progressPercent(elapsed: Int): Int? {
        val planned = plannedDurationSeconds ?: return null
        if (planned <= 0) return null
        return ((elapsed.toFloat() / planned) * 100).toInt().coerceIn(0, 100)
    }
}

data class FocusRoom(
    val activeSessions: List<FocusSession> = emptyList(),
    val todayTotalSeconds: Int = 0
)

/** 秒を 01:24:36 形式にする */
fun formatDuration(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

/** 「1時間48分」のような、読ませるための表記 */
fun formatDurationJa(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val hours = s / 3600
    val minutes = (s % 3600) / 60
    return when {
        hours > 0 -> "${hours}時間${minutes}分"
        minutes > 0 -> "${minutes}分"
        else -> "${s}秒"
    }
}
