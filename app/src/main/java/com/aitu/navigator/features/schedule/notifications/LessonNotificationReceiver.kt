package com.aitu.navigator.features.schedule.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LessonNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Upcoming lesson"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Your lesson starts soon."
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        NotificationUtils.showLessonNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            message = message
        )
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}