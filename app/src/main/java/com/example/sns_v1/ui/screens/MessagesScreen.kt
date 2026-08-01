package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.ui.components.AppCard
import com.example.sns_v1.ui.components.UserAvatar
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.MessagesViewModel

@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    onBack: () -> Unit = {},
    onOpenChat: (String) -> Unit = {},
    /** メニューのタブとして開いたときは戻る矢印を出さない */
    showBack: Boolean = true
) {
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "戻る")
                }
            }
            Text(
                "メッセージ",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                modifier = Modifier.padding(start = if (showBack) 4.dp else 16.dp)
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        when {
            isLoading && conversations.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }
            conversations.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("メッセージはまだありません", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "ユーザーのプロフィールから送れます",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                ) {
                    items(conversations, key = { it.conversationId }) { conversation ->
                        AppCard(
                            onClick = { onOpenChat(conversation.conversationId) },
                            padding = 16.dp
                        ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(
                                displayName = conversation.partnerDisplayName,
                                imageUrl = conversation.partnerImageUrl,
                                size = 46.dp,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        conversation.partnerDisplayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (conversation.lastMessageAt != null) {
                                        Text(
                                            conversation.lastMessageAt,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = conversation.lastMessage ?: "まだメッセージがありません",
                                        fontSize = 13.sp,
                                        color = if (conversation.unreadCount > 0)
                                            MaterialTheme.colorScheme.onBackground else TextSecondary,
                                        fontWeight = if (conversation.unreadCount > 0)
                                            FontWeight.Medium else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (conversation.unreadCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(shape = CircleShape, color = Accent) {
                                            Text(
                                                text = "${conversation.unreadCount}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}
