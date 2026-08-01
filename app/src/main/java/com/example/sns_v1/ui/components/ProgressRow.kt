package com.example.sns_v1.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.AccentSoft
import com.example.sns_v1.ui.theme.AccentSoftDark
import com.example.sns_v1.ui.theme.TextSecondary

/**
 * 「ラベル ……… 42%」＋細いバー、という進捗表示。
 * 投稿カード・目標カード・発見画面で共通して使う。
 */
@Composable
fun ProgressRow(
    label: String,
    progress: Int,
    modifier: Modifier = Modifier,
    trailingText: String? = null
) {
    val animated by animateFloatAsState(
        targetValue = (progress.coerceIn(0, 100)) / 100f,
        label = "progress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = trailingText ?: "$progress%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (trailingText == null) Accent else TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isSystemInDarkTheme()) AccentSoftDark else AccentSoft)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Accent)
            )
        }
    }
}
