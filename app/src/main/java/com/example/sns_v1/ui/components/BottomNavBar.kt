package com.example.sns_v1.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.navigation.Route
import com.example.sns_v1.ui.theme.Accent

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

private val BarHeight = 64.dp
private val BarCorner = 26.dp
private val ActiveCircleRadius = 25.dp
private val CutoutRadius = 30.dp
private val ItemInset = 8.dp

/**
 * 選択中の項目が円形にせり出し、バー側がその周りを丸くえぐられるフローティングナビ。
 * えぐれは「角丸矩形から円を引く」ことで作っているので、
 * [notchCenterX] を動かすだけでへこみが滑らかに移動する。
 */
private class NotchedBarShape(
    private val notchCenterX: Dp,
    private val cutoutRadius: Dp,
    private val cornerRadius: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val corner = with(density) { cornerRadius.toPx() }
        val radius = with(density) { cutoutRadius.toPx() }
        val centerX = with(density) { notchCenterX.toPx() }

        val bar = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    cornerRadius = CornerRadius(corner, corner)
                )
            )
        }
        // 円の中心をバーの上辺に置くと、上半分がバーの外に出て「えぐれ」になる
        val cutout = Path().apply {
            addOval(Rect(center = Offset(centerX, 0f), radius = radius))
        }

        val result = Path()
        result.op(bar, cutout, PathOperation.Difference)
        return Outline.Generic(result)
    }
}

@Composable
fun GrowLogBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    // 投稿は右下の FAB に出したので、ここは閲覧系の5項目だけ
    val items = listOf(
        BottomNavItem(Route.Home.route, Icons.Outlined.Home, "ホーム"),
        BottomNavItem(Route.Discover.route, Icons.Outlined.Search, "見つける"),
        BottomNavItem(Route.Notifications.route, Icons.Outlined.Notifications, "通知"),
        BottomNavItem(Route.Messages.route, Icons.Outlined.MailOutline, "DM"),
        BottomNavItem(Route.Profile.route, Icons.Outlined.Person, "マイページ"),
    )

    // 投稿画面はタブとして残らないので、選択中が見つからなければホームを指したままにする
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    val barColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(ActiveCircleRadius + BarHeight)
        ) {
            // 両端の項目でえぐれがバーの角を食い尽くさないよう、内側に少し余白を取る
            val itemWidth = (maxWidth - ItemInset * 2) / items.size
            val targetCenterX = ItemInset + itemWidth * (selectedIndex + 0.5f)
            val notchCenterX by animateDpAsState(
                targetValue = targetCenterX,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "notchCenterX"
            )

            val barShape = NotchedBarShape(notchCenterX, CutoutRadius, BarCorner)

            // バー本体
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(BarHeight)
                    .shadow(elevation = 10.dp, shape = barShape, clip = false)
                    .background(color = barColor, shape = barShape)
                    .border(width = 1.dp, color = outlineColor, shape = barShape)
            )

            // せり出す円（選択中の項目のアイコン）
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = notchCenterX - ActiveCircleRadius)
                    .size(ActiveCircleRadius * 2)
                    .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                    .background(color = barColor, shape = CircleShape)
                    .border(width = 1.dp, color = outlineColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = items[selectedIndex].icon,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            // タップ領域とラベル
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(BarHeight)
                    .padding(horizontal = ItemInset)
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onNavigate(item.route) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            // アイコンは上の円に出ているので、ここではラベルだけを下寄せで置く
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Accent,
                                maxLines = 1,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp)
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = inactiveColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    color = inactiveColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
