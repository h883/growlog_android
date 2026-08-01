package com.example.sns_v1.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.Goal
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.Success
import com.example.sns_v1.ui.theme.TextSecondary

private const val PROGRESS_STEP = 10

/**
 * [onProgressChange] / [onAchievedChange] を渡すと操作 UI が出る。
 * 他人の目標を表示するときは省略して読み取り専用にできる。
 */
@Composable
fun GoalCard(
    goal: Goal,
    onProgressChange: ((Int) -> Unit)? = null,
    onAchievedChange: ((Boolean) -> Unit)? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = goal.progress / 100f,
        label = "goalProgress"
    )

    AppCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = goal.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (goal.isAchieved) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = "達成済み",
                        tint = Success,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (goal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = goal.description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Accent,
                trackColor = com.example.sns_v1.ui.theme.AccentSoft
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onProgressChange != null) {
                    IconButton(
                        onClick = { onProgressChange(goal.progress - PROGRESS_STEP) },
                        enabled = goal.progress > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Remove,
                            contentDescription = "進捗を${PROGRESS_STEP}%下げる",
                            modifier = Modifier.size(16.dp),
                            tint = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = { onProgressChange(goal.progress + PROGRESS_STEP) },
                        enabled = goal.progress < 100,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "進捗を${PROGRESS_STEP}%上げる",
                            modifier = Modifier.size(16.dp),
                            tint = Accent
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${goal.progress}%",
                    fontSize = 12.sp,
                    color = Accent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("開始 ${goal.startDate}", fontSize = 12.sp, color = TextSecondary)
                if (goal.endDate != null) {
                    Text("期限 ${goal.endDate}", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("${goal.activityCount}件の活動記録", fontSize = 12.sp, color = TextSecondary)

            if (onAchievedChange != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onAchievedChange(!goal.isAchieved) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Text(
                        if (goal.isAchieved) "達成を取り消す" else "達成にする",
                        fontSize = 13.sp,
                        color = if (goal.isAchieved) TextSecondary else Success
                    )
                }
            }
        }
    }
}
