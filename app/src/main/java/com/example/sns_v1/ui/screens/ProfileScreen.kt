package com.example.sns_v1.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.Goal
import com.example.sns_v1.ui.components.AppCard
import com.example.sns_v1.ui.components.ProgressRow
import com.example.sns_v1.ui.components.CreateGoalDialog
import com.example.sns_v1.ui.components.GoalCard
import com.example.sns_v1.ui.components.GrowLogTopBar
import com.example.sns_v1.ui.components.PostCard
import com.example.sns_v1.ui.components.TopBarIcon
import com.example.sns_v1.ui.components.UnderlineTabs
import com.example.sns_v1.ui.components.UserAvatar
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.SubBackground
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.utils.compressImageForUpload
import com.example.sns_v1.viewmodel.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToPostDetail: (String) -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDiscover: () -> Unit = {}
) {
    val tabs = listOf("投稿", "目標", "保存", "達成記録")
    var selectedTab by remember { mutableIntStateOf(0) }

    val profile by viewModel.profile.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val savedPosts by viewModel.savedPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val activeGoals = goals.filterNot { it.isAchieved }
    val achievedGoals = goals.filter { it.isAchieved }
    var showCreateGoal by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (showCreateGoal) {
        CreateGoalDialog(
            onDismiss = { showCreateGoal = false },
            onCreate = { title, description, startDate, endDate ->
                viewModel.createGoal(title, description, startDate, endDate)
            }
        )
    }

    val pickAvatar = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    compressImageForUpload(context, uri, maxDimen = 512)
                }
                if (bytes != null) viewModel.updateAvatar(bytes)
                else viewModel.showError("画像を読み込めませんでした")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GrowLogTopBar(actions = {
            TopBarIcon(Icons.Outlined.Search, "検索", onNavigateToDiscover)
            TopBarIcon(Icons.Outlined.Settings, "設定", onNavigateToSettings)
        })

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            UserAvatar(
                                displayName = profile.displayName,
                                imageUrl = profile.profileImageUrl,
                                size = 68.dp,
                                fontSize = 26.sp,
                                backgroundColor = SubBackground,
                                contentColor = Accent,
                                modifier = Modifier.clickable(enabled = !isUploadingAvatar) {
                                    pickAvatar.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                            Surface(
                                shape = CircleShape,
                                color = Accent,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isUploadingAvatar) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Outlined.PhotoCamera,
                                            contentDescription = "プロフィール画像を変更",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                profile.displayName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text("@${profile.userName}", fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row {
                                CountLabel("${profile.followingCount}", "フォロー")
                                Spacer(modifier = Modifier.width(16.dp))
                                CountLabel("${profile.followerCount}", "フォロワー")
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { viewModel.clearError() }
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    if (profile.biography.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            profile.biography,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        StatCard("挑戦中", "${activeGoals.size}")
                        StatCard("達成", "${achievedGoals.size}")
                        StatCard("投稿", "${posts.size}")
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onNavigateToEditProfile,
                        shape = RoundedCornerShape(50),
                        border = ButtonDefaults.outlinedButtonBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.outline
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text(
                            "プロフィールを編集",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (activeGoals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        ActiveGoalsProgress(
                            goals = activeGoals,
                            onSeeAll = { selectedTab = 1 }
                        )
                    }
                }

                Surface(color = MaterialTheme.colorScheme.surface) {
                    UnderlineTabs(
                        tabs = tabs,
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Accent)
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> postList(posts, "投稿がまだありません", viewModel, onNavigateToPostDetail, onNavigateToUserProfile)
                    1 -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Button(
                                    onClick = { showCreateGoal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Text("新しい目標を追加", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        if (activeGoals.isEmpty()) {
                            item { EmptyMessage("挑戦中の目標はありません") }
                        } else {
                            items(activeGoals, key = { it.goalId }) { goal ->
                                GoalCard(
                                    goal = goal,
                                    onProgressChange = { viewModel.updateGoalProgress(goal.goalId, it) },
                                    onAchievedChange = { viewModel.setGoalAchieved(goal.goalId, it) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                    2 -> postList(savedPosts, "保存した投稿はまだありません", viewModel, onNavigateToPostDetail, onNavigateToUserProfile)
                    3 -> {
                        if (achievedGoals.isEmpty()) {
                            item { EmptyMessage("達成記録はまだありません") }
                        } else {
                            items(achievedGoals, key = { it.goalId }) { goal ->
                                GoalCard(
                                    goal = goal,
                                    onAchievedChange = { viewModel.setGoalAchieved(goal.goalId, it) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.postList(
    posts: List<com.example.sns_v1.model.Post>,
    emptyText: String,
    viewModel: ProfileViewModel,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit
) {
    if (posts.isEmpty()) {
        item { EmptyMessage(emptyText) }
    } else {
        items(posts, key = { it.postId }) { post ->
            PostCard(
                post = post,
                onCommentClick = { onNavigateToPostDetail(post.postId) },
                onAuthorClick = { onNavigateToUserProfile(post.userName) },
                onSave = { viewModel.toggleSave(post.postId) },
                onPostClick = { onNavigateToPostDetail(post.postId) }
            )
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun CountLabel(value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

private const val GOALS_SHOWN_IN_HEADER = 3

/**
 * 挑戦中の目標の進み具合をプロフィール上部に出す。
 * 全体の平均を大きく見せたうえで、内訳を数件だけ並べる。
 */
@Composable
private fun ActiveGoalsProgress(
    goals: List<Goal>,
    onSeeAll: () -> Unit
) {
    // 目標ごとの進捗を単純平均したものを「全体」として扱う
    val overall = remember(goals) {
        if (goals.isEmpty()) 0 else goals.sumOf { it.progress } / goals.size
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "挑戦中の進捗",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$overall%",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Accent
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (goals.size == 1) "目標1件の進み具合" else "目標${goals.size}件の平均",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))
        goals.take(GOALS_SHOWN_IN_HEADER).forEachIndexed { index, goal ->
            if (index > 0) Spacer(modifier = Modifier.height(12.dp))
            ProgressRow(label = goal.title, progress = goal.progress)
        }

        if (goals.size > GOALS_SHOWN_IN_HEADER) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "他${goals.size - GOALS_SHOWN_IN_HEADER}件を見る",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onSeeAll)
                    .padding(vertical = 4.dp)
            )
        }
    }
}

/** X のプロフィールに合わせ、枠を持たない「数字＋ラベル」の並びにする */
@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 14.sp, color = TextSecondary)
    }
}
