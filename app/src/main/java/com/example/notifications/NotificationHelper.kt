package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_DAILY_TIPS = "qr_daily_tips_channel"
    const val CHANNEL_UPDATES = "qr_updates_channel"
    const val EXTRA_NAVIGATE_TO = "navigate_to_screen"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val dailyTipsChannel = NotificationChannel(
                CHANNEL_DAILY_TIPS,
                "QR Scanner Pro Daily Tips",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily productivity tips and QR scanner recommendations"
                enableLights(true)
                lightColor = 0xFF0F9D58.toInt()
                enableVibration(true)
            }

            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES,
                "App Announcements & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important updates, new features and security alerts"
                enableLights(true)
                lightColor = 0xFF10B981.toInt()
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(dailyTipsChannel)
            notificationManager.createNotificationChannel(updatesChannel)
        }
    }

    fun showSystemNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        channelId: String = CHANNEL_DAILY_TIPS
    ) {
        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_NAVIGATE_TO, "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_qr_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF0F9D58.toInt())

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission was revoked
        }
    }
}
