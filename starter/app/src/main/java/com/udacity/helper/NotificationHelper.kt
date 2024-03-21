package com.udacity.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.udacity.R

class NotificationHelper private constructor(private val context: Context) {

    private val channelId = context.getString(R.string.download_notification_channel_id)
    private val notificationManager: NotificationManager by lazy {
        ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) as NotificationManager
    }
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

    fun <C : AppCompatActivity> sendNotification(targetActivity: Class<C>) {
        val contentIntent = Intent(context, targetActivity)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_assistant_black)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_content))
            .setContentIntent(contentPendingIntent)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine("\n")
                    .addLine(context.getString(R.string.notification_text))
                )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    companion object {
        private const val NOTIFICATION_ID = 0
        private const val REQUEST_CODE = 100

        fun with(context: Context): NotificationHelper {
            val helper = NotificationHelper(context.applicationContext)
            helper.createChannel(
                context.getString(R.string.download_notification_channel_id),
                context.getString(R.string.download_notification_channel_name)
            )
            return helper
        }
    }
}