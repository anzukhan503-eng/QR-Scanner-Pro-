package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.db.AppDatabase
import com.example.data.repository.GeneratedRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ScanRepository
import com.example.data.repository.SettingsRepository
import com.example.notifications.NotificationHelper
import com.example.notifications.RemoteNotificationService
import com.example.scanner.QrTypeDetector
import com.example.ui.create.CreateQrScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.notifications.NotificationScreen
import com.example.ui.result.ScanResultScreen
import com.example.ui.scanner.ScannerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.QrScannerProTheme
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var scanRepository: ScanRepository
    private lateinit var generatedRepository: GeneratedRepository
    private lateinit var notificationRepository: NotificationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)
        scanRepository = ScanRepository(db.scanDao())
        generatedRepository = GeneratedRepository(db.generatedDao())
        notificationRepository = NotificationRepository(db.notificationDao())

        // Check if opened from a notification intent
        val navigateTo = intent.getStringExtra(NotificationHelper.EXTRA_NAVIGATE_TO)

        setContent {
            val darkModeTheme by settingsRepository.darkModeTheme.collectAsState()
            val isDark = when (darkModeTheme) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            QrScannerProTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                val unreadNotifications by notificationRepository.unreadCount.collectAsState(initial = 0)

                // Notification permission launcher for Android 13+
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* no-op or handled gracefully */ }

                // Seed initial notification, trigger daily 24h cycle, and request permission
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    try {
                        notificationRepository.seedInitialNotificationsIfNeeded(this@MainActivity)
                        notificationRepository.checkAndDeliverDailyNotification(this@MainActivity)
                        RemoteNotificationService.syncRemoteNotifications(this@MainActivity)
                    } catch (_: Exception) {}
                }

                // If launched from notification, navigate directly to notifications screen after splash
                LaunchedEffect(navigateTo) {
                    if (navigateTo == "notifications") {
                        navController.navigate("notifications")
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onSplashFinished = {
                                    navController.navigate("home") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                unreadNotificationCount = unreadNotifications,
                                onNavigateToScanner = {
                                    navController.navigate("scanner")
                                },
                                onNavigateToCreate = {
                                    navController.navigate("create")
                                },
                                onNavigateToScanHistory = {
                                    navController.navigate("history?tab=0")
                                },
                                onNavigateToGeneratedHistory = {
                                    navController.navigate("history?tab=1")
                                },
                                onNavigateToNotifications = {
                                    navController.navigate("notifications")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToAbout = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable("scanner") {
                            ScannerScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onQrScanned = { rawText, qrType ->
                                    val parsed = QrTypeDetector.parse(rawText)
                                    scope.launch {
                                        scanRepository.insertScan(
                                            content = rawText,
                                            qrType = qrType,
                                            displayTitle = parsed.title
                                        )
                                    }
                                    val encodedContent = URLEncoder.encode(rawText, StandardCharsets.UTF_8.toString())
                                    navController.navigate("result/$encodedContent") {
                                        popUpTo("scanner") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "result/{encodedContent}",
                            arguments = listOf(navArgument("encodedContent") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val encoded = backStackEntry.arguments?.getString("encodedContent") ?: ""
                            val decoded = try {
                                URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                            } catch (_: Exception) {
                                encoded
                            }

                            ScanResultScreen(
                                scannedContent = decoded,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onScanAgain = {
                                    navController.navigate("scanner") {
                                        popUpTo("home")
                                    }
                                }
                            )
                        }

                        composable("create") {
                            CreateQrScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onSaveToHistory = { qrType, content, title ->
                                    scope.launch {
                                        generatedRepository.insertGenerated(qrType, content, title)
                                    }
                                }
                            )
                        }

                        composable(
                            route = "history?tab={tab}",
                            arguments = listOf(
                                navArgument("tab") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                }
                            )
                        ) { backStackEntry ->
                            val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
                            HistoryScreen(
                                initialTab = initialTab,
                                scansFlow = scanRepository.allScans,
                                generatedFlow = generatedRepository.allGenerated,
                                onDeleteScan = { id ->
                                    scope.launch { scanRepository.deleteScan(id) }
                                },
                                onDeleteGenerated = { id ->
                                    scope.launch { generatedRepository.deleteGenerated(id) }
                                },
                                onClearAllScans = {
                                    scope.launch { scanRepository.clearAll() }
                                },
                                onClearAllGenerated = {
                                    scope.launch { generatedRepository.clearAll() }
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("notifications") {
                            NotificationScreen(
                                notificationsFlow = notificationRepository.allNotifications,
                                onMarkAsRead = { id ->
                                    scope.launch { notificationRepository.markAsRead(id) }
                                },
                                onMarkAllAsRead = {
                                    scope.launch { notificationRepository.markAllAsRead() }
                                },
                                onDeleteNotification = { id ->
                                    scope.launch { notificationRepository.deleteNotification(id) }
                                },
                                onClearAll = {
                                    scope.launch { notificationRepository.clearAll() }
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                settingsRepository = settingsRepository,
                                onNavigateToScanHistory = {
                                    navController.navigate("history?tab=0")
                                },
                                onNavigateToGeneratedHistory = {
                                    navController.navigate("history?tab=1")
                                },
                                onClearNotificationHistory = {
                                    scope.launch { notificationRepository.clearAll() }
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

