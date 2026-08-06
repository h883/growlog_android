package com.example.sns_v1.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.FocusSession
import com.example.sns_v1.model.Garden
import com.example.sns_v1.model.GardenTheme
import com.example.sns_v1.model.formatDuration
import com.example.sns_v1.model.formatDurationJa
import com.example.sns_v1.ui.components.UserAvatar
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.AccentSoft
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.FocusBasin
import com.example.sns_v1.ui.theme.FocusMuted
import com.example.sns_v1.ui.theme.FocusOnSurface
import com.example.sns_v1.ui.theme.FocusOutline
import com.example.sns_v1.ui.theme.FocusPrimary
import com.example.sns_v1.ui.theme.FocusPrimarySoft
import com.example.sns_v1.ui.theme.FocusRest
import com.example.sns_v1.ui.theme.FocusSurface
import com.example.sns_v1.ui.theme.FocusTrack
import com.example.sns_v1.ui.theme.SubBackground
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.FocusViewModel

@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    onBack: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val mySession by viewModel.mySession.collectAsState()
    val others by viewModel.others.collectAsState()
    val elapsed by viewModel.elapsed.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val result by viewModel.result.collectAsState()
    val error by viewModel.error.collectAsState()
    val garden by viewModel.garden.collectAsState()
    val lastTheme by viewModel.lastTheme.collectAsState()

    var showStart by remember { mutableStateOf(false) }
    var showEndConfirm by remember { mutableStateOf(false) }

    if (showStart) {
        FocusStartScreen(
            goals = goals,
            defaultTheme = lastTheme,
            onBack = { showStart = false },
            onStart = { title, goalId, minutes, visibility, mode, theme ->
                viewModel.start(title, goalId, minutes, visibility, mode, theme)
                showStart = false
            }
        )
        return
    }

    // 本気モードだけは終了前に確認を挟む（仕様書 7）
    if (showEndConfirm) {
        EndFocusDialog(
            onDismiss = { showEndConfirm = false },
            onFinish = { reason ->
                showEndConfirm = false
                viewModel.finish(completed = reason == null, endReason = reason)
            }
        )
    }

    // 完了はダイアログではなく全画面で受け止める。
    // 一区切りついた実感が要る場面なので、標準ダイアログの見た目は使わない
    result?.let { r ->
        FocusResultScreen(
            result = r,
            onDismiss = { viewModel.dismissResult() },
            onPost = { text -> viewModel.postReflection(r, text) {} }
        )
        return
    }

    mySession?.let { session ->
        FocusActiveScreen(
            session = session,
            elapsed = elapsed,
            onPause = { viewModel.pause() },
            onResume = { viewModel.resume() },
            onFinish = {
                if (session.focusMode.value == "serious") showEndConfirm = true
                else viewModel.finish(completed = true)
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(53.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "戻る")
            }
            Text("集中", fontWeight = FontWeight.Bold, fontSize = 19.sp,
                modifier = Modifier.padding(start = 8.dp))
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        if (error != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.clickable { viewModel.clearError() }
            ) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // 進行中のセッションは FocusActiveScreen が全画面で受け持つので、
            // ここに来るのは未開始のときだけ
            item { IdleCard(isLoading = isLoading, onStart = { showStart = true }) }

            if (garden.plants.isNotEmpty()) {
                item { WeeklyGarden(garden) }
            }

            if (others.isNotEmpty()) {
                item {
                    Text(
                        "集中している人 ${others.size}人",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
                    )
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                }
                items(others, key = { it.focusSessionId }) { session ->
                    OtherFocusRow(
                        session = session,
                        onWater = { viewModel.water(session) },
                        onClick = { onNavigateToUserProfile(session.userName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleCard(isLoading: Boolean, onStart: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Accent)
        } else {
            Text("いま何に取り組みますか", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "集中を始めると、終わるまで通知が届きません",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = com.example.sns_v1.ui.theme.Warning),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("集中をはじめる", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    }
}

/**
 * 集中タイマーの本体。長く見つめる画面なので、アプリ本体の配色とは分けて
 * 紙色の面に置き、文字盤・リング・丸ボタンの3要素だけに絞る。
 */
@Composable
fun FocusTimerFace(
    session: FocusSession,
    elapsed: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit
) {
    val accent = if (session.isPaused) FocusRest else FocusPrimary

    // リングは段階の進み具合を表す。予定時間があれば連続的に、無ければ段階刻みで進める
    val target = session.plannedDurationSeconds
        ?.takeIf { it > 0 }
        ?.let { (elapsed.toFloat() / it).coerceIn(0f, 1f) }
        ?: (session.gardenStage.toFloat() / GardenTheme.MAX_STAGE)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(800),
        label = "focusProgress"
    )

    // 生きている感じを出すための、ごくゆるい呼吸。休憩中は止める
    val breathing = if (session.isPaused) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "breathe")
        val scale by transition.animateFloat(
            initialValue = 0.985f,
            targetValue = 1.015f,
            animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
            label = "breatheScale"
        )
        scale
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = FocusSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ステータスピル
            Surface(
                shape = RoundedCornerShape(50),
                color = FocusPrimarySoft.copy(alpha = if (session.isPaused) 0.4f else 0.6f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (session.isPaused) "ON A BREAK" else "NOW FOCUSING",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.0.sp,
                        fontFamily = FontFamily.Monospace,
                        color = accent
                    )
                }
            }

            // 作業内容。明朝で editorial な見え方にする
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = session.activityTitle,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
                color = FocusOnSurface,
                textAlign = TextAlign.Center
            )
            if (session.goalTitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = session.goalTitle,
                    fontSize = 13.sp,
                    color = FocusMuted,
                    textAlign = TextAlign.Center
                )
            }

            // 文字盤
            Spacer(modifier = Modifier.height(44.dp))
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val inset = stroke / 2
                    drawArc(
                        color = FocusTrack,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    if (progress > 0f) {
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }

                // 土の窪みのような面。中央をわずかに沈めて立体感を出す
                Box(
                    modifier = Modifier
                        .size(244.dp)
                        .scale(breathing)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(FocusBasin, FocusTrack),
                                radius = 340f
                            )
                        )
                        .border(1.dp, FocusOutline.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(session.stageEmoji, fontSize = 34.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatDuration(elapsed),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-2).sp,
                            color = accent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = session.stageName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = FocusMuted
                        )
                    }
                }
            }

            // 次の段階までの案内。パーセントは出さない
            if (session.secondsToNextStage != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "次は「${session.gardenTheme.stageName(session.gardenStage + 1)}」まで あと${formatDurationJa(session.secondsToNextStage)}",
                    fontSize = 13.sp,
                    color = FocusMuted,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = buildString {
                    append("邪魔せず応援 ${session.cheerCount}")
                    if (session.breakCount > 0) append(" ・ 休憩${session.breakCount}回")
                },
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
                fontFamily = FontFamily.Monospace,
                color = FocusMuted.copy(alpha = 0.7f)
            )

            // 操作は丸ボタン2つだけ。終了は控えめ、休憩・再開を主にする
            Spacer(modifier = Modifier.height(36.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleAction(
                    icon = Icons.Outlined.StopCircle,
                    label = "終了",
                    size = 78.dp,
                    contentColor = FocusMuted,
                    onClick = onFinish
                )
                CircleAction(
                    icon = if (session.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    label = if (session.isPaused) "再開" else "休憩",
                    size = 94.dp,
                    contentColor = accent,
                    onClick = if (session.isPaused) onResume else onPause
                )
            }
        }
    }
}

/** アイコンと小さなラベルだけの丸ボタン。押すと少し縮む */
@Composable
internal fun CircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    size: Dp,
    contentColor: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        label = "actionScale"
    )

    Column(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(FocusSurface)
            .border(1.dp, FocusOutline, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(size * 0.3f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
            fontFamily = FontFamily.Monospace,
            color = contentColor
        )
    }
}

/**
 * 今週の庭。1回の集中で育てたものを、そのまま並べる。
 * 途中でやめたものも消さない。
 */
@Composable
private fun WeeklyGarden(garden: Garden) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("今週の庭", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            garden.plants.forEach { plant ->
                Text(plant.emoji, fontSize = 30.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "集中セッション：${garden.sessionCount}回",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            "育てた時間：${formatDurationJa(garden.totalSeconds)}",
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
    HorizontalDivider(color = BorderColor, thickness = 1.dp)
}

/** 段階を6つのマスで表す。到達済みは塗り、未到達は薄いまま */
@Composable
private fun StageTrack(stage: Int, animated: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0..GardenTheme.MAX_STAGE) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i <= stage) Accent else AccentSoft)
            )
        }
    }
}

/**
 * 他の人の集中の見せ方。数字で競わせず、いま何が育っているかを伝える。
 * 送れるのは水やりだけで、相手には終了後にまとめて届く。
 */
@Composable
private fun OtherFocusRow(
    session: FocusSession,
    onWater: () -> Unit,
    onClick: () -> Unit
) {
    val watered = session.myReactions.contains("water")
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    displayName = session.displayName,
                    imageUrl = session.profileImageUrl,
                    size = 36.dp,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "${session.displayName}さんは${if (session.isPaused) "休憩中" else "集中中"}です",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(session.stageEmoji, fontSize = 44.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(session.activityTitle, fontSize = 14.sp, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Text(
                "作業開始から ${formatDurationJa(session.elapsedSeconds)}",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Text(
                "状態：${session.gardenTheme.stageSentence(session.gardenStage)}",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (watered) Accent else SubBackground,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onWater)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (watered) "水をあげました 💧 ${session.cheerCount}"
                        else "そっと水をあげる 💧",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (watered) MaterialTheme.colorScheme.background else Accent
                    )
                }
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
    }
}
