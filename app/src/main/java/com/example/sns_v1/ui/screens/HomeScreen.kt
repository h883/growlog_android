package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.ui.components.GrowLogTopBar
import com.example.sns_v1.ui.components.PostCard
import com.example.sns_v1.ui.components.UnderlineTabs
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.Feed
import com.example.sns_v1.viewmodel.PostsViewModel

@Composable
fun HomeScreen(
    viewModel: PostsViewModel,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {}
) {
    val tabs = listOf("おすすめ" to Feed.POPULAR, "フォロー中" to Feed.FOLLOWING, "最新" to Feed.LATEST)

    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val feed by viewModel.feed.collectAsState()
    val selectedTab = tabs.indexOfFirst { it.second == feed }.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // DM はメニューバーから開けるので、ヘッダーにはメールアイコンを置かない
        GrowLogTopBar(
            onSearchClick = onNavigateToDiscover,
            onNotificationsClick = onNavigateToNotifications,
            onFocusClick = onNavigateToFocus
        )

        Surface(color = MaterialTheme.colorScheme.surface) {
            UnderlineTabs(
                tabs = tabs.map { it.first },
                selectedIndex = selectedTab,
                onSelect = { viewModel.selectFeed(tabs[it].second) }
            )
        }

        when {
            isLoading && posts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }
            error != null && posts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("読み込みに失敗しました", color = TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadPosts() },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) {
                            Text("再試行")
                        }
                    }
                }
            }
            posts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = when (feed) {
                            Feed.FOLLOWING -> "フォロー中のユーザーの投稿はまだありません"
                            Feed.POPULAR -> "この1週間の投稿はまだありません"
                            Feed.LATEST -> "投稿がまだありません"
                        },
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.loadPosts() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                    ) {
                        items(posts, key = { it.postId }) { post ->
                            PostCard(
                                post = post,
                                onReaction = { viewModel.toggleReaction(post.postId) },
                                onCommentClick = { onNavigateToPostDetail(post.postId) },
                                onAuthorClick = { onNavigateToUserProfile(post.userName) },
                                onSave = { viewModel.toggleSave(post.postId) },
                                onPostClick = { onNavigateToPostDetail(post.postId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
