package com.example.sns_v1.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.ui.theme.Background
import kotlinx.coroutines.delay

/** Stitch の起動画面。余白だけを見せてからアプリ本体へフェードする。 */
@Composable
fun LaunchScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(950)
        onFinished()
    }

    val transition = rememberInfiniteTransition(label = "launch")
    val scale = transition.animateFloat(0.94f, 1.04f, infiniteRepeatable(androidx.compose.animation.core.tween(1150, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "leaf")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(104.dp).background(Accent.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Eco, null, tint = Accent, modifier = Modifier.size(62.dp).scale(scale.value))
            }
            Spacer(Modifier.height(20.dp))
            Text("GrowLog", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Accent)
            Spacer(Modifier.height(8.dp))
            Text("挑戦の成長を、記録しよう。", fontSize = 13.sp, color = TextSecondary)
        }
    }
}
