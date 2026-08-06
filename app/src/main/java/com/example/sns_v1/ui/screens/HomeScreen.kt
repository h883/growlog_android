package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.sns_v1.model.FocusSession
import com.example.sns_v1.ui.components.FocusAvatarIndicator
import com.example.sns_v1.ui.components.GrowLogTopBar
import com.example.sns_v1.ui.components.PostCard
import com.example.sns_v1.ui.components.UnderlineTabs
import com.example.sns_v1.ui.components.UserAvatar
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.Feed
import com.example.sns_v1.viewmodel.PostsViewModel
import kotlinx.coroutines.delay

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
    val focusing by viewModel.focusing.collectAsState()
    val selectedTab = tabs.indexOfFirst { it.second == feed }.coerceAtLeast(0)

    // 集中状況は動きが速いので、ホームを開いている間だけ定期的に取り直す。
    // 他画面へ移ると NavHost がこの画面を破棄するので、ポーリングも一緒に止まる
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadFocusing()
            delay(30_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // DM はメニューバーから開けるので、ヘッダーにはメールアイコンを置かない
        GrowLogTopBar(
            onSearchClick = onNavigateToDiscover,
            onNotificationsClick = onNavigateToNotifications,
            onMessagesClick = onNavigateToMessages,
            onFocusClick = onNavigateToFocus
        )

        // Discord のオンライン一覧のように、いま集中している人を上に並べる
        if (focusing.isNotEmpty()) {
            FocusingRail(
                sessions = focusing,
                onClick = { onNavigateToFocus() }
            )
        }

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
                        item { FocusInvitationCard(onClick = onNavigateToFocus) }
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

@Composable
private fun FocusInvitationCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        color = Accent,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("FOCUS SESSION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White.copy(alpha = .82f))
            Spacer(Modifier.height(6.dp))
            Text("いまから、ひとつだけ進めよう", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
            Spacer(Modifier.height(6.dp))
            Text("集中した時間が、あなたの成長になります。", fontSize = 13.sp, color = androidx.compose.ui.graphics.Color.White.copy(alpha = .86f))
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = androidx.compose.ui.graphics.Color.White) {
                Text("集中をはじめる", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp))
            }
        }
    }
}

/**
 * 「いま集中中」の横並び。アバターと作業内容だけの最小表示にして、
 * タイムラインの邪魔にならないようにする。
 */
@Composable
private fun FocusingRail(sessions: List<FocusSession>, onClick: () -> Unit) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        Text(
            text = "いま集中中 ${sessions.size}人",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 16.dp, top = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(sessions, key = { it.focusSessionId }) { session ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp).clickable(onClick = onClick)
                ) {
                    Box {
                        UserAvatar(
                            displayName = session.displayName,
                            imageUrl = session.profileImageUrl,
                            size = 44.dp,
                            fontSize = 17.sp
                        )
                        // 何が育っているかを、点の代わりに絵文字で見せる
                        Text(
                            text = session.stageEmoji,
                            fontSize = 15.sp,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.activityTitle,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
    }
}
