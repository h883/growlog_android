package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sns_v1.model.Group
import com.example.sns_v1.model.GroupVisibility
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.SubBackground
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.viewmodel.GroupsViewModel

@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onBack: () -> Unit = {},
    onOpenGroup: (String) -> Unit = {}
) {
    val groups by viewModel.groups.collectAsState()
    val query by viewModel.query.collectAsState()
    val mineOnly by viewModel.mineOnly.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showCreate by remember { mutableStateOf(false) }

    if (showCreate) {
        CreateGroupDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, slug, description, visibility ->
                viewModel.createGroup(name, slug, description, visibility) { groupId ->
                    showCreate = false
                    onOpenGroup(groupId)
                }
            }
        )
    }

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
                "グループ",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            IconButton(onClick = { showCreate = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "グループを作成", tint = Accent)
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)

        TextField(
            value = query,
            onValueChange = { viewModel.onQueryChange(it) },
            placeholder = { Text("グループを検索", color = TextSecondary, fontSize = 15.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary,
                    modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = SubBackground,
                focusedContainerColor = SubBackground,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPill("すべて", !mineOnly) { viewModel.setMineOnly(false) }
            FilterPill("参加中", mineOnly) { viewModel.setMineOnly(true) }
        }

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

        Spacer(modifier = Modifier.height(6.dp))

        when {
            isLoading && groups.isEmpty() ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            groups.isEmpty() ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (mineOnly) "参加しているグループはありません" else "グループが見つかりません",
                        color = TextSecondary, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showCreate = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) { Text("グループを作る", fontWeight = FontWeight.Bold) }
                }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(groups, key = { it.groupId }) { group ->
                    GroupRow(group = group, onClick = { onOpenGroup(group.groupId) })
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Accent else SubBackground,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.background else TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun GroupVisibilityBadge(visibility: GroupVisibility) {
    val icon = when (visibility) {
        GroupVisibility.PUBLIC -> Icons.Outlined.Public
        GroupVisibility.APPROVAL -> Icons.Outlined.VerifiedUser
        GroupVisibility.PRIVATE -> Icons.Outlined.Lock
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSecondary)
        Spacer(modifier = Modifier.width(3.dp))
        Text(visibility.label, fontSize = 13.sp, color = TextSecondary)
    }
}

@Composable
private fun GroupRow(group: Group, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GroupIcon(group.iconImageUrl, group.groupName, 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    group.groupName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GroupVisibilityBadge(group.visibility)
                    Text(" ・ ${group.memberCount}人", fontSize = 13.sp, color = TextSecondary)
                }
                if (group.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        group.description,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (group.isMember) {
                Spacer(modifier = Modifier.width(8.dp))
                Text("参加中", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Accent)
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
    }
}

@Composable
fun GroupIcon(imageUrl: String?, name: String, size: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size / 4),
        color = SubBackground
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(size / 4))
                )
            } else {
                Icon(
                    Icons.Outlined.Group,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(size / 2)
                )
            }
        }
    }
}
