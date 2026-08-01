package com.example.sns_v1.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sns_v1.AppState
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.model.Goal
import com.example.sns_v1.network.ApiClient
import com.example.sns_v1.ui.components.UserAvatar
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.AccentSoft
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.utils.compressImageForUpload
import com.example.sns_v1.viewmodel.CreatePostViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onCancel: () -> Unit = {},
    onPost: () -> Unit = {}
) {
    var content by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedGoal by remember { mutableStateOf<Goal?>(null) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var showGoalPicker by remember { mutableStateOf(false) }
    var showProgressPicker by remember { mutableStateOf(false) }
    var showTagInput by remember { mutableStateOf(false) }
    var postType by remember { mutableStateOf("progress") }
    val goals by viewModel.goals.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }

    fun submitPost() {
        scope.launch {
            isPosting = true
            errorMsg = null
            try {
                val token = TokenManager.getIdToken()
                val imageKey = imageUri?.let { uri ->
                    val bytes = withContext(Dispatchers.IO) { compressImageForUpload(context, uri) }
                        ?: throw IllegalStateException("画像を読み込めませんでした")
                    ApiClient.instance.uploadImage(token, bytes, "image/jpeg").getOrElse { throw it }.imageKey
                }
                ApiClient.instance.createPost(
                    idToken = token,
                    content = content,
                    postType = postType,
                    imageKey = imageKey,
                    goalId = selectedGoal?.goalId,
                    progress = progress,
                    tags = tags
                ).onSuccess { onPost() }.onFailure { errorMsg = it.message }
            } catch (e: Exception) {
                errorMsg = e.message ?: "投稿に失敗しました"
            } finally {
                isPosting = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(62.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel, contentPadding = PaddingValues(8.dp)) {
                Text("キャンセル", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = "GrowLog",
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = ::submitPost,
                enabled = content.isNotBlank() && !isPosting,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    disabledContainerColor = Accent.copy(alpha = .48f)
                )
            ) {
                if (isPosting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("投稿", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        if (errorMsg != null) {
            Text(
                text = errorMsg.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }

        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                displayName = AppState.displayName.ifBlank { "あなた" },
                imageUrl = null,
                size = 52.dp,
                fontSize = 18.sp
            )
            Spacer(Modifier.width(14.dp))
            Text("◉  全員が返信できます", color = Accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        TextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text("何に取り組んでいますか？", color = TextSecondary.copy(alpha = .62f), fontSize = 24.sp) },
            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, lineHeight = 30.sp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        imageUri?.let { uri ->
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                AsyncImage(
                    model = uri, contentDescription = "選択した画像", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).clip(RoundedCornerShape(12.dp))
                )
                IconButton(
                    onClick = { imageUri = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).background(Color.Black.copy(alpha = .48f), CircleShape)
                ) { Icon(Icons.Outlined.Close, "画像を削除", tint = Color.White) }
            }
        }
        if (tags.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { tag ->
                    AssistChip(onClick = { tags = tags - tag }, label = { Text(tag) }, trailingIcon = { Icon(Icons.Outlined.Close, null, Modifier.size(14.dp)) })
                }
            }
        }

        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        PostToolBar(
            onImage = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onGoal = { showGoalPicker = true },
            onProgress = { if (selectedGoal != null) showProgressPicker = true else showGoalPicker = true },
            onTag = { showTagInput = true },
            onType = { postType = if (postType == "progress") "insight" else "progress" }
        )
    }

    if (showGoalPicker) GoalPickerDialog(goals, selectedGoal?.goalId, { showGoalPicker = false }) { goal ->
        selectedGoal = goal
        if (goal == null) progress = null
        showGoalPicker = false
    }
    if (showProgressPicker) ProgressPickerDialog(progress ?: selectedGoal?.progress ?: 0, { showProgressPicker = false }, { progress = it; showProgressPicker = false }) {
        progress = null; showProgressPicker = false
    }
    if (showTagInput) TagInputDialog({ showTagInput = false }) { tag ->
        if (tag !in tags) tags = tags + tag
        showTagInput = false
    }
}

@Composable
private fun PostToolBar(onImage: () -> Unit, onGoal: () -> Unit, onProgress: () -> Unit, onTag: () -> Unit, onType: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        PostToolIcon(Icons.Outlined.Image, "画像", onImage)
        PostToolIcon(Icons.Outlined.MyLocation, "目標", onGoal)
        PostToolIcon(Icons.Outlined.TrendingUp, "進捗", onProgress)
        PostToolIcon(Icons.Outlined.Label, "タグ", onTag)
        Box(Modifier.width(1.dp).height(30.dp).background(BorderColor))
        PostToolIcon(Icons.Outlined.AddCircleOutline, "投稿タイプ", onType)
    }
}

@Composable
private fun PostToolIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, label, tint = Accent, modifier = Modifier.size(25.dp)) }
}

@Composable
private fun GoalPickerDialog(goals: List<Goal>, selectedGoalId: String?, onDismiss: () -> Unit, onSelect: (Goal?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("目標を選択") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (goals.isEmpty()) Text("進行中の目標はありません", color = TextSecondary)
                goals.forEach { goal ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = goal.goalId == selectedGoalId, onClick = { onSelect(goal) })
                        Text(goal.title, Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSelect(null) }) { Text("選択しない") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("閉じる") } }
    )
}

@Composable
private fun ProgressPickerDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit, onClear: () -> Unit) {
    var value by remember { mutableIntStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("進捗を設定") },
        text = {
            Column {
                Text("$value%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Accent)
                Slider(value = value.toFloat(), onValueChange = { value = it.toInt() }, valueRange = 0f..100f, steps = 19,
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent))
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("設定", color = Accent) } },
        dismissButton = { TextButton(onClick = onClear) { Text("設定しない") } }
    )
}

@Composable
private fun TagInputDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("タグを追加") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it.take(24) }, placeholder = { Text("例：#朝活") }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { onAdd("#" + text.trim().removePrefix("#")) }, enabled = text.trim().isNotEmpty()) { Text("追加", color = Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}
