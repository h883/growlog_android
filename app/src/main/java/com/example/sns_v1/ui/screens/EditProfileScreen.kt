package com.example.sns_v1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.ProfileViewModel

private const val BIO_MAX_LENGTH = 160
private const val NAME_MAX_LENGTH = 30

@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit = {}
) {
    val profile by viewModel.profile.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // 読み込み完了で流れてくる値を初期値にしたいので、profile の変化に追従させる
    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var biography by remember(profile.biography) { mutableStateOf(profile.biography) }
    var isSaving by remember { mutableStateOf(false) }

    val isDirty = displayName != profile.displayName || biography != profile.biography
    val canSave = displayName.isNotBlank() && isDirty && !isSaving

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
                "プロフィール編集",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .weight(1f)
            )
            Button(
                onClick = {
                    isSaving = true
                    viewModel.saveProfile(displayName.trim(), biography.trim()) {
                        isSaving = false
                        onBack()
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("保存", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        if (errorMessage != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("@${profile.userName}", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { if (it.length <= NAME_MAX_LENGTH) displayName = it },
                label = { Text("表示名") },
                supportingText = { Text("${displayName.length} / $NAME_MAX_LENGTH") },
                isError = displayName.isBlank(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderColor,
                    focusedBorderColor = Accent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = biography,
                onValueChange = { if (it.length <= BIO_MAX_LENGTH) biography = it },
                label = { Text("自己紹介") },
                supportingText = { Text("${biography.length} / $BIO_MAX_LENGTH") },
                minLines = 4,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderColor,
                    focusedBorderColor = Accent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "プロフィール画像はプロフィール画面のアイコンから変更できます。",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}
