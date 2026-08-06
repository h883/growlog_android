package com.example.sns_v1.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.FocusPresence
import com.example.sns_v1.model.formatDurationJa
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.AccentSoft
import com.example.sns_v1.ui.theme.TextSecondary

/**
 * 集中中を表す点。集中中はゆっくり明滅させ、休憩中は止めて色も落とす。
 * Discord のオンライン表示と同じ役割。
 */
@Composable
fun FocusDot(presence: FocusPresence, size: Dp = 8.dp) {
    val color = if (presence.isPaused) TextSecondary else Accent
    val alpha = if (presence.isPaused) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "focusDot")
        val animated by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
            label = "focusDotAlpha"
        )
        animated
    }

    Box(
        modifier = Modifier
            .size(size)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * アバターの右下に重ねる集中インジケータ。
 * 背景色で縁取りして、アバターの絵柄に埋もれないようにする。
 */
@Composable
fun BoxScope.FocusAvatarIndicator(presence: FocusPresence?, size: Dp = 13.dp) {
    if (presence == null) return
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        FocusDot(presence, size = size - 4.dp)
    }
}

/**
 * 名前の横に並べる小さなバッジ。「集中中」だけの最小表示。
 * タイムラインのように行が詰まっている場所で使う。
 */
@Composable
fun FocusMiniBadge(presence: FocusPresence?, modifier: Modifier = Modifier) {
    if (presence == null) return
    Surface(
        shape = RoundedCornerShape(50),
        color = if (presence.isPaused) MaterialTheme.colorScheme.surfaceVariant else AccentSoft,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FocusDot(presence, size = 6.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${presence.stageEmoji} ${presence.label}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (presence.isPaused) TextSecondary else Accent
            )
        }
    }
}

/**
 * 作業内容と経過時間まで出す1行。プロフィールなど、縦に余裕がある場所で使う。
 */
@Composable
fun FocusStatusLine(
    presence: FocusPresence?,
    /** 「樹さんは〜を育てています」の主語。自分の画面では省く */
    displayName: String? = null,
    modifier: Modifier = Modifier
) {
    if (presence == null) return
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (presence.isPaused) MaterialTheme.colorScheme.surfaceVariant else AccentSoft,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(presence.stageEmoji, fontSize = 30.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // パーセントではなく、何を育てているかで伝える
                Text(
                    text = if (displayName != null) {
                        "${displayName}さんは「${presence.activityTitle}」を育てています"
                    } else {
                        "「${presence.activityTitle}」を育てています"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "作業開始から ${formatDurationJa(presence.elapsedSeconds)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = if (presence.isPaused) {
                        "休憩中・${presence.stageSentence}"
                    } else {
                        "今は「${presence.gardenTheme.stageName(presence.gardenStage)}の段階」"
                    },
                    fontSize = 12.sp,
                    color = if (presence.isPaused) TextSecondary else Accent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
