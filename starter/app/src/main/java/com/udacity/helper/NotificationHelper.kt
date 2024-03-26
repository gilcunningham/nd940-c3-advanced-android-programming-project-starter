package com.udacity.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.udacity.R

class NotificationHelper private constructor(private val context: Context) {

    private val channelId = context.getString(R.string.download_notification_channel_id)
    private val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }
    private val notificationManager: NotificationManager by lazy {
        ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager
    }

    fun areNotificationsEnabled() = notificationManager.areNotificationsEnabled()

    private fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = context.getString(R.string.download_notification_channel_description)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    fun sendNotification(contentIntent : Intent) {
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            contentIntent,
            pendingIntentFlags
        )
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_assistant_black_24dp)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_assistant_black_24dp,
                context.getString(R.string.notification_text),
                contentPendingIntent
            )
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    companion object {
        private const val NOTIFICATION_ID = 0
        private const val REQUEST_CODE = 100

        fun with(context: Context): NotificationHelper {
            return NotificationHelper(context.applicationContext).apply {
                createChannel(
                    context.getString(R.string.download_notification_channel_id),
                    context.getString(R.string.download_notification_channel_name)
                )
            }
        }
    }
}