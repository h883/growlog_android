package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.Background
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.ui.theme.Warning

private data class TutorialPage(
    val imageUrl: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val heading: String,
    val body: String
)

@Composable
fun TutorialScreen(onFinished: () -> Unit, onLogin: () -> Unit) {
    val pages = remember {
        listOf(
            TutorialPage(
                "https://lh3.googleusercontent.com/aida-public/AB6AXuA7np2FpSKq7JBdlDpm2wTGW6ukTH5p8r4zej0R3emVUSYpsp_lkbGea2xleMBsVTqWY5GsgMl4SpxoD0OObXrlf75pp0w3KfZSpQTumHFZv7PWXwmXlVdGC9alMp08eMw1VRYHO878rO_Yrtm9ee6WLe_F1H30d-mG1H-gfEyjPXoRAREKDHWE2N6vh3-uI4Zv09oKt8oTtAmc-nzNS61SHMBAq27FUN6ZoHLel8Zen67-xUCufoZ3",
                Icons.Outlined.TrendingUp,
                "途中経過も、立派な成果。",
                "完成する前の学びや失敗も、気軽に記録できます。"
            ),
            TutorialPage(
                "https://lh3.googleusercontent.com/aida-public/AB6AXuClxNB57Z_ZA5ggdiBzzs78mFraciawsa-53iuX3V3aBJZD769W69L8WgB8X1meG_yRD3F4GolAesuGZlqikEd1Z6pPvNx1fZaGnGSJolxe5yJDojdiqcajDQ4DPjwBxxyZI6dfgnGHIKr-OayJdPsXNurMnM38ICnW2zKAfoBiFSv-oGTyO0K-KRb9o-_I1jOIfaA7ht-9Qo2aQtBtAg8UtXaKkoG2KzF1Pk6p87vc4hXsdEYQcXfP",
                Icons.Outlined.Eco,
                "集中した時間が、形になる。",
                "集中セッションを続けると、植物や街、ロボットが少しずつ育ちます。"
            ),
            TutorialPage(
                "https://lh3.googleusercontent.com/aida-public/AB6AXuAIM-SJ5ZXRcvhvCxkxTCQiq2MEgCADCixheIrByf_NjKC3se4gEu4g_VIGJNms1QTU45cxYjA4-4rbgZkq6biUtn5fEZrMNz8w6IaObGipF31q64Z9gosCPQJd7Oy9ZYJotCo5guGZjJitGGNJgbKBRb7l_EzyNTcYhj87yJUW8SQvILuNLu6MXv3j9sc5sJOYpq8MobYRdMrlu6DktKw5yq-NUgwz8ARjkebvSszSpa_e11GGDYoY",
                Icons.Outlined.Groups,
                "同じ目標の仲間とつながる。",
                "グループで進捗を共有し、静かに応援し合えます。"
            )
        )
    }
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]
    val finalPage = pageIndex == pages.lastIndex

    Column(Modifier.fillMaxSize().background(Background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("GrowLog", color = Accent, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onLogin) { Text("スキップ", color = TextSecondary, fontSize = 16.sp) }
        }
        Column(Modifier.weight(1f).padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(278.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = page.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
                    )
                    // Offline or image-load fallback remains visible under the remote illustration.
                    Icon(page.icon, null, tint = Accent.copy(.2f), modifier = Modifier.size(92.dp))
                }
            }
            Spacer(Modifier.height(52.dp))
            Text(page.heading, fontSize = 27.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(16.dp))
            Text(page.body, fontSize = 17.sp, lineHeight = 29.sp, textAlign = TextAlign.Center, color = TextSecondary)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                pages.indices.forEach { index ->
                    Surface(
                        modifier = Modifier.height(9.dp).width(if (index == pageIndex) 34.dp else 9.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (index == pageIndex) if (finalPage) Warning else Accent else Accent.copy(.15f)
                    ) {}
                }
            }
            Spacer(Modifier.height(28.dp))
        }
        Button(
            onClick = { if (finalPage) onFinished() else pageIndex++ },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).height(62.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (finalPage) Warning else Accent)
        ) {
            if (finalPage) Text("Googleで始める", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            else { Text("次へ", fontSize = 19.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(10.dp)); Icon(Icons.Outlined.ArrowForward, null) }
        }
        Spacer(Modifier.height(18.dp))
        if (!finalPage) TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("スキップ", color = TextSecondary, fontSize = 15.sp) }
        Spacer(Modifier.height(8.dp))
    }
}
