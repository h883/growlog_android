package com.example.sns_v1.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.AppState
import com.example.sns_v1.BuildConfig
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.ErrorRed
import com.example.sns_v1.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "戻る")
            }
            Text(
                "設定",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            SectionLabel("アカウント")
            InfoRow("表示名", AppState.displayName)
            InfoRow("ユーザー名", "@${AppState.userName}")

            SectionLabel("アプリ")
            InfoRow("バージョン", BuildConfig.VERSION_NAME)

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutConfirm = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Logout,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("ログアウト", color = ErrorRed, fontSize = 15.sp)
            }
            HorizontalDivider(color = BorderColor, thickness = 1.dp)
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("ログアウトしますか？", fontSize = 17.sp) },
            text = { Text("再度ログインするには Google アカウントでのサインインが必要です。", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onLogout()
                }) {
                    Text("ログアウト", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("キャンセル", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value.ifBlank { "—" }, fontSize = 14.sp, color = TextSecondary)
    }
    HorizontalDivider(color = BorderColor, thickness = 1.dp)
}
