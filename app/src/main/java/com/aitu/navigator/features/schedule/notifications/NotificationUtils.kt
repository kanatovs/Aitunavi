package com.aitu.navigator.features.schedule.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationUtils {
    const val CHANNEL_ID = "schedule_channel"
    private const val SILENT_CHANNEL_ID = "schedule_channel_silent"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Schedule notifications", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Notifications about upcoming lessons"
        })
        manager.createNotificationChannel(NotificationChannel(SILENT_CHANNEL_ID, "Silent schedule notifications", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Lesson reminders without sound or vibration"
            setSound(null, null)
            enableVibration(false)
        })
    }

    fun showLessonNotification(context: Context, notificationId: Int, title: String, message: String, soundEnabled: Boolean = true) {
        createChannel(context)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, if (soundEnabled) CHANNEL_ID else SILENT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(!soundEnabled)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
