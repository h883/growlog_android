package com.example.sns_v1.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.CardBackground

/** 画面共通の余白。カードの左右マージンと内側パディングをここで揃える */
val CardHorizontalMargin = 16.dp
val CardGap = 10.dp
val CardPadding = 18.dp

/**
 * X 風の一覧行。カードではなく、下端の細い区切り線で分ける。
 * 影も角丸も付けないのは、X のタイムラインが1枚の紙に見えるようにするため。
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CardHorizontalMargin, vertical = 5.dp)
            .background(CardBackground, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = padding, vertical = 14.dp),
        content = content
    )
}
