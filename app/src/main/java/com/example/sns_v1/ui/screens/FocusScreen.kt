package com.example.sns_v1.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.FocusSession
import com.example.sns_v1.model.formatDuration
import com.example.sns_v1.model.formatDurationJa
import com.example.sns_v1.ui.components.UserAvatar
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.AccentSoft
import com.example.sns_v1.ui.theme.BorderColor
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

    var showStart by remember { mutableStateOf(false) }
    var showEndConfirm by remember { mutableStateOf(false) }

    if (showStart) {
        StartFocusDialog(
            goals = goals,
            onDismiss = { showStart = false },
            onStart = { title, goalId, minutes, visibility, mode ->
                viewModel.start(title, goalId, minutes, visibility, mode)
                showStart = false
            }
        )
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

    result?.let { r ->
        FocusResultDialog(
            result = r,
            onDismiss = { viewModel.dismissResult() },
            onPost = { text -> viewModel.postReflection(r, text) {} }
        )
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
            item {
                val session = mySession
                if (session == null) {
                    IdleCard(isLoading = isLoading, onStart = { showStart = true })
                } else {
                    MyFocusCard(
                        session = session,
                        elapsed = elapsed,
                        onPause = { viewModel.pause() },
                        onResume = { viewModel.resume() },
                        onFinish = {
                            if (session.focusMode.value == "serious") showEndConfirm = true
                            else viewModel.finish(completed = true)
                        }
                    )
                }
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
                        onCheer = { viewModel.cheer(session) },
                        onClick = { onNavigateToUserProfile(session.userName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleCard(isLoading: Boolean, onStart: () -> Unit) {
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
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("集中をはじめる", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MyFocusCard(
    session: FocusSession,
    elapsed: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit
) {
    val percent = session.progressPercent(elapsed)
    val animated by animateFloatAsState(
        targetValue = (percent ?: 0) / 100f,
        label = "focusProgress"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            if (session.isPaused) "休憩中" else "集中しています",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Accent
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(session.activityTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
        if (session.goalTitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("目標: ${session.goalTitle}", fontSize = 13.sp, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(formatDuration(elapsed), fontSize = 40.sp, fontWeight = FontWeight.Bold)
        if (session.plannedDurationSeconds != null) {
            Text(
                "目標時間 ${formatDuration(session.plannedDurationSeconds)}",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        if (percent != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentSoft)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animated)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Accent)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("$percent%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Accent)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null,
                tint = Accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("邪魔せず応援 ${session.cheerCount}", fontSize = 14.sp, color = TextSecondary)
            if (session.breakCount > 0) {
                Text(" ・ 休憩${session.breakCount}回", fontSize = 14.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = if (session.isPaused) onResume else onPause,
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text(
                    if (session.isPaused) "再開する" else "休憩する",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text("終了する", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    HorizontalDivider(color = BorderColor, thickness = 1.dp)
}

@Composable
private fun OtherFocusRow(
    session: FocusSession,
    onCheer: () -> Unit,
    onClick: () -> Unit
) {
    val cheered = session.myReactions.contains("cheer")
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                displayName = session.displayName,
                imageUrl = session.profileImageUrl,
                size = 40.dp,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (session.isPaused) " ・ 休憩中" else " ・ 集中中",
                        fontSize = 13.sp,
                        color = if (session.isPaused) TextSecondary else Accent
                    )
                }
                Text(session.activityTitle, fontSize = 14.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(formatDurationJa(session.elapsedSeconds), fontSize = 13.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            // 集中中に送れるのは応援だけ（仕様書 6）
            Surface(
                shape = RoundedCornerShape(50),
                color = if (cheered) Accent else SubBackground,
                modifier = Modifier.clickable(onClick = onCheer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = "邪魔せず応援する",
                        modifier = Modifier.size(15.dp),
                        tint = if (cheered) MaterialTheme.colorScheme.background else Accent
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${session.cheerCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cheered) MaterialTheme.colorScheme.background else Accent
                    )
                }
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
    }
}
