package com.aitu.navigator.features.schedule.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** WorkManager performs the disk/JSON work outside the broadcast receiver's short lifetime. */
class NotificationRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) {
            try {
                // RTC alarms from the old clock/zone must not fire while the worker is queued.
                NotificationScheduler.cancelAll(context)
            } catch (error: Exception) {
                android.util.Log.w("LessonReminders", "Unable to invalidate old reminders", error)
            }
            NotificationRefreshWorker.requestRefresh(context)
        }
    }
}
