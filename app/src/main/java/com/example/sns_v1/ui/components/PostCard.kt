package com.example.sns_v1.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import com.example.sns_v1.model.Post
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.LikeColor
import com.example.sns_v1.ui.theme.TextSecondary

/** 本文の1行目を見出し、2行目以降を本文として扱う */
fun splitPostContent(content: String): Pair<String, String> {
    val trimmed = content.trim()
    val title = trimmed.substringBefore('\n').trim()
    val body = trimmed.substringAfter('\n', "").trim()
    return title to body
}

@Composable
fun PostCard(
    post: Post,
    onReaction: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onAuthorClick: () -> Unit = {},
    onSave: () -> Unit = {},
    onPostClick: () -> Unit = {}
) {
    val reactionScale by animateFloatAsState(
        targetValue = if (post.myReaction) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "reactionScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onPostClick)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
    ) {
        Box(modifier = Modifier.clickable(onClick = onAuthorClick)) {
            UserAvatar(
                displayName = post.displayName,
                imageUrl = post.authorImageUrl,
                size = 40.dp,
                fontSize = 16.sp
            )
            // 集中中なら、アバターの右下に点を重ねる
            FocusAvatarIndicator(post.authorFocus)
        }
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // 名前・ID・時刻を1行に収めるのが X の基本形
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (post.authorFocus != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    FocusMiniBadge(post.authorFocus)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "@${post.userName} · ${post.createdAt}",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = post.content,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (post.imageUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
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
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    post.tags.take(3).forEach { tag ->
                        Text(
                            text = tag,
                            fontSize = 15.sp,
                            color = Accent,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            if (post.progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    ProgressRow(
                        label = post.goalTitle ?: "進捗",
                        progress = post.progress
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // X と同じく、アクションは等間隔で本文幅いっぱいに散らす
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    count = post.commentCount,
                    tint = TextSecondary,
                    contentDescription = "コメント",
                    onClick = onCommentClick
                )
                ActionButton(
                    icon = if (post.myReaction) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    count = post.reactionCount,
                    tint = if (post.myReaction) LikeColor else TextSecondary,
                    scale = reactionScale,
                    contentDescription = "いいね",
                    onClick = onReaction
                )
                ActionButton(
                    icon = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    count = null,
                    tint = if (post.isSaved) Accent else TextSecondary,
                    contentDescription = if (post.isSaved) "保存を解除" else "保存",
                    onClick = onSave
                )
                Spacer(modifier = Modifier.width(40.dp))
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    count: Int?,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
    scale: Float = 1f
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp * scale),
            tint = tint
        )
        if (count != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = if (count == 0) "" else "$count", fontSize = 13.sp, color = tint)
        }
    }
}
