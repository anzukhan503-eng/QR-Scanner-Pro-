package com.example.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.model.AppNotification
import com.example.data.repository.NotificationRepository
import com.example.data.repository.SettingsRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class DailyNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsRepo = SettingsRepository(appContext)

        // Respect user settings
        if (!settingsRepo.notificationsEnabled.value || !settingsRepo.dailyTipsEnabled.value) {
            Log.d(TAG, "Notifications disabled by user in Settings. Skipping.")
            return Result.success()
        }

        val db = AppDatabase.getDatabase(appContext)
        val notifDao = db.notificationDao()
        val notifRepo = NotificationRepository(notifDao)

        var notificationPosted = false

        try {
            // First attempt to query cloud backend for remote scheduled daily tip
            val firestore = FirebaseFirestore.getInstance()
            val querySnapshot = firestore.collection("daily_tips")
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents[0]
                val title = doc.getString("title") ?: "Daily Tip"
                val message = doc.getString("message") ?: "Discover handy QR tools with QR Scanner Pro."
                val type = doc.getString("type") ?: "TIP"

                val insertedId = notifDao.insertNotification(
                    AppNotification(
                        title = title,
                        message = message,
                        type = type,
                        timestamp = System.currentTimeMillis()
                    )
                )

                NotificationHelper.showSystemNotification(
                    context = appContext,
                    notificationId = 1001,
                    title = title,
                    message = message,
                    channelId = NotificationHelper.CHANNEL_DAILY_TIPS
                )
                Log.d(TAG, "Successfully posted backend daily notification: $title (ID $insertedId)")
                notificationPosted = true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Daily notification cloud query note: ${e.message}")
        }

        // If no cloud notification was delivered, deliver from the reliable rotating catalog
        if (!notificationPosted) {
            try {
                notifRepo.deliverDailyNotification(appContext)
                Log.d(TAG, "Successfully delivered local catalog 24h daily notification")
            } catch (e: Exception) {
                Log.e(TAG, "Failed delivering local 24h daily notification: ${e.message}")
            }
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "DailyNotificationWorker"
        private const val WORK_NAME = "qr_scanner_daily_notification_work"

        fun scheduleDailyNotification(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()

                // Schedule recurring every 24 hours
                val periodicRequest = PeriodicWorkRequestBuilder<DailyNotificationWorker>(
                    24, TimeUnit.HOURS,
                    1, TimeUnit.HOURS // 1 hour flex window to optimize system battery
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
                Log.d(TAG, "Enqueued 24h daily notification worker")
            } catch (e: Exception) {
                Log.w(TAG, "Could not initialize WorkManager: ${e.message}")
            }
        }

        fun cancelDailyNotification(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            } catch (e: Exception) {
                Log.w(TAG, "Could not cancel WorkManager work: ${e.message}")
            }
        }
    }
}
