package com.example.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onNavigateToScanHistory: () -> Unit,
    onNavigateToGeneratedHistory: () -> Unit,
    onClearNotificationHistory: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationsEnabled by settingsRepository.notificationsEnabled.collectAsState()
    val dailyTipsEnabled by settingsRepository.dailyTipsEnabled.collectAsState()
    val darkModeTheme by settingsRepository.darkModeTheme.collectAsState()
    val vibrateOnScan by settingsRepository.vibrateOnScan.collectAsState()
    val beepOnScan by settingsRepository.beepOnScan.collectAsState()

    var showClearNotifsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        settingsRepository.setNotificationsEnabled(granted)
    }

    if (showClearNotifsDialog) {
        AlertDialog(
            onDismissRequest = { showClearNotifsDialog = false },
            title = { Text("Clear Notification History?") },
            text = { Text("All saved notifications will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearNotificationHistory()
                        showClearNotifsDialog = false
                        scope.launch { snackbarHostState.showSnackbar("Notification history cleared") }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearNotifsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    listOf(
                        "SYSTEM" to "System Default",
                        "LIGHT" to "Light Mode",
                        "DARK" to "Dark Mode"
                    ).forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsRepository.setDarkModeTheme(key)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = darkModeTheme == key,
                                onClick = {
                                    settingsRepository.setDarkModeTheme(key)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .height(300.dp)
                ) {
                    Text(
                        text = "QR Scanner Pro - Privacy Policy\n\n" +
                                "1. Camera Data:\n" +
                                "The camera is used solely for real-time decoding of QR codes and barcodes on your device. Video frames are never recorded, transmitted, or stored on external servers.\n\n" +
                                "2. Local Storage:\n" +
                                "Scan and generation history are stored strictly on your local device. You retain full control to clear history at any time.\n\n" +
                                "3. Notifications:\n" +
                                "We send periodic useful productivity tips. You can toggle off notifications at any time in these settings.\n\n" +
                                "4. Advertising:\n" +
                                "Advertisements are served via Unity LevelPlay/Unity Ads according to industry safety and privacy guidelines.\n\n" +
                                "Developed by Niazi Traders.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Accept & Close")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About QR Scanner Pro") },
            text = {
                Column {
                    Text(
                        text = "QR Scanner Pro v1.0.0 Pro\n\n" +
                                "DEVELOPED BY NIAZI TRADERS\n\n" +
                                "A professional, high-performance Android QR code and barcode utility designed for instant scanning, custom generation, and secure local management.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("Help & FAQ") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .height(300.dp)
                ) {
                    Text(
                        text = "Q: How do I turn on the flashlight?\n" +
                                "A: Tap the flashlight icon in the top right corner of the camera scanner screen.\n\n" +
                                "Q: Why do I watch an ad to scan?\n" +
                                "A: Rewarded advertisements keep QR Scanner Pro free and continuously updated with new features.\n\n" +
                                "Q: Can I scan Wi-Fi QR codes?\n" +
                                "A: Yes! Scanning a Wi-Fi QR code automatically identifies network details and offers a one-tap connection shortcut.\n\n" +
                                "Q: How do I save generated codes?\n" +
                                "A: After generating, tap 'Save Image' to save directly to your phone's Pictures gallery.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFaqDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Section 1: Notifications
            SettingsSectionHeader(title = "Notifications")

            SettingsSwitchItem(
                title = "Enable Notifications",
                subtitle = "Receive updates and scanning tips",
                icon = Icons.Default.Notifications,
                isChecked = notificationsEnabled,
                onCheckedChange = { isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        settingsRepository.setNotificationsEnabled(isChecked)
                    }
                },
                testTag = "setting_notifications_toggle"
            )

            SettingsSwitchItem(
                title = "Daily QR Tips",
                subtitle = "Receive one useful tip per day",
                icon = Icons.Default.Lightbulb,
                isChecked = dailyTipsEnabled,
                enabled = notificationsEnabled,
                onCheckedChange = { settingsRepository.setDailyTipsEnabled(it) },
                testTag = "setting_daily_tips_toggle"
            )

            SettingsClickableItem(
                title = "Clear Notification History",
                subtitle = "Delete all stored notification records",
                icon = Icons.Default.DeleteSweep,
                onClick = { showClearNotifsDialog = true },
                testTag = "setting_clear_notifications"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Appearance & Preferences
            SettingsSectionHeader(title = "Appearance & Feedback")

            SettingsClickableItem(
                title = "Dark Mode",
                subtitle = when (darkModeTheme) {
                    "LIGHT" -> "Light Mode"
                    "DARK" -> "Dark Mode"
                    else -> "System Default"
                },
                icon = Icons.Default.DarkMode,
                onClick = { showThemeDialog = true },
                testTag = "setting_dark_mode"
            )

            SettingsSwitchItem(
                title = "Vibrate on Scan",
                subtitle = "Haptic feedback when QR code is found",
                icon = Icons.Default.VolumeUp,
                isChecked = vibrateOnScan,
                onCheckedChange = { settingsRepository.setVibrateOnScan(it) },
                testTag = "setting_vibrate"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: History Shortcuts
            SettingsSectionHeader(title = "History Management")

            SettingsClickableItem(
                title = "Scan History",
                subtitle = "View and export scanned codes",
                icon = Icons.Default.History,
                onClick = onNavigateToScanHistory,
                testTag = "setting_scan_history"
            )

            SettingsClickableItem(
                title = "Generated QR History",
                subtitle = "Manage created QR codes",
                icon = Icons.Default.QrCode,
                onClick = onNavigateToGeneratedHistory,
                testTag = "setting_generated_history"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 4: About & Legal
            SettingsSectionHeader(title = "About & Support")

            SettingsClickableItem(
                title = "Help & FAQ",
                subtitle = "Frequently asked questions and guides",
                icon = Icons.Default.HelpOutline,
                onClick = { showFaqDialog = true },
                testTag = "setting_faq"
            )

            SettingsClickableItem(
                title = "Privacy Policy",
                subtitle = "How your data and privacy are protected",
                icon = Icons.Default.PrivacyTip,
                onClick = { showPrivacyDialog = true },
                testTag = "setting_privacy"
            )

            SettingsClickableItem(
                title = "About Developer",
                subtitle = "DEVELOPED BY NIAZI TRADERS",
                icon = Icons.Default.Info,
                onClick = { showAboutDialog = true },
                testTag = "setting_about"
            )

            SettingsClickableItem(
                title = "Share QR Scanner Pro",
                subtitle = "Share with colleagues and friends",
                icon = Icons.Default.Share,
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Scan and generate high-precision QR codes with QR Scanner Pro! Developed by Niazi Traders.")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share App"))
                },
                testTag = "setting_share_app"
            )

            SettingsClickableItem(
                title = "App Version",
                subtitle = "v1.0.0 Pro (Build 1)",
                icon = Icons.Default.Star,
                onClick = {
                    scope.launch { snackbarHostState.showSnackbar("QR Scanner Pro is up to date!") }
                },
                testTag = "setting_version"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag(testTag),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (enabled) 1f else 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f)
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
