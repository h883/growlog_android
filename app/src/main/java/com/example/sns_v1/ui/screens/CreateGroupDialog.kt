package com.example.sns_v1.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.GroupVisibility
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary

/** グループIDに使えるのは英数字・ハイフン・アンダースコア（仕様書 4.4） */
private val SLUG_PATTERN = Regex("^[A-Za-z0-9_-]{3,32}$")

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, slug: String, description: String, visibility: GroupVisibility) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(GroupVisibility.PUBLIC) }

    val slugInvalid = slug.isNotBlank() && !SLUG_PATTERN.matches(slug)
    val canCreate = name.isNotBlank() && SLUG_PATTERN.matches(slug)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("グループを作成", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("グループ名") },
                    placeholder = { Text("基本情報技術者 勉強会", color = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderColor, focusedBorderColor = Accent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = slug,
                    onValueChange = { if (it.length <= 32) slug = it },
                    label = { Text("グループID") },
                    placeholder = { Text("fe-study", color = TextSecondary) },
                    supportingText = {
                        Text(
                            if (slugInvalid) "英数字・ハイフン・アンダースコアで3〜32文字"
                            else "URLに使われます（例: /groups/${slug.ifBlank { "fe-study" }}）"
                        )
                    },
                    isError = slugInvalid,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderColor, focusedBorderColor = Accent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 300) description = it },
                    label = { Text("説明（任意）") },
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderColor, focusedBorderColor = Accent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("公開範囲", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                GroupVisibility.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { visibility = option }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = visibility == option,
                            onClick = { visibility = option },
                            colors = RadioButtonDefaults.colors(selectedColor = Accent)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(option.description, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), slug.trim(), description.trim(), visibility) },
                enabled = canCreate
            ) {
                Text("作成", color = if (canCreate) Accent else TextSecondary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル", color = TextSecondary) }
        }
    )
}
