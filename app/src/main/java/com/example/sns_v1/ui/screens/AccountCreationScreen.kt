package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.SubBackground
import com.example.sns_v1.ui.theme.TextSecondary

@Composable
fun AccountCreationScreen(
    onComplete: (String, String) -> Unit,
    onCancel: () -> Unit,
    canExit: Boolean = true
) {
    var displayName by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var biography by remember { mutableStateOf("") }
    // ユーザー名は API 側で初回ログイン時に作成済みなので、表示名だけを必須にする。
    val canRegister = displayName.isNotBlank()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().height(52.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canExit) {
                    IconButton(onClick = onCancel) { Icon(Icons.Outlined.Close, "閉じる", tint = TextSecondary, modifier = Modifier.size(20.dp)) }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                Text("GrowLog", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Accent, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.size(48.dp))
            }
            LinearProgressIndicator(
                progress = { .4f },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = Accent,
                trackColor = BorderColor
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(20.dp))
                Text("STEP 2 OF 5", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Accent)
                Spacer(Modifier.height(8.dp))
                Text("プロフィールを充実させましょう", fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
                Text("あなたの「成長の記録」を共有する準備を整えましょう。後からいつでも変更可能です。", fontSize = 12.sp, lineHeight = 19.sp, color = TextSecondary)
                Spacer(Modifier.height(22.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(modifier = Modifier.size(84.dp), shape = CircleShape, color = Accent.copy(alpha = .15f)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PersonOutline, null, tint = Accent.copy(alpha = .6f), modifier = Modifier.size(36.dp)) }
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).offset(x = 28.dp),
                        shape = CircleShape,
                        color = Accent
                    ) { Icon(Icons.Outlined.AddAPhoto, "アイコンを登録", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(7.dp).size(15.dp)) }
                }
                Spacer(Modifier.height(6.dp))
                Text("アイコンを登録", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(14.dp))
                ProfileField("名前", displayName, "例：山田 太郎") { displayName = it.take(24) }
                Spacer(Modifier.height(10.dp))
                ProfileField("ユーザー名", userName, "@ growlog_user") { userName = it.filter { char -> char.isLetterOrDigit() || char == '_' }.take(20) }
                Text("一意のハンドルネームを設定してください。", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(10.dp))
                ProfileField("自己紹介", biography, "あなたの目標や、取り組んでいることについて教えてください。", singleLine = false) { biography = it.take(160) }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (canExit) {
                    TextButton(onClick = onCancel, modifier = Modifier.height(44.dp)) { Icon(Icons.Outlined.ArrowBack, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("戻る", color = TextSecondary) }
                } else {
                    Spacer(Modifier.width(72.dp))
                }
                Button(
                    onClick = { onComplete(displayName.trim(), biography.trim()) },
                    enabled = canRegister,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, disabledContainerColor = Accent.copy(alpha = .40f))
                ) { Text("登録する", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String, placeholder: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) {
    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 4.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = .55f), fontSize = 12.sp) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SubBackground,
            unfocusedContainerColor = SubBackground,
            focusedBorderColor = Accent,
            unfocusedBorderColor = BorderColor
        )
    )
}
