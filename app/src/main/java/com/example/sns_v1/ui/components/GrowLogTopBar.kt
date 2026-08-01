package com.example.sns_v1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.ui.theme.TextSecondary

/**
 * 「GrowLog」＋右側のアイコンだけの軽いヘッダー。
 * [actions] を渡すと右端のアイコンを差し替えられる。
 */
@Composable
fun GrowLogTopBar(
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMessagesClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GrowLog",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.sns_v1.ui.theme.Accent,
                modifier = Modifier.weight(1f)
            )
            if (actions != null) {
                actions()
            } else {
                TopBarIcon(Icons.Outlined.Search, "検索", onSearchClick)
                if (onMessagesClick != null) {
                    TopBarIcon(Icons.Outlined.MailOutline, "メッセージ", onMessagesClick)
                }
                TopBarIcon(Icons.Outlined.Notifications, "通知", onNotificationsClick)
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f),
            thickness = 1.dp
        )
        }
    }
}

@Composable
fun TopBarIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(21.dp)
        )
    }
}

/** 「おすすめ / フォロー中 / 最新」のような、下線付きのテキストタブ */
@Composable
fun UnderlineTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            // IntrinsicSize.Max にしないと、下線の fillMaxWidth が親いっぱいまで広がってしまう
            // X のタブは等幅で、選択中のラベルぶんだけ短い下線が付く
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = { onSelect(index) })
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onBackground else TextSecondary,
                    modifier = Modifier.padding(vertical = 15.dp)
                )
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(if (selected) 52.dp else 0.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                )
            }
        }
    }
}
