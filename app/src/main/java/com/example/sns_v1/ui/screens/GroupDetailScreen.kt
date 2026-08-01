package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.Group
import com.example.sns_v1.model.GroupMember
import com.example.sns_v1.ui.components.PostCard
import com.example.sns_v1.ui.components.UnderlineTabs
import com.example.sns_v1.ui.components.UserAvatar
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.PrimaryButton
import com.example.sns_v1.ui.theme.SubBackground
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.GroupDetailViewModel

@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onBack: () -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val group by viewModel.group.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPosting by viewModel.isPosting.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var composerText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(53.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "戻る")
            }
            Text(
                text = group?.groupName ?: "グループ",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        if (error != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.clickable { viewModel.clearError() }
            ) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        val current = group
        if (isLoading && current == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            return@Column
        }
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("グループを表示できません", color = TextSecondary, fontSize = 14.sp)
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item { GroupHeader(current, onJoin = { viewModel.join() }, onLeave = { viewModel.leave() }) }

            item {
                Surface(color = MaterialTheme.colorScheme.background) {
                    UnderlineTabs(
                        tabs = listOf("ホーム", "メンバー", "情報"),
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it }
                    )
                }
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
            }

            if (!current.canViewContent) {
                item { LockedNotice() }
                return@LazyColumn
            }

            when (selectedTab) {
                0 -> {
                    if (posts.isEmpty()) {
                        item { EmptyNotice("まだ投稿がありません") }
                    } else {
                        items(posts, key = { it.postId }) { post ->
                            if (post.isPinned) PinnedLabel()
                            PostCard(
                                post = post,
                                onCommentClick = { onNavigateToPostDetail(post.postId) },
                                onAuthorClick = { onNavigateToUserProfile(post.userName) },
                                onPostClick = { onNavigateToPostDetail(post.postId) }
                            )
                        }
                    }
                }
                1 -> {
                    if (members.isEmpty()) {
                        item { EmptyNotice("メンバーを表示できません") }
                    } else {
                        items(members, key = { it.userId }) { member ->
                            MemberRow(member) { onNavigateToUserProfile(member.userName) }
                        }
                    }
                }
                else -> item { GroupInfo(current) }
            }
        }

        // メンバーだけが投稿できる（仕様書 9）
        if (current.isMember && selectedTab == 0) {
            HorizontalDivider(color = BorderColor, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = composerText,
                    onValueChange = { composerText = it },
                    placeholder = { Text("グループに投稿", color = TextSecondary, fontSize = 15.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = SubBackground,
                        focusedContainerColor = SubBackground,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val text = composerText.trim()
                        if (text.isNotBlank()) {
                            composerText = ""
                            viewModel.post(text)
                        }
                    },
                    enabled = composerText.isNotBlank() && !isPosting,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("投稿", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(group: Group, onJoin: () -> Unit, onLeave: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroupIcon(group.iconImageUrl, group.groupName, 60.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.groupName, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GroupVisibilityBadge(group.visibility)
                    Text(
                        " ・ ${group.memberCount}/${group.maximumMembers}人",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        if (group.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(group.description, fontSize = 15.sp, lineHeight = 22.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))
        when {
            group.isOwner -> {
                Text(
                    "あなたはこのグループのオーナーです",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            group.isMember -> {
                OutlinedButton(
                    onClick = onLeave,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("参加中", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                }
            }
            group.joinType == "open" -> {
                Button(
                    onClick = onJoin,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryButton,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("参加する", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            else -> {
                // 承認制・非公開は第3段階で申請/招待に対応する
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text(
                        if (group.joinType == "approval") "参加申請（準備中）" else "招待制",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedNotice() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary,
            modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text("このグループの投稿はメンバーのみ閲覧できます",
            color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun EmptyNotice(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun PinnedLabel() {
    Row(
        modifier = Modifier.padding(start = 16.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.PushPin, contentDescription = null,
            modifier = Modifier.size(13.dp), tint = TextSecondary)
        Spacer(modifier = Modifier.width(4.dp))
        Text("固定された投稿", fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun MemberRow(member: GroupMember, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                displayName = member.displayName,
                imageUrl = member.profileImageUrl,
                size = 42.dp,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("@${member.userName}", fontSize = 13.sp, color = TextSecondary)
            }
            if (member.role != "member") {
                Surface(shape = RoundedCornerShape(50), color = SubBackground) {
                    Text(
                        member.roleLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
    }
}

@Composable
private fun GroupInfo(group: Group) {
    Column(modifier = Modifier.padding(16.dp)) {
        InfoRow("グループID", group.groupSlug)
        InfoRow("公開範囲", group.visibility.label)
        InfoRow("参加方法", when (group.joinType) {
            "open" -> "誰でも参加できる"
            "approval" -> "管理者の承認が必要"
            else -> "招待制"
        })
        InfoRow("メンバー", "${group.memberCount} / ${group.maximumMembers}人")
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "グループルールと管理者機能は今後追加されます。",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.width(110.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
    HorizontalDivider(color = BorderColor, thickness = 1.dp)
}
