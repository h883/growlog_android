package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary

private data class TutorialPage(val icon: ImageVector, val title: String, val body: String)

@Composable
fun TutorialScreen(onFinished: () -> Unit, onLogin: () -> Unit) {
    val pages = listOf(
        TutorialPage(Icons.Outlined.Eco, "GrowLogへようこそ", "完璧さよりも前進を。日々の小さな成長を記録し、仲間と共有しましょう。"),
        TutorialPage(Icons.Outlined.TrendingUp, "挑戦を記録する", "目標と進捗を残すことで、昨日の自分を少しずつ超えていけます。"),
        TutorialPage(Icons.Outlined.Groups, "仲間と成長する", "応援やコメントを通して、あなたの挑戦を続ける力に変えましょう。")
    )
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "GrowLog",
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 18.dp),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Accent
            )
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(page.icon, null, tint = Accent.copy(alpha = .23f), modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(34.dp))
                Text(page.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(10.dp))
                Text(page.body, textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 21.sp, color = TextSecondary)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    pages.indices.forEach { index ->
                        Box(
                            Modifier
                                .height(3.dp)
                                .width(if (index == pageIndex) 20.dp else 5.dp)
                                .background(if (index == pageIndex) Accent else BorderColor, CircleShape)
                        )
                    }
                }
            }
            HorizontalDivider(color = BorderColor)
            Button(
                onClick = {
                    if (pageIndex == pages.lastIndex) onFinished() else pageIndex++
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Text(if (pageIndex == pages.lastIndex) "はじめる" else "次へ  →", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally).height(42.dp)) {
                Text("ログインはこちら", color = TextSecondary, fontSize = 11.sp)
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
