package com.example.sns_v1.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** minSdk 24 なので java.time は使わず SimpleDateFormat で揃える。
 *  SimpleDateFormat はスレッドセーフでないため、使うたびに作る */
private fun isoDateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun today(): String = isoDateFormat().format(Date())

/** "2026-07-29" 形式かどうか。空文字は呼び出し側で「未設定」として扱う */
fun isIsoDate(text: String): Boolean =
    Regex("""^\d{4}-\d{2}-\d{2}$""").matches(text) &&
        runCatching { isoDateFormat().apply { isLenient = false }.parse(text) }.isSuccess

fun formatRelativeTime(isoTime: String): String {
    if (isoTime.isBlank()) return ""
    return try {
        val date = try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).also {
                it.timeZone = TimeZone.getTimeZone("UTC")
            }.parse(isoTime)
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).also {
                it.timeZone = TimeZone.getTimeZone("UTC")
            }.parse(isoTime)
        } ?: return isoTime

        val diff = System.currentTimeMillis() - date.time
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000

        when {
            minutes < 1 -> "たった今"
            minutes < 60 -> "${minutes}分前"
            hours < 24 -> "${hours}時間前"
            days < 7 -> "${days}日前"
            else -> SimpleDateFormat("M月d日", Locale.JAPAN).format(date)
        }
    } catch (_: Exception) {
        isoTime
    }
}
