package com.example.sns_v1.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary

@Composable
fun LoginScreen(onGoogleSignIn: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GrowLog",
                modifier = Modifier.padding(top = 20.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Accent
            )

            Spacer(Modifier.height(30.dp))

            Text(
                text = "ログイン",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = "あなたの挑戦を記録しましょう",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            OutlinedButton(
                onClick = onGoogleSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Text("G", color = androidx.compose.ui.graphics.Color(0xFF4285F4), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(11.dp))
                Text("Googleでログイン", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
            }

            Spacer(Modifier.height(28.dp))
            LoginGrowthMark()
        }
    }
}

@Composable
private fun LoginGrowthMark() {
    Canvas(modifier = Modifier.width(132.dp).height(54.dp)) {
        val xPositions = listOf(.12f, .30f, .48f, .66f, .84f)
        val heights = listOf(.43f, .75f, 1f, .72f, .34f)
        xPositions.zip(heights).forEach { (position, height) ->
            val x = size.width * position
            val bottom = size.height * .9f
            drawLine(
                color = Accent.copy(alpha = .30f),
                start = androidx.compose.ui.geometry.Offset(x, bottom),
                end = androidx.compose.ui.geometry.Offset(x, bottom - size.height * height * .64f),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
