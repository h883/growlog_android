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
    var showInviteDialog by remember { mutableStateOf(false) }

    if (showInviteDialog) {
        InviteUserDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { userName ->
                showInviteDialog = false
                viewModel.invite(userName)
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().height(53.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = group?.groupName ?: "Group",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider(color = BorderColor)

        error?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().clickable { viewModel.clearError() }
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        val current = group
        when {
            isLoading && current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This group is unavailable.", color = TextSecondary)
            }
            else -> {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        GroupHeader(
                            group = current,
                            onJoin = viewModel::join,
                            onLeave = viewModel::leave,
                            onInvite = { showInviteDialog = true }
                        )
                    }
                    item {
                        UnderlineTabs(
                            tabs = listOf("Home", "Members", "Info"),
                            selectedIndex = selectedTab,
                            onSelect = { selectedTab = it }
                        )
                        HorizontalDivider(color = BorderColor)
                    }

                    if (!current.canViewContent) {
                        item { LockedNotice() }
                    } else when (selectedTab) {
                        0 -> if (posts.isEmpty()) {
                            item { EmptyNotice("No posts yet.") }
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
                        1 -> if (members.isEmpty()) {
                            item { EmptyNotice("No members to show.") }
                        } else {
                            items(members, key = { it.userId }) { member ->
                                MemberRow(member) { onNavigateToUserProfile(member.userName) }
                            }
                        }
                        else -> item { GroupInfo(current) }
                    }
                }

                if (current.isMember && selectedTab == 0) {
                    GroupComposer(
                        value = composerText,
                        isPosting = isPosting,
                        onChange = { composerText = it },
                        onPost = {
                            val text = composerText.trim()
                            if (text.isNotEmpty()) {
                                composerText = ""
                                viewModel.post(text)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(group: Group, onJoin: () -> Unit, onLeave: () -> Unit, onInvite: () -> Unit) {
    val canInvite = group.myRole in setOf("owner", "admin", "moderator")
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroupIcon(group.iconImageUrl, group.groupName, 60.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(group.groupName, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GroupVisibilityBadge(group.visibility)
                    Text(" · ${group.memberCount}/${group.maximumMembers}", fontSize = 13.sp, color = TextSecondary)
                }
            }
        }
        if (group.description.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(group.description, fontSize = 15.sp, lineHeight = 22.sp)
        }
        Spacer(Modifier.height(14.dp))
        when {
            group.joinType == "invite" && canInvite -> Button(
                onClick = onInvite,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) { Text("\u30e6\u30fc\u30b6\u30fc\u540d\u3067\u62db\u5f85", fontWeight = FontWeight.Bold) }
            group.isMember -> OutlinedButton(
                onClick = onLeave,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) { Text("\u30b0\u30eb\u30fc\u30d7\u3092\u9000\u4f1a", fontWeight = FontWeight.Bold) }
            group.joinType == "open" -> JoinButton(onJoin, "\u53c2\u52a0\u3059\u308b")
            group.joinType == "approval" -> JoinButton(onJoin, "\u53c2\u52a0\u3092\u7533\u8acb")
            else -> OutlinedButton(
                onClick = {},
                enabled = false,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) { Text("\u62db\u5f85\u304c\u5fc5\u8981\u3067\u3059") }
        }
    }
}

@Composable
private fun JoinButton(onClick: () -> Unit, label: String) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryButton),
        modifier = Modifier.fillMaxWidth().height(40.dp)
    ) { Text(label, fontWeight = FontWeight.Bold) }
}

@Composable
private fun InviteUserDialog(onDismiss: () -> Unit, onInvite: (String) -> Unit) {
    var userName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u30e6\u30fc\u30b6\u30fc\u3092\u62db\u5f85") },
        text = {
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("\u30e6\u30fc\u30b6\u30fc\u540d") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("\u30ad\u30e3\u30f3\u30bb\u30eb") } },
        confirmButton = {
            TextButton(onClick = { onInvite(userName.trim()) }, enabled = userName.trim().isNotEmpty()) {
                Text("\u62db\u5f85")
            }
        }
    )
}

@Composable
private fun GroupComposer(
    value: String,
    isPosting: Boolean,
    onChange: (String) -> Unit,
    onPost: () -> Unit
) {
    HorizontalDivider(color = BorderColor)
    Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("Write a post", color = TextSecondary) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = SubBackground,
                focusedContainerColor = SubBackground,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            maxLines = 4
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onPost,
            enabled = value.isNotBlank() && !isPosting,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) { Text("Post", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun LockedNotice() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(10.dp))
        Text("Join this group to view its content.", color = TextSecondary)
    }
}

@Composable
private fun EmptyNotice(text: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = TextSecondary)
    }
}

@Composable
private fun PinnedLabel() {
    Row(Modifier.padding(start = 16.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.PushPin, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSecondary)
        Spacer(Modifier.width(4.dp))
        Text("Pinned", fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun MemberRow(member: GroupMember, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(member.displayName, member.profileImageUrl, 42.dp, 17.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(member.displayName, fontWeight = FontWeight.Bold)
                Text("@${member.userName}", fontSize = 13.sp, color = TextSecondary)
            }
            if (member.role != "member") {
                Surface(shape = RoundedCornerShape(50), color = SubBackground) {
                    Text(member.roleLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
        HorizontalDivider(color = BorderColor)
    }
}

@Composable
private fun GroupInfo(group: Group) {
    Column(Modifier.padding(16.dp)) {
        InfoRow("Group ID", group.groupSlug)
        InfoRow("Visibility", group.visibility.label)
        InfoRow("Joining", when (group.joinType) {
            "open" -> "Open"
            "approval" -> "Admin approval required"
            else -> "Invitation only"
        })
        InfoRow("Members", "${group.memberCount} / ${group.maximumMembers}")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, color = TextSecondary, modifier = Modifier.width(110.dp))
        Text(value, fontWeight = FontWeight.Medium)
    }
    HorizontalDivider(color = BorderColor)
}
