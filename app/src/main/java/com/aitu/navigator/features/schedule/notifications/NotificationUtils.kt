package com.aitu.navigator.features.schedule.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
object NotificationUtils {

    const val CHANNEL_ID = "schedule_channel"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Schedule notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about upcoming lessons"
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showLessonNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        createChannel(context)

        fun showLessonNotification(
            context: Context,
            notificationId: Int,
            title: String,
            message: String
        ) {
            createChannel(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (!granted) return
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build()
            android.util.Log.d("NOTIFY_TEST", "show notify id=$notificationId title=$title")
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}