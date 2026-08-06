package com.example.sns_v1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.sns_v1.navigation.Route
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.Warning

private data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun GrowLogBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(
        BottomNavItem(Route.Home.route, Icons.Outlined.Home, "ホーム"),
        BottomNavItem(Route.Discover.route, Icons.Outlined.Search, "見つける"),
        BottomNavItem(Route.CreatePost.route, Icons.Outlined.Add, "新しい記録"),
        BottomNavItem(Route.Notifications.route, Icons.Outlined.Notifications, "通知"),
        BottomNavItem(Route.Profile.route, Icons.Outlined.Person, "プロフィール")
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .shadow(14.dp, RoundedCornerShape(32.dp), clip = false),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val isCreate = item.route == Route.CreatePost.route
                Box(
                    modifier = Modifier
                        .size(if (isCreate) 54.dp else 48.dp)
                        .background(
                            when {
                                isCreate -> Warning
                                selected -> Accent.copy(alpha = .78f)
                                else -> androidx.compose.ui.graphics.Color.Transparent
                            },
                            CircleShape
                        )
                        .clickable { onNavigate(item.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected || isCreate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(if (isCreate) 30.dp else 25.dp)
                    )
                }
            }
        }
    }
}
