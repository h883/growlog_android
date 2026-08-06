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
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.model.Goal
import com.example.sns_v1.ui.components.CreateGoalDialog
import com.example.sns_v1.ui.components.ProgressRow
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.Background
import com.example.sns_v1.ui.theme.BorderColor
import com.example.sns_v1.ui.theme.TextSecondary
import com.example.sns_v1.ui.theme.Warning
import com.example.sns_v1.viewmodel.ProfileViewModel

@Composable
fun GoalListScreen(viewModel: ProfileViewModel, onBack: () -> Unit, onOpenGoal: (String) -> Unit) {
    val goals by viewModel.goals.collectAsState()
    var creating by remember { mutableStateOf(false) }
    if (creating) CreateGoalDialog(onDismiss = { creating = false }, onCreate = { title, description, start, end -> viewModel.createGoal(title, description, start, end); creating = false })
    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(62.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
            Text("Goals", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButton(onClick = { creating = true }) { Icon(Icons.Outlined.Add, "Create goal", tint = Accent) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Eco, null, tint = Warning); Spacer(Modifier.width(8.dp)); Text("Active goals: ${goals.count { !it.isAchieved }}", fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
            items(goals.filterNot { it.isAchieved }, key = { it.goalId }) { goal -> GoalOverviewCard(goal) { onOpenGoal(goal.goalId) } }
            if (goals.isEmpty()) item { EmptyGoalCard() }
        }
        Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp).height(58.dp), shape = RoundedCornerShape(30.dp), colors = ButtonDefaults.buttonColors(containerColor = Warning)) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(7.dp)); Text("Create a new goal", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun GoalDetailScreen(viewModel: ProfileViewModel, goalId: String, onBack: () -> Unit) {
    val goals by viewModel.goals.collectAsState()
    val goal = goals.firstOrNull { it.goalId == goalId }
    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(62.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }; Text("Goal details", fontSize = 21.sp, fontWeight = FontWeight.Bold) }
        if (goal == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading goal", color = TextSecondary) } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(goal.title, fontSize = 28.sp, fontWeight = FontWeight.Bold); if(goal.description.isNotBlank()) Text(goal.description, color = TextSecondary, fontSize = 16.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 10.dp)); Spacer(Modifier.height(10.dp)); ProgressRow("Progress", goal.progress) }
            item { ProgressActions(goal, viewModel) }
            item { Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Timer, null, tint = Accent); Spacer(Modifier.width(12.dp)); Column { Text("Started ${goal.startDate}"); Text("${goal.activityCount} recorded activities", color = TextSecondary, fontSize = 13.sp) } } } }
            item { OutlinedButton(onClick = { viewModel.setGoalAchieved(goal.goalId, !goal.isAchieved) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp)) { Text(if(goal.isAchieved) "Mark as active" else "Mark as achieved", color = Accent, fontWeight = FontWeight.Bold) } }
        }
    }
}

@Composable private fun ProgressActions(goal: Goal, viewModel: ProfileViewModel) { Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) { Column(Modifier.padding(18.dp)) { Text("Today's step", color = Accent, fontWeight = FontWeight.Bold); Text("Record progress and help your goal grow.", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)); Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = { viewModel.updateGoalProgress(goal.goalId, goal.progress - 10) }, modifier = Modifier.weight(1f)) { Text("-10%") }; OutlinedButton(onClick = { viewModel.updateGoalProgress(goal.goalId, goal.progress + 10) }, modifier = Modifier.weight(1f)) { Text("+10%") } } } } }
@Composable private fun GoalOverviewCard(goal: Goal, click: () -> Unit) { Surface(Modifier.fillMaxWidth().clickable(onClick = click), RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) { Column(Modifier.padding(20.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(goal.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Surface(shape = RoundedCornerShape(20.dp), color = Accent.copy(.12f)) { Text(if(goal.progress >= 70) "On track" else "In progress", color = Accent, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) } }; if(goal.description.isNotBlank()) Text(goal.description, fontSize = 14.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp)); Spacer(Modifier.height(15.dp)); ProgressRow("Progress ${goal.progress}%", goal.progress); Text("${goal.activityCount} activities", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 8.dp)) } } }
@Composable private fun EmptyGoalCard() { Surface(Modifier.fillMaxWidth().height(210.dp), RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) { Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Outlined.Eco, null, tint = TextSecondary, modifier = Modifier.size(42.dp)); Spacer(Modifier.height(10.dp)); Text("Small habits grow into something meaningful.", color = TextSecondary, textAlign = TextAlign.Center) } } }
