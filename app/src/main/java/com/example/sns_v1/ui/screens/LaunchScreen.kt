package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.sns_v1.ui.theme.Background
import kotlinx.coroutines.delay

/** Stitch の起動画面。余白だけを見せてからアプリ本体へフェードする。 */
@Composable
fun LaunchScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(950)
        onFinished()
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    )
}
