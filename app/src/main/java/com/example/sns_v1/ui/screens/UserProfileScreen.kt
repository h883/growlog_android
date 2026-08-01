package com.example.sns_v1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.ui.components.PostCard
import com.example.sns_v1.ui.components.UserAvatar
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.PrimaryButton
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.UserProfileViewModel

@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel,
    onBack: () -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {},
    onOpenChat: (String) -> Unit = {}
) {
    val profile by viewModel.profile.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "戻る")
                }
            }
        }

        if (isLoading || profile == null) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }
        } else {
            val p = profile!!
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(
                        displayName = p.displayName,
                        imageUrl = p.profileImageUrl,
                        size = 80.dp,
                        fontSize = 32.sp,
                        backgroundColor = Accent.copy(alpha = 0.15f),
                        contentColor = Accent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(p.displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("@${p.userName}", fontSize = 14.sp, color = TextSecondary)
                    if (p.biography.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(p.biography, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${p.followingCount}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("フォロー", fontSize = 12.sp, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${p.followerCount}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("フォロワー", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // X のフォローボタンは黒いピル。フォロー中は枠線だけになる
                        Button(
                            onClick = { viewModel.toggleFollow() },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (p.isFollowing) MaterialTheme.colorScheme.background
                                                 else PrimaryButton,
                                contentColor = if (p.isFollowing) MaterialTheme.colorScheme.onBackground
                                               else MaterialTheme.colorScheme.background
                            ),
                            border = if (p.isFollowing)
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            else null,
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text(
                                if (p.isFollowing) "フォロー中" else "フォロー",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.openConversation(onOpenChat) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text(
                                "メッセージ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        listOf("投稿" to "${p.postCount}", "目標" to "${p.goalCount}").forEach { (label, value) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Accent)
                                Text(label, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
            }

            if (posts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("投稿がまだありません", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                items(posts, key = { it.postId }) { post ->
                    PostCard(
                        post = post,
                        onReaction = { viewModel.toggleReaction(post.postId) },
                        onCommentClick = { onNavigateToPostDetail(post.postId) },
                        onSave = { viewModel.toggleSave(post.postId) },
                        onPostClick = { onNavigateToPostDetail(post.postId) }
                    )
                }
            }
        }
    }
}
