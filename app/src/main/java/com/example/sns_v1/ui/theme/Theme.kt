package com.example.sns_v1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = CardBackground,
    background = Background,
    onBackground = TextPrimary,
    // surface はカード。背景より一段明るくして浮かせる
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = SubBackground,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    secondary = TextSecondary,
    onSecondary = CardBackground,
    error = ErrorRed,
    // 未指定だとダイアログやチップに M3 既定の紫が出るので、全段階を明示する
    surfaceContainerLowest = CardBackground,
    surfaceContainerLow = CardBackground,
    surfaceContainer = CardBackground,
    surfaceContainerHigh = CardBackground,
    surfaceContainerHighest = SubBackground,
    secondaryContainer = SubBackground,
    onSecondaryContainer = Accent,
)

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CardBackgroundDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SubBackgroundDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    secondary = TextSecondaryDark,
    error = ErrorRed,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = CardBackgroundDark,
    surfaceContainer = CardBackgroundDark,
    surfaceContainerHigh = CardBackgroundDark,
    surfaceContainerHighest = SubBackgroundDark,
    secondaryContainer = SubBackgroundDark,
    onSecondaryContainer = TextPrimaryDark,
)

@Composable
fun SNSV1Theme(
    // 参考デザインの明るい紙面調を常に保つ。
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
