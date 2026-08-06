package com.example.sns_v1.ui.screens

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.GardenTheme
import com.example.sns_v1.model.formatDurationJa
import com.example.sns_v1.ui.theme.FocusBasin
import com.example.sns_v1.ui.theme.FocusGreen
import com.example.sns_v1.ui.theme.FocusMuted
import com.example.sns_v1.ui.theme.FocusOnSurface
import com.example.sns_v1.ui.theme.FocusOutline
import com.example.sns_v1.ui.theme.FocusPrimary
import com.example.sns_v1.ui.theme.FocusRest
import com.example.sns_v1.ui.theme.FocusSurface
import com.example.sns_v1.ui.theme.FocusTrack
import com.example.sns_v1.viewmodel.FocusResult

/**
 * 集中セッションの完了画面。
 *
 * 途中でやめた場合もこの画面を出す。育った分をそのまま見せ、
 * 減点や失敗を思わせる表現は使わない。
 */
@Composable
fun FocusResultScreen(
    result: FocusResult,
    isPosting: Boolean = false,
    onDismiss: () -> Unit,
    onPost: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val reachedGoal = result.gardenStage >= GardenTheme.MAX_STAGE

    // 育ったものを、少し遅れて跳ねるように出す
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val artScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.7f,
        animationSpec = tween(durationMillis = 520, easing = EaseOutBack),
        label = "resultArt"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = FocusSurface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 育ったもの
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(artScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(FocusBasin, FocusTrack),
                            radius = 300f
                        )
                    )
                    .border(1.dp, FocusOutline.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (reachedGoal) "SESSION COMPLETE" else "SESSION ENDED",
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp,
                        fontFamily = FontFamily.Monospace,
                        color = FocusMuted.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(result.gardenTheme.emoji(result.gardenStage), fontSize = 84.sp)
                }
            }

            // ねぎらいの言葉。到達段階で言い方を変える
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = if (reachedGoal) "お疲れ様でした！" else "ここまで育ちました",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FocusPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = buildAnnotatedString {
                    append("「")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = FocusOnSurface)) {
                        append(result.gardenTheme.stageName(result.gardenStage))
                    }
                    append("」の段階です。")
                    append(
                        if (reachedGoal) "あなたの庭に新しい実りが加わりました。"
                        else "次回はここから続けられます。"
                    )
                },
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = FocusMuted,
                textAlign = TextAlign.Center
            )

            // 実績。数字は2つだけに絞る
            Spacer(modifier = Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ResultStatCard(
                    value = formatDurationJa(result.actualDurationSeconds),
                    label = "集中した時間",
                    accent = FocusPrimary,
                    modifier = Modifier.weight(1f)
                )
                ResultStatCard(
                    value = "${result.breakCount}回",
                    label = "休憩",
                    accent = FocusRest,
                    modifier = Modifier.weight(1f)
                )
            }

            // 振り返り。下線だけの控えめな入力欄にする
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "今日の成果を書く（任意）",
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
                fontFamily = FontFamily.Monospace,
                color = FocusMuted,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(10.dp))
            ReflectionField(
                value = text,
                onValueChange = { if (it.length <= 300) text = it }
            )

            Spacer(modifier = Modifier.height(32.dp))
            PillButton(
                label = if (text.isBlank()) "記録して投稿する" else "投稿する",
                filled = true,
                enabled = !isPosting,
                loading = isPosting,
                onClick = { onPost(text) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PillButton(
                label = "投稿しない",
                filled = false,
                enabled = !isPosting,
                onClick = onDismiss
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** 数字ひとつと見出しだけのカード */
@Composable
private fun ResultStatCard(
    value: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = FocusSurface,
        border = BorderStroke(1.dp, FocusOutline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp,
                fontFamily = FontFamily.Monospace,
                color = FocusMuted
            )
        }
    }
}

/** 面を塗らず、下線だけで見せる入力欄 */
@Composable
private fun ReflectionField(value: String, onValueChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val underline = if (focused) FocusPrimary else FocusOutline

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
            if (value.isEmpty()) {
                Text(
                    text = "気づいたこと、進んだこと",
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = FocusMuted.copy(alpha = 0.45f)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = FocusOnSurface
                ),
                cursorBrush = SolidColor(FocusPrimary)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (focused) 2.dp else 1.dp)
                .background(underline)
        )
    }
}

/** 角丸のピルボタン。押すと少し縮む */
@Composable
private fun PillButton(
    label: String,
    filled: Boolean,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "pillScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .then(
                if (filled) Modifier.background(FocusPrimary.copy(alpha = if (enabled) 1f else 0.5f))
                else Modifier.border(1.dp, FocusOutline, RoundedCornerShape(28.dp))
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = if (filled) FocusSurface else FocusPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (filled) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = FocusSurface,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                }
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    color = if (filled) FocusSurface else FocusMuted
                )
            }
        }
    }
}
