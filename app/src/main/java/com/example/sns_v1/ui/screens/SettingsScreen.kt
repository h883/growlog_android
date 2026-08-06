package com.example.sns_v1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sns_v1.AppState
import com.example.sns_v1.BuildConfig
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.Background
import com.example.sns_v1.ui.theme.CardBackground
import com.example.sns_v1.ui.theme.ErrorRed
import com.example.sns_v1.ui.theme.TextSecondary

object SettingsPreferences {
    const val FILE = "growlog_settings"
    const val REACTIONS = "reactions"
    const val COMMENTS = "comments"
    const val FOLLOWS = "follows"
    const val FOCUS_ALERTS = "focus_alerts"
    const val DEFAULT_TIMER = "default_timer"
}

@Composable
fun SettingsScreen(onBack: () -> Unit = {}, onLogout: () -> Unit = {}, onEditProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(SettingsPreferences.FILE, android.content.Context.MODE_PRIVATE) }
    fun setting(key: String, default: Boolean) = mutableStateOf(prefs.getBoolean(key, default))
    var reactions by remember { setting(SettingsPreferences.REACTIONS, true) }
    var comments by remember { setting(SettingsPreferences.COMMENTS, true) }
    var follows by remember { setting(SettingsPreferences.FOLLOWS, true) }
    var focusAlerts by remember { setting(SettingsPreferences.FOCUS_ALERTS, false) }
    var timer by remember { setting(SettingsPreferences.DEFAULT_TIMER, true) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var infoDialog by remember { mutableStateOf<String?>(null) }
    fun update(key: String, value: Boolean, set: (Boolean) -> Unit) { prefs.edit().putBoolean(key, value).apply(); set(value) }

    Column(Modifier.fillMaxSize().background(Background)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
            Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SettingsSection("Account") {
                SettingsNavigationRow(Icons.Outlined.PersonOutline, "Edit profile", "${AppState.displayName}  @${AppState.userName}", onEditProfile)
                SettingsNavigationRow(Icons.Outlined.PersonOutline, "Google account", "Signed in", { infoDialog = "Your GrowLog account is connected with Google. Use Log out below to disconnect this device." })
            }
            SettingsSection("Notifications") {
                SettingsToggleRow(Icons.Outlined.Notifications, "Reactions", "Show reaction notifications", reactions) { update(SettingsPreferences.REACTIONS, it) { reactions = it } }
                SettingsToggleRow(Icons.Outlined.Notifications, "Comments", "Show comment notifications", comments) { update(SettingsPreferences.COMMENTS, it) { comments = it } }
                SettingsToggleRow(Icons.Outlined.PersonOutline, "Follows", "Show new follower notifications", follows) { update(SettingsPreferences.FOLLOWS, it) { follows = it } }
                SettingsToggleRow(Icons.Outlined.Timer, "Focus mode", "Show a notification when focus ends", focusAlerts) { update(SettingsPreferences.FOCUS_ALERTS, it) { focusAlerts = it } }
            }
            SettingsSection("Focus mode") {
                SettingsToggleRow(Icons.Outlined.Timer, "Use 25-minute default", "Use 25 minutes for a new focus session", timer) { update(SettingsPreferences.DEFAULT_TIMER, it) { timer = it } }
            }
            SettingsSection("About") {
                SettingsNavigationRow(Icons.Outlined.Description, "Terms of use", "", { infoDialog = "GrowLog is a personal growth journal. Please do not post content that violates laws, rights, or community safety." })
                SettingsNavigationRow(Icons.Outlined.PrivacyTip, "Privacy policy", "", { infoDialog = "Your profile, posts, and focus visibility are controlled by the sharing choices you make in GrowLog." })
                SettingsNavigationRow(Icons.Outlined.Description, "GrowLog Version ${BuildConfig.VERSION_NAME}", "", { infoDialog = "GrowLog Version ${BuildConfig.VERSION_NAME}" })
            }
            Spacer(Modifier.height(16.dp))
            Surface(color = CardBackground, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { showLogoutConfirm = true }) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Logout, null, tint = ErrorRed); Spacer(Modifier.width(12.dp)); Text("Log out", color = ErrorRed, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (showLogoutConfirm) AlertDialog(onDismissRequest = { showLogoutConfirm = false }, title = { Text("Log out?", fontWeight = FontWeight.Bold) }, text = { Text("You can sign in again with Google at any time.") }, confirmButton = { TextButton(onClick = { showLogoutConfirm = false; onLogout() }) { Text("Log out", color = ErrorRed) } }, dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") } })
    infoDialog?.let { message -> AlertDialog(onDismissRequest = { infoDialog = null }, title = { Text("GrowLog") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { infoDialog = null }) { Text("OK", color = Accent) } }) }
}

@Composable private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) { Text(title, color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)); Surface(color = CardBackground, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Column(content = content) } }
@Composable private fun SettingsNavigationRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Accent, modifier = Modifier.size(21.dp)); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium); if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = TextSecondary) }; Icon(Icons.Outlined.ChevronRight, null, tint = TextSecondary) } }
@Composable private fun SettingsToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Accent, modifier = Modifier.size(21.dp)); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium); Text(subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp) }; Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = Color.White)) } }
