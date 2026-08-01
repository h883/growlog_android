package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.Notification
import com.example.sns_v1.ui.components.AppCard
import com.example.sns_v1.ui.components.GrowLogTopBar
import com.example.sns_v1.ui.components.UnderlineTabs
import com.example.sns_v1.ui.components.UserAvatar
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.SubBackground
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.NotificationsViewModel

private val tabTypes = listOf(null, "reaction", "comment", "follow")

@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val filtered = remember(notifications, selectedTab) {
        val type = tabTypes[selectedTab]
        if (type == null) notifications else notifications.filter { it.type == type }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GrowLogTopBar(actions = {
            if (notifications.any { !it.isRead }) {
                TextButton(onClick = { viewModel.markAllRead() }) {
                    Icon(
                        Icons.Outlined.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Accent
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("すべて既読", fontSize = 13.sp, color = Accent)
                }
            }
        })

        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            Text(
                text = "通知",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
            )
            UnderlineTabs(
                tabs = listOf("すべて", "応援", "コメント", "フォロー"),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            filtered.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("通知はまだありません", color = TextSecondary, fontSize = 14.sp)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
            ) {
                items(filtered, key = { it.notificationId }) { notif ->
                    NotificationRow(notif)
                }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("通知は最大30日間保存されます", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notif: Notification) {
    AppCard(padding = 16.dp) {
    Row(verticalAlignment = Alignment.Top) {
        // 未読を示す小さなドット
        Box(
            modifier = Modifier
                .padding(top = 14.dp)
                .size(6.dp)
                .background(
                    color = if (notif.isRead) Color.Transparent else Accent,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(10.dp))

        UserAvatar(
            displayName = notif.actorDisplayName,
            imageUrl = notif.actorImageUrl,
            size = 38.dp,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val action = when (notif.type) {
                "reaction" -> "があなたの投稿を応援しました"
                "comment" -> "があなたの投稿にコメントしました"
                "follow" -> "があなたをフォローしました"
                else -> ""
            }
            Text(
                text = notif.actorDisplayName + action,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(notif.createdAt, fontSize = 11.sp, color = TextSecondary)

            if (notif.commentContent != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = SubBackground) {
                    Text(
                        text = "「${notif.commentContent}」",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            } else if (notif.postContent != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("「${notif.postContent}…」", fontSize = 12.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))
        val (icon, tint) = when (notif.type) {
            "reaction" -> Icons.Filled.Favorite to Accent
            "comment" -> Icons.Outlined.ChatBubbleOutline to TextSecondary
            else -> Icons.Outlined.PersonAdd to TextSecondary
        }
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(top = 10.dp)
                .size(18.dp)
        )
    }
    }
}
