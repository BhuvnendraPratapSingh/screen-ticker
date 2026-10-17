package com.socialwatch.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.socialwatch.app.BuildConfig
import com.socialwatch.app.R
import com.socialwatch.app.ui.components.SWSwitch
import com.socialwatch.app.util.PermissionHelper

@Composable
fun SettingsScreen(
    onDarkModeChange: (Boolean) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Large title
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ── Monitoring Section ──
        item {
            SettingsSection(title = stringResource(R.string.monitoring_section)) {
                SettingsToggleRow(
                    icon = Icons.Rounded.PowerSettingsNew,
                    iconTint = Color(0xFF34C759),
                    title = stringResource(R.string.start_on_boot),
                    subtitle = stringResource(R.string.start_on_boot_desc),
                    checked = state.startOnBoot,
                    onCheckedChange = { viewModel.setStartOnBoot(it) },
                    showDivider = true
                )
                SettingsToggleRow(
                    icon = Icons.Rounded.Notifications,
                    iconTint = Color(0xFFFF9500),
                    title = "Notification Timer",
                    subtitle = "Show running time in the notification bar",
                    checked = state.showNotificationTimer,
                    onCheckedChange = { viewModel.setShowNotificationTimer(it) },
                    showDivider = false
                )
            }
        }

        // ── Appearance Section ──
        item {
            SettingsSection(title = stringResource(R.string.appearance_section)) {
                SettingsToggleRow(
                    icon = Icons.Rounded.DarkMode,
                    iconTint = Color(0xFF5856D6),
                    title = stringResource(R.string.dark_mode),
                    subtitle = stringResource(R.string.dark_mode_desc),
                    checked = state.darkMode,
                    onCheckedChange = {
                        viewModel.setDarkMode(it)
                        onDarkModeChange(it)
                    },
                    showDivider = false
                )
            }
        }

        // ── Permissions Section ──
        item {
            SettingsSection(title = "PERMISSIONS") {
                SettingsNavigateRow(
                    icon = Icons.Rounded.Shield,
                    iconTint = Color(0xFF007AFF),
                    title = "Usage Access",
                    subtitle = if (PermissionHelper.hasUsageStatsPermission(context)) "Granted" else "Not granted",
                    onClick = {
                        context.startActivity(PermissionHelper.usageStatsSettingsIntent())
                    },
                    showDivider = true
                )
                SettingsNavigateRow(
                    icon = Icons.Rounded.Layers,
                    iconTint = Color(0xFFFF9500),
                    title = stringResource(R.string.overlay_permission_title),
                    subtitle = if (PermissionHelper.hasOverlayPermission(context)) "Granted" else "Not granted",
                    onClick = {
                        context.startActivity(PermissionHelper.overlaySettingsIntent(context))
                    },
                    showDivider = false
                )
            }
        }

        // ── About Section ──
        item {
            SettingsSection(title = stringResource(R.string.about_section)) {
                SettingsNavigateRow(
                    icon = Icons.Rounded.Info,
                    iconTint = Color(0xFF8E8E93),
                    title = stringResource(R.string.about_app),
                    subtitle = "${stringResource(R.string.version)} ${BuildConfig.VERSION_NAME}",
                    onClick = { showAboutDialog = true },
                    showDivider = false
                )
            }
        }

        // ── Made By ──
        item {
            Text(
                text = "Made by Bhuvnendra",
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    // About dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.about_description))
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Reusable Settings Components ─────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(iconTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SWSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }

        if (showDivider) {
            Divider(
                modifier = Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun SettingsNavigateRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(iconTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }

        if (showDivider) {
            Divider(
                modifier = Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )
        }
    }
}
