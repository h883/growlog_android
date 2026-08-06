package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sns_v1.AppState
import com.example.sns_v1.model.Comment
import com.example.sns_v1.model.Post
import com.example.sns_v1.ui.components.FocusAvatarIndicator
import com.example.sns_v1.ui.components.FocusMiniBadge
import com.example.sns_v1.ui.components.ProgressRow
import com.example.sns_v1.ui.components.UserAvatar
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.LikeColor
import com.example.sns_v1.ui.theme.SubBackground
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.PostDetailViewModel

@Composable
fun PostDetailScreen(
    viewModel: PostDetailViewModel,
    onBack: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val post by viewModel.post.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()
    var inputText by remember { mutableStateOf("") }

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
            Text(
                text = "ポスト",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        if (error != null && post == null) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("投稿を読み込めませんでした", color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.load() },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) { Text("再試行") }
            }
        } else if (isLoading && post == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                post?.let { p ->
                    item {
                        PostDetailHeader(
                            post = p,
                            onAuthorClick = { onNavigateToUserProfile(p.userName) },
                            onReaction = { viewModel.toggleReaction() },
                            onSave = { viewModel.toggleSave() }
                        )
                    }
                }

                if (comments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("まだ返信がありません", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    // タイムラインと同じ形にすることで、返信であることが一目で分かる
                    items(comments, key = { it.commentId }) { comment ->
                        CommentRow(
                            comment = comment,
                            onAuthorClick = { onNavigateToUserProfile(comment.userName) },
                            onDelete = { viewModel.deleteComment(comment.commentId) }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("返信をポスト", color = TextSecondary, fontSize = 15.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = SubBackground,
                    focusedContainerColor = SubBackground,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val text = inputText.trim()
                    if (text.isNotBlank()) {
                        inputText = ""
                        viewModel.addComment(text)
                    }
                },
                enabled = inputText.isNotBlank() && !isSending
            ) {
                Icon(
                    Icons.Outlined.Send,
                    contentDescription = "返信",
                    tint = if (inputText.isNotBlank() && !isSending) Accent else TextSecondary
                )
            }
        }
    }
}

/** 返信1件。タイムラインの投稿と同じ「アバター左・1行メタ・本文」の形 */
@Composable
private fun CommentRow(
    comment: Comment,
    onAuthorClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isMine = comment.userId == AppState.userId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    ) {
        UserAvatar(
            displayName = comment.displayName,
            imageUrl = comment.userImageUrl,
            size = 36.dp,
            fontSize = 15.sp,
            modifier = Modifier.clickable(onClick = onAuthorClick)
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "@${comment.userName} · ${comment.createdAt}",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.content,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (isMine) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "削除",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
    HorizontalDivider(color = BorderColor, thickness = 1.dp)
}

private fun postTypeLabel(postType: String): String = when (postType) {
    "achievement" -> "達成報告"
    "insight" -> "気づき"
    "question" -> "相談"
    else -> "今日の進捗"
}

@Composable
private fun PostDetailHeader(
    post: Post,
    onAuthorClick: () -> Unit,
    onReaction: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clickable(onClick = onAuthorClick)) {
                UserAvatar(
                    displayName = post.displayName,
                    imageUrl = post.authorImageUrl,
                    size = 42.dp,
                    fontSize = 17.sp
                )
                FocusAvatarIndicator(post.authorFocus, size = 14.dp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(post.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (post.authorFocus != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        FocusMiniBadge(post.authorFocus)
                    }
                }
                Text("@${post.userName}", fontSize = 15.sp, color = TextSecondary)
            }
        }

        // 詳細では本文を一段大きくして主役にする
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = post.content,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (post.imageUrl != null) {
            Spacer(modifier = Modifier.height(14.dp))
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            )
        }

        if (post.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                post.tags.forEach { tag ->
                    Text(tag, fontSize = 15.sp, color = Accent, modifier = Modifier.padding(end = 10.dp))
                }
            }
        }

        if (post.goalTitle != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(post.goalTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (post.progress != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ProgressRow(label = "この投稿時点の進捗", progress = post.progress)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "${post.createdAt} ・ ${postTypeLabel(post.postType)}",
            fontSize = 14.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DetailAction(
            icon = Icons.Outlined.ChatBubbleOutline,
            count = post.commentCount,
            tint = TextSecondary,
            description = "返信",
            onClick = {}
        )
        DetailAction(
            icon = if (post.myReaction) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            count = post.reactionCount,
            tint = if (post.myReaction) LikeColor else TextSecondary,
            description = "いいね",
            onClick = onReaction
        )
        DetailAction(
            icon = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            count = null,
            tint = if (post.isSaved) Accent else TextSecondary,
            description = if (post.isSaved) "保存を解除" else "保存",
            onClick = onSave
        )
    }

    }
}

@Composable
private fun DetailAction(
    icon: ImageVector,
    count: Int?,
    tint: Color,
    description: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp), tint = tint)
        if (count != null && count > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "$count", fontSize = 14.sp, color = tint)
        }
    }
}
