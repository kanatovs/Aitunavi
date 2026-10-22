package com.aitu.navigator.features.schedule.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class NotificationRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        NotificationCoordinator.refreshFromPreferences(applicationContext)
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        android.util.Log.w("LessonReminders", "Unable to refresh lesson reminders", error)
        Result.retry()
    }

    companion object {
        fun ensurePeriodicRefresh(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "lesson-reminders-daily-refresh", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<NotificationRefreshWorker>(24, TimeUnit.HOURS).build()
            )
        }

        fun requestRefresh(context: Context) {
            ensurePeriodicRefresh(context)
            WorkManager.getInstance(context).enqueueUniqueWork(
                "lesson-reminders-refresh", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<NotificationRefreshWorker>().build()
            )
        }
    }
}
