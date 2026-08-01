package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.Post
import com.example.sns_v1.model.UserSummary
import com.example.sns_v1.ui.components.AppCard
import com.example.sns_v1.ui.components.GrowLogTopBar
import com.example.sns_v1.ui.components.PostCard
import com.example.sns_v1.ui.components.ProgressRow
import com.example.sns_v1.ui.components.UnderlineTabs
import com.example.sns_v1.ui.components.UserAvatar
import com.example.sns_v1.ui.components.splitPostContent
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.SubBackground
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.DiscoverViewModel

@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onNavigateToPostDetail: (String) -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToGroups: () -> Unit = {}
) {
    val discover by viewModel.discover.collectAsState()
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedResultTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GrowLogTopBar(actions = {})

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "見つける",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
                    )

                    TextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("挑戦やユーザーを検索", color = TextSecondary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "検索をクリア",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = SubBackground,
                            focusedContainerColor = SubBackground,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onNavigateToGroups)
                            .background(SubBackground)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Group,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("グループ", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "同じ目標の人が集まる場所を探す",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            if (query.isBlank()) {
                // カテゴリチップ（人気タグをそのまま絞り込み候補として使う）
                if (discover.trendingTags.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryChip("すべて", selected = true) {}
                            discover.trendingTags.take(6).forEach { tag ->
                                CategoryChip(tag.tag, selected = false) { viewModel.searchTag(tag.tag) }
                            }
                        }
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .height(20.dp)
                        )
                    }
                }

                challengeSection(
                    posts = discover.suggestedPosts,
                    onPostClick = onNavigateToPostDetail
                )

                if (discover.suggestedUsers.isNotEmpty()) {
                    item {
                        SectionHeading("おすすめのユーザー")
                    }
                    items(discover.suggestedUsers, key = { it.userId }) { user ->
                        UserRow(user = user, onClick = { onNavigateToUserProfile(user.userName) })
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    }
                }

                if (discover.trendingTags.isNotEmpty()) {
                    item {
                        SectionHeading("トレンドのタグ")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            discover.trendingTags.take(2).forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SubBackground,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.searchTag(tag.tag) }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            tag.tag,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Accent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("${tag.count} 投稿", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .height(24.dp)
                        )
                    }
                }

                if (discover.trendingTags.isEmpty() && discover.suggestedUsers.isEmpty() &&
                    discover.suggestedPosts.isEmpty()
                ) {
                    item { EmptyResult("まだ表示できるものがありません") }
                }
            } else {
                item {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        UnderlineTabs(
                            tabs = listOf(
                                "投稿 ${results.posts.size}",
                                "ユーザー ${results.users.size}"
                            ),
                            selectedIndex = selectedResultTab,
                            onSelect = { selectedResultTab = it },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = Accent) }
                    }
                } else if (selectedResultTab == 0) {
                    if (results.posts.isEmpty()) {
                        item { EmptyResult("「$query」に一致する投稿は見つかりませんでした") }
                    } else {
                        items(results.posts, key = { it.postId }) { post ->
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
                } else {
                    if (results.users.isEmpty()) {
                        item { EmptyResult("「$query」に一致するユーザーは見つかりませんでした") }
                    } else {
                        items(results.users, key = { it.userId }) { user ->
                            UserRow(user = user, onClick = { onNavigateToUserProfile(user.userName) })
                            HorizontalDivider(color = BorderColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

/** 目標が紐付いた投稿を「今週の挑戦」として、進捗つきで並べる */
private fun LazyListScope.challengeSection(
    posts: List<Post>,
    onPostClick: (String) -> Unit
) {
    if (posts.isEmpty()) return

    item { SectionHeading("今週の挑戦") }
    items(posts, key = { it.postId }) { post ->
        val (title, body) = splitPostContent(post.content)
        AppCard(onClick = { onPostClick(post.postId) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    displayName = post.displayName,
                    imageUrl = post.authorImageUrl,
                    size = 26.dp,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(post.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(" ・ ${post.createdAt}", fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = post.goalTitle ?: title,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val description = body.ifBlank { title }
            if (description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (post.progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                ProgressRow(label = "進捗: ${post.progress}%", progress = post.progress)
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Accent else SubBackground,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else Accent,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
        )
    }
}

@Composable
private fun EmptyResult(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun UserRow(user: UserSummary, onClick: () -> Unit) {
    AppCard(onClick = onClick, padding = 16.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        UserAvatar(
            displayName = user.displayName,
            imageUrl = user.profileImageUrl,
            size = 42.dp,
            fontSize = 17.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("@${user.userName}", fontSize = 12.sp, color = TextSecondary)
            if (user.biography.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    user.biography,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text("${user.followerCount}", fontSize = 12.sp, color = TextSecondary)
    }
    }
}
