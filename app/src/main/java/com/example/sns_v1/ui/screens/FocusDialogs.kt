package com.example.sns_v1.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.sns_v1.model.FocusMode
import com.example.sns_v1.model.FocusVisibility
import com.example.sns_v1.model.GardenTheme
import com.example.sns_v1.model.Goal
import com.example.sns_v1.model.formatDurationJa
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.SubBackground
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.FocusResult

private val PRESET_MINUTES = listOf(25, 45, 60, 90, 120)

@Composable
fun StartFocusDialog(
    goals: List<Goal>,
    defaultTheme: GardenTheme = GardenTheme.PLANT,
    onDismiss: () -> Unit,
    onStart: (
        title: String,
        goalId: String?,
        minutes: Int?,
        visibility: FocusVisibility,
        mode: FocusMode,
        theme: GardenTheme
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var goalId by remember { mutableStateOf<String?>(null) }
    var minutes by remember { mutableStateOf<Int?>(60) }
    var visibility by remember { mutableStateOf(FocusVisibility.FOLLOWERS) }
    var mode by remember { mutableStateOf(FocusMode.LIGHT) }
    var theme by remember { mutableStateOf(defaultTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        // AlertDialog は既定では IME に合わせて縮まないため、キーボードを出すと
        // 下端のボタン行が画面外へ押し出されて「開始」が押せなくなる。
        // decorFitsSystemWindows = false + imePadding でキーボードの上に収める。
        properties = DialogProperties(decorFitsSystemWindows = false),
        modifier = Modifier.imePadding(),
        title = { Text("集中をはじめる", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            // 中身が長いので、スクロール領域の高さも上限を決めておく
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("作業内容") },
                    placeholder = { Text("SNSアプリのグループ画面を設計", color = TextSecondary) },
                    supportingText = {
                        if (title.isBlank()) {
                            Text("作業内容を入力すると開始できます", fontSize = 12.sp, color = TextSecondary)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderColor, focusedBorderColor = Accent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("予定時間", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_MINUTES.forEach { m ->
                        Chip("${m}分", minutes == m) { minutes = m }
                    }
                    Chip("なし", minutes == null) { minutes = null }
                }

                if (goals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("関連する目標", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip("なし", goalId == null) { goalId = null }
                        goals.forEach { goal ->
                            Chip(goal.title, goalId == goal.goalId) { goalId = goal.goalId }
                        }
                    }
                }

                // 何を育てるかを選ばせる。全部が植物だと飽きるので複数用意している
                Spacer(modifier = Modifier.height(16.dp))
                Text("育てるもの", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GardenTheme.entries.forEach { t ->
                        Chip("${t.emoji(GardenTheme.MAX_STAGE)} ${t.label}", theme == t) { theme = t }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${theme.emoji(0)} ${theme.stageName(0)} → ${theme.emoji(GardenTheme.MAX_STAGE)} ${theme.stageName(GardenTheme.MAX_STAGE)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("公開範囲", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocusVisibility.entries.forEach { v ->
                        Chip(v.label, visibility == v) { visibility = v }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("集中モード", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                FocusMode.entries.forEach { m ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mode = m }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = mode == m,
                            onClick = { mode = m },
                            colors = RadioButtonDefaults.colors(selectedColor = Accent)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(m.description, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onStart(title.trim(), goalId, minutes, visibility, mode, theme) },
                enabled = title.isNotBlank()
            ) {
                Text(
                    "開始",
                    color = if (title.isNotBlank()) Accent else TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル", color = TextSecondary) }
        }
    )
}

/** 本気モードの終了確認。理由を選ばせ、緊急終了は常に残す（仕様書 7・10） */
@Composable
fun EndFocusDialog(
    onDismiss: () -> Unit,
    onFinish: (endReason: String?) -> Unit
) {
    val reasons = listOf("作業が終わった", "休憩したい", "予定が入った", "集中できない")
    var selected by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("集中を終了しますか", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("なぜ終了しますか", fontSize = 14.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = reason }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = selected == reason,
                            onClick = { selected = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = Accent)
                        )
                        Text(reason, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "理由を選ばなくても終了できます。",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onFinish(selected) }) {
                Text("終了する", color = Accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("続ける", color = TextSecondary) }
        }
    )
}

/** 終了後の振り返り。投稿するかどうかは任意（仕様書 8） */
@Composable
fun FocusResultDialog(
    result: FocusResult,
    onDismiss: () -> Unit,
    onPost: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        // こちらも入力欄があるので、キーボードでボタンが隠れないようにする
        properties = DialogProperties(decorFitsSystemWindows = false),
        modifier = Modifier.imePadding(),
        title = { Text("今日はここまで育ちました", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 途中でやめても責めない。育った分をそのまま見せる
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(result.gardenTheme.emoji(result.gardenStage), fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "「${result.gardenTheme.stageName(result.gardenStage)}」まで育ちました",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (result.gardenStage < GardenTheme.MAX_STAGE) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "次回はここから続けられます",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                Surface(shape = RoundedCornerShape(12.dp), color = SubBackground) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ResultRow("作業内容", result.activityTitle)
                        ResultRow("集中時間", formatDurationJa(result.actualDurationSeconds))
                        ResultRow("休憩回数", "${result.breakCount}回")
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 300) text = it },
                    label = { Text("今日の成果を書く") },
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderColor, focusedBorderColor = Accent
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPost(text) }) {
                Text("投稿する", color = Accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("投稿しない", color = TextSecondary) }
        }
    )
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.width(72.dp))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Accent else SubBackground,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.background else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}
