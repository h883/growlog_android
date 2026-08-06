package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.GroupVisibility
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.Background
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.ui.theme.Warning

@Composable
fun CreateGroupScreen(onBack: () -> Unit, onCreate: (String, String, String, GroupVisibility) -> Unit) {
    var name by remember { mutableStateOf("") }; var slug by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(GroupVisibility.PUBLIC) }
    val validSlug = Regex("^[A-Za-z0-9_-]{3,32}$").matches(slug)
    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(62.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "戻る") }
            Text("グループを作成", fontWeight = FontWeight.Bold, fontSize = 21.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(Modifier.align(Alignment.CenterHorizontally).size(92.dp), CircleShape, color = Accent.copy(.12f)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AddAPhoto, null, tint = Accent, modifier = Modifier.size(36.dp)) } }
            Text("カバー画像を追加", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally))
            GroupField("グループ名", name, "基本情報技術者試験・勉強部") { name = it.take(50) }
            GroupField("グループID", slug, "fe-study") { slug = it.take(32) }
            Text(if (slug.isBlank() || validSlug) "URLで使用します（英数字・ハイフン・アンダースコア）" else "3〜32文字の英数字・ハイフン・アンダースコアで入力してください", color = if (validSlug || slug.isBlank()) TextSecondary else MaterialTheme.colorScheme.error, fontSize = 12.sp)
            GroupField("グループの説明（任意）", description, "どんなグループかを紹介しましょう", false) { description = it.take(300) }
            Text("公開範囲", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            GroupVisibility.entries.forEach { option ->
                val selected = visibility == option
                val icon = when(option) { GroupVisibility.PUBLIC -> Icons.Outlined.Public; GroupVisibility.APPROVAL -> Icons.Outlined.VerifiedUser; GroupVisibility.PRIVATE -> Icons.Outlined.Lock }
                Surface(Modifier.fillMaxWidth().clickable { visibility = option }, RoundedCornerShape(16.dp), color = if (selected) Accent.copy(.08f) else MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Accent else BorderColor)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Accent); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(option.label, fontWeight = FontWeight.Bold); Text(option.description, fontSize = 12.sp, color = TextSecondary) }; RadioButton(selected, { visibility = option }, colors = RadioButtonDefaults.colors(selectedColor = Accent)) }
                }
            }
            Text("参加タイプ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Group, null, tint = Accent); Spacer(Modifier.width(12.dp)); Column { Text("誰でも参加可能", fontWeight = FontWeight.Medium); Text("グループ作成後に詳細設定できます", fontSize = 12.sp, color = TextSecondary) } } }
            Spacer(Modifier.height(8.dp))
        }
        Button(onClick = { onCreate(name.trim(), slug.trim(), description.trim(), visibility) }, enabled = name.isNotBlank() && validSlug, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp).height(58.dp), shape = RoundedCornerShape(30.dp), colors = ButtonDefaults.buttonColors(containerColor = Warning)) { Text("グループを作成", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}
@Composable private fun GroupField(label: String, value: String, hint: String, singleLine: Boolean = true, change: (String) -> Unit) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold); OutlinedTextField(value, change, placeholder = { Text(hint, color = TextSecondary.copy(.6f)) }, singleLine = singleLine, minLines = if(singleLine) 1 else 3, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = BorderColor)) }
