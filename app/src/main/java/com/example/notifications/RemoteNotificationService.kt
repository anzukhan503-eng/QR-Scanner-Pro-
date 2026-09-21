package com.example.notifications

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.repository.SettingsRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object RemoteNotificationService {

    private const val TAG = "RemoteNotifService"

    suspend fun syncRemoteNotifications(context: Context) = withContext(Dispatchers.IO) {
        val settingsRepo = SettingsRepository(context)
        if (!settingsRepo.notificationsEnabled.value) {
            return@withContext
        }

        val db = AppDatabase.getDatabase(context)
        val notifDao = db.notificationDao()

        try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("notifications")
                .limit(20)
                .get()
                .await()

            for (doc in snapshot.documents) {
                val title = doc.getString("title") ?: continue
                val message = doc.getString("message") ?: continue
                val type = doc.getString("type") ?: "TIP"
                val actionUrl = doc.getString("actionUrl")
                val timestamp = doc.getLong("scheduledTime") ?: System.currentTimeMillis()

                val newId = notifDao.insertNotification(
                    com.example.data.model.AppNotification(
                        title = title,
                        message = message,
                        timestamp = timestamp,
                        type = type,
                        actionUrl = actionUrl,
                        isRead = false
                    )
                )

                if (newId > 0) {
                    NotificationHelper.showSystemNotification(
                        context = context,
                        notificationId = (newId % 10000).toInt(),
                        title = title,
                        message = message,
                        channelId = if (type == "UPDATE") NotificationHelper.CHANNEL_UPDATES else NotificationHelper.CHANNEL_DAILY_TIPS
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Backend sync note: ${e.message}")
        }
    }
}
