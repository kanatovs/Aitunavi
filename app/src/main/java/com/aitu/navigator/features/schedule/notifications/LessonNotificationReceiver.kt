package com.aitu.navigator.features.schedule.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LessonNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationScheduler.deliverIfCurrent(context, intent)
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SOUND_ENABLED = "extra_sound_enabled"
        const val EXTRA_GENERATION = "extra_generation"
    }
}
