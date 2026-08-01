package com.example.sns_v1.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.utils.isIsoDate
import com.example.sns_v1.utils.today

@Composable
fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, startDate: String, endDate: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    // 空欄なら「期限なし」。入力があるときだけ形式を検査する
    val endDateInvalid = endDate.isNotBlank() && !isIsoDate(endDate)
    val canCreate = title.isNotBlank() && !endDateInvalid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新しい目標", fontSize = 18.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("タイトル") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderColor,
                        focusedBorderColor = Accent
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 200) description = it },
                    label = { Text("説明（任意）") },
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderColor,
                        focusedBorderColor = Accent
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("期限（任意）") },
                    placeholder = { Text("2026-12-31", color = TextSecondary) },
                    supportingText = {
                        Text(
                            if (endDateInvalid) "YYYY-MM-DD の形式で入力してください"
                            else "空欄なら期限なし"
                        )
                    },
                    isError = endDateInvalid,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderColor,
                        focusedBorderColor = Accent
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        title.trim(),
                        description.trim(),
                        today(),
                        endDate.trim().ifBlank { null }
                    )
                    onDismiss()
                },
                enabled = canCreate
            ) {
                Text("作成", color = if (canCreate) Accent else TextSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル", color = TextSecondary)
            }
        }
    )
}
