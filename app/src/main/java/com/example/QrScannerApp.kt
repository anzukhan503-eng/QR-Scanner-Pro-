package com.example

import android.app.Application
import android.util.Log
import com.example.ads.UnityAdsManager
import com.example.data.db.AppDatabase
import com.example.notifications.DailyNotificationWorker
import com.example.notifications.NotificationHelper

class QrScannerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Initializing QR Scanner Pro application...")

        // 1. Initialize Room Database
        AppDatabase.getDatabase(this)

        // 2. Setup Notification Channels
        NotificationHelper.createNotificationChannels(this)

        // 3. Initialize Unity LevelPlay / Unity Ads SDK
        try {
            UnityAdsManager.getInstance().initialize(this)
        } catch (e: Exception) {
            Log.w(TAG, "UnityAds init skipped: ${e.message}")
        }

        // 4. Schedule battery-friendly 24-hour daily notification worker
        try {
            DailyNotificationWorker.scheduleDailyNotification(this)
        } catch (e: Exception) {
            Log.w(TAG, "WorkManager scheduling skipped: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "QrScannerApp"
    }
}
