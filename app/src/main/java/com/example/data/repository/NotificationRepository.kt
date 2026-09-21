package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.db.NotificationDao
import com.example.data.model.AppNotification
import com.example.notifications.NotificationCatalog
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NotificationRepository(private val notificationDao: NotificationDao) {
    val allNotifications: Flow<List<AppNotification>> = notificationDao.getAllNotifications()
    val unreadCount: Flow<Int> = notificationDao.getUnreadCount()

    /**
     * Ensures the app has notifications immediately upon first installation or launch,
     * so the notification tab is never empty on start.
     */
    suspend fun seedInitialNotificationsIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        try {
            val count = notificationDao.getNotificationCount()
            if (count == 0) {
                Log.d(TAG, "Seeding initial in-app notifications...")
                val currentTime = System.currentTimeMillis()

                notificationDao.insertNotification(
                    AppNotification(
                        title = "🎉 Welcome to QR Scanner Pro",
                        message = "Fast barcode & QR scanning, custom code generation, and history tracking are ready. A new daily tip will automatically arrive every 24 hours!",
                        timestamp = currentTime,
                        type = "ANNOUNCEMENT",
                        isRead = false
                    )
                )

                notificationDao.insertNotification(
                    AppNotification(
                        title = "💡 Instant Flashlight Tip",
                        message = "Scanning in dim environments? Tap the torch icon in the scanner toolbar to activate flash for instant detection.",
                        timestamp = currentTime - 30_000L,
                        type = "TIP",
                        isRead = false
                    )
                )

                // Initialize the daily notification timestamp so the 24-hour cycle begins
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                if (!prefs.contains(KEY_LAST_DAILY_TIME)) {
                    prefs.edit()
                        .putLong(KEY_LAST_DAILY_TIME, currentTime)
                        .putInt(KEY_TIP_INDEX, 0)
                        .apply()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not seed initial notifications: ${e.message}")
        }
    }

    /**
     * Checks if 24 hours have elapsed since the last daily notification.
     * If 24h has passed, automatically delivers the next rotating daily notification
     * both inside the app (Room DB) and to the Android system notification tray.
     */
    suspend fun checkAndDeliverDailyNotification(context: Context): Boolean = withContext(Dispatchers.IO) {
        val settingsRepo = SettingsRepository(context)
        if (!settingsRepo.notificationsEnabled.value || !settingsRepo.dailyTipsEnabled.value) {
            return@withContext false
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastTime = prefs.getLong(KEY_LAST_DAILY_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L

        if (currentTime - lastTime >= twentyFourHours) {
            deliverDailyNotification(context)
            return@withContext true
        }
        return@withContext false
    }

    /**
     * Delivers the next scheduled daily notification immediately.
     */
    suspend fun deliverDailyNotification(context: Context): Long = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentIndex = prefs.getInt(KEY_TIP_INDEX, 0)
        val nextTip = NotificationCatalog.getTipForIndex(currentIndex)
        val currentTime = System.currentTimeMillis()

        val insertedId = notificationDao.insertNotification(
            AppNotification(
                title = nextTip.title,
                message = nextTip.message,
                timestamp = currentTime,
                type = nextTip.type,
                isRead = false
            )
        )

        // Show Android system status-bar notification
        NotificationHelper.showSystemNotification(
            context = context,
            notificationId = (2000 + (currentIndex % 1000)),
            title = nextTip.title,
            message = nextTip.message,
            channelId = NotificationHelper.CHANNEL_DAILY_TIPS
        )

        // Update preferences to start next 24-hour interval
        prefs.edit()
            .putLong(KEY_LAST_DAILY_TIME, currentTime)
            .putInt(KEY_TIP_INDEX, currentIndex + 1)
            .apply()

        Log.i(TAG, "Delivered 24h daily notification [index $currentIndex]: ${nextTip.title}")
        insertedId
    }

    suspend fun insertNotification(
        title: String,
        message: String,
        type: String = "TIP",
        actionUrl: String? = null
    ): Long {
        val notification = AppNotification(
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            type = type,
            isRead = false,
            actionUrl = actionUrl
        )
        return notificationDao.insertNotification(notification)
    }

    suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun deleteNotification(id: Long) {
        notificationDao.deleteNotificationById(id)
    }

    suspend fun clearAll() {
        notificationDao.clearAllNotifications()
    }

    companion object {
        private const val TAG = "NotificationRepo"
        const val PREFS_NAME = "qr_scanner_daily_notification_prefs"
        const val KEY_LAST_DAILY_TIME = "last_daily_notification_time"
        const val KEY_TIP_INDEX = "last_daily_tip_index"
    }
}

