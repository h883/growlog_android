package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.FocusMode
import com.example.sns_v1.model.FocusSession
import com.example.sns_v1.model.FocusVisibility
import com.example.sns_v1.model.GardenTheme
import com.example.sns_v1.model.Goal
import com.example.sns_v1.model.formatDurationJa
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.AccentSoft
import com.example.sns_v1.ui.theme.Background
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.ui.theme.Warning

@Composable
fun FocusStartScreen(
    goals: List<Goal>,
    defaultTheme: GardenTheme,
    onBack: () -> Unit,
    onStart: (String, String?, Int?, FocusVisibility, FocusMode, GardenTheme) -> Unit
) {
    val context = LocalContext.current
    val useDefaultTimer = remember { context.getSharedPreferences(SettingsPreferences.FILE, android.content.Context.MODE_PRIVATE).getBoolean(SettingsPreferences.DEFAULT_TIMER, true) }
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf<Int?>(if (useDefaultTimer) 25 else 60) }
    var goalId by remember { mutableStateOf<String?>(goals.firstOrNull()?.goalId) }
    var visibility by remember { mutableStateOf(FocusVisibility.FOLLOWERS) }
    var mode by remember { mutableStateOf(FocusMode.FOCUS) }
    var theme by remember { mutableStateOf(defaultTheme) }

    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(62.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "戻る") }
            Text("集中を始める", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.size(48.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            FocusLabel(Icons.Outlined.Eco, "何に取り組みますか？")
            OutlinedTextField(
                value = title, onValueChange = { title = it.take(60) },
                placeholder = { Text("SNSアプリのホーム画面を設計する", color = TextSecondary.copy(.55f)) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = BorderColor)
            )
            FocusLabel(Icons.Outlined.Timer, "予定時間")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(25, 45, 60, 90, 120).forEach { value -> FocusPill("${value}分", minutes == value) { minutes = value } }
                FocusPill("指定なし", minutes == null) { minutes = null }
            }
            FocusLabel(Icons.Outlined.Eco, "関連する目標")
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                goals.take(4).forEach { goal ->
                    val selected = goalId == goal.goalId
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { goalId = goal.goalId },
                        color = if (selected) AccentSoft else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(15.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Accent else BorderColor)
                    ) { Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(goal.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        RadioButton(selected, { goalId = goal.goalId }, colors = RadioButtonDefaults.colors(selectedColor = Accent))
                    } }
                }
                if (goals.isEmpty()) Text("目標を選ばずに開始できます", fontSize = 13.sp, color = TextSecondary)
            }
            FocusLabel(Icons.Outlined.Public, "公開範囲")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FocusPill("自分のみ", visibility == FocusVisibility.PRIVATE, Modifier.weight(1f)) { visibility = FocusVisibility.PRIVATE }
                FocusPill("フォロワーのみ", visibility == FocusVisibility.FOLLOWERS, Modifier.weight(1f)) { visibility = FocusVisibility.FOLLOWERS }
            }
            FocusLabel(Icons.Outlined.Eco, "集中モード")
            FocusMode.entries.forEach { candidate ->
                val selected = mode == candidate
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { mode = candidate }, shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Accent else BorderColor)
                ) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(42.dp), CircleShape, color = if (selected) Accent.copy(.16f) else AccentSoft) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Eco, null, tint = Accent) } }
                    Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(candidate.label, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(candidate.description, fontSize = 12.sp, color = TextSecondary) }
                    RadioButton(selected, { mode = candidate }, colors = RadioButtonDefaults.colors(selectedColor = Accent))
                } }
            }
            FocusLabel(Icons.Outlined.Eco, "育てるテーマ")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GardenTheme.entries.forEach { candidate ->
                    val selected = theme == candidate
                    Surface(
                        modifier = Modifier.width(132.dp).height(126.dp).clickable { theme = candidate },
                        color = if (selected) AccentSoft else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Accent else BorderColor)
                    ) { Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(candidate.emoji(GardenTheme.MAX_STAGE), fontSize = 42.sp); Spacer(Modifier.height(5.dp)); Text(candidate.label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    } }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Button(
            onClick = { onStart(title.trim().ifBlank { "集中セッション" }, goalId, minutes, visibility, mode, theme) },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp).height(58.dp),
            shape = RoundedCornerShape(30.dp), colors = ButtonDefaults.buttonColors(containerColor = Warning)
        ) { Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("集中を始める", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
    }
}

/**
 * 集中中の全画面。見た目は FocusTimerFace が持つ。
 * この画面は下部メニューを出さず、タイマーだけを残す。
 */
@Composable
fun FocusActiveScreen(session: FocusSession, elapsed: Int, onPause: () -> Unit, onResume: () -> Unit, onFinish: () -> Unit) {
    FocusTimerFace(
        session = session,
        elapsed = elapsed,
        onPause = onPause,
        onResume = onResume,
        onFinish = onFinish
    )
}

@Composable private fun FocusLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Accent, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp) } }
@Composable private fun FocusPill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) { Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), color = if (selected) AccentSoft else MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Accent else BorderColor)) { Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Accent else MaterialTheme.colorScheme.onBackground, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp)) } }
private fun formatClock(seconds: Int): String = "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
