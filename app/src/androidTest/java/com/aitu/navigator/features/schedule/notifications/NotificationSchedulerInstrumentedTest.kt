package com.aitu.navigator.features.schedule.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.work.WorkManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aitu.navigator.data.datastore.SettingsState
import com.aitu.navigator.data.model.GroupSchedule
import com.aitu.navigator.data.model.Lesson
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class NotificationSchedulerInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val enabled = SettingsState(notificationsEnabled = true)
    private val group get() = GroupSchedule("TEST", listOf(
        Lesson(LocalDate.now().plusDays(1).dayOfWeek.name, "12:00-13:00", "Test lesson", "101", "Practice", "Teacher")
    ))
    private val stored get() = context.getSharedPreferences("scheduled_lesson_notifications", Context.MODE_PRIVATE)

    @Before fun reset() {
        // APK updates can enqueue a real refresh; isolate the synthetic test schedule from it.
        WorkManager.getInstance(context).cancelAllWork().result.get(10, TimeUnit.SECONDS)
        NotificationScheduler.cancelAll(context)
        manager.cancelAll()
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
    }

    @After fun cleanUp() {
        NotificationScheduler.cancelAll(context)
        manager.cancelAll()
    }

    private fun pending() = PendingIntent.getBroadcast(
        context, 1, Intent(context, LessonNotificationReceiver::class.java).apply {
            action = "com.aitu.navigator.LESSON_REMINDER"
        }, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )

    private fun awaitNotificationCount(count: Int) {
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (manager.activeNotifications.size != count && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(50)
        }
        assertEquals(count, manager.activeNotifications.size)
    }

    private fun delivery(generation: String? = stored.getString("generation", null), silent: Boolean = false) =
        Intent(context, LessonNotificationReceiver::class.java).apply {
            putExtra(LessonNotificationReceiver.EXTRA_GENERATION, generation)
            putExtra(LessonNotificationReceiver.EXTRA_NOTIFICATION_ID, 1)
            putExtra(LessonNotificationReceiver.EXTRA_TITLE, "Test lesson")
            putExtra(LessonNotificationReceiver.EXTRA_MESSAGE, "Reminder")
            putExtra(LessonNotificationReceiver.EXTRA_SOUND_ENABLED, !silent)
        }

    @Test fun disablingNotificationsActuallyCancelsPendingIntent() {
        NotificationScheduler.rescheduleForGroup(context, group, enabled)
        assertNotNull(pending())
        NotificationScheduler.rescheduleForGroup(context, group, enabled.copy(notificationsEnabled = false))
        assertNull(pending())
        assertTrue(stored.getStringSet("pending_ids", emptySet()).orEmpty().isEmpty())
    }

    @Test fun clearingGroupActuallyCancelsPendingIntent() {
        NotificationScheduler.rescheduleForGroup(context, group, enabled)
        NotificationScheduler.rescheduleForGroup(context, null, enabled)
        assertNull(pending())
    }

    @Test fun cancelledRefreshCannotRearmItsOldSnapshot() {
        NotificationScheduler.rescheduleForGroup(context, group, enabled)
        val generationBeforeReading = NotificationScheduler.currentGeneration(context)
        NotificationScheduler.cancelAll(context)
        assertFalse(NotificationScheduler.rescheduleIfUnchanged(context, group, enabled, generationBeforeReading))
        assertNull(pending())
    }

    @Test fun queuedOldBroadcastCannotDisplayAfterRescheduling() {
        NotificationScheduler.rescheduleForGroup(context, group, enabled)
        val obsolete = delivery()
        NotificationScheduler.rescheduleForGroup(context, group.copy(group_name = "NEW"), enabled)
        LessonNotificationReceiver().onReceive(context, obsolete)
        assertTrue(manager.activeNotifications.isEmpty())
        assertEquals(setOf("1"), stored.getStringSet("pending_ids", emptySet()))
    }

    @Test fun receiverPostsNotificationAndConsumesDeliveryOnlyOnce() {
        NotificationScheduler.rescheduleForGroup(context, group, enabled)
        val intent = delivery(silent = true)
        LessonNotificationReceiver().onReceive(context, intent)
        awaitNotificationCount(1)
        val notification = manager.activeNotifications.single().notification
        assertEquals("Test lesson", notification.extras.getString("android.title"))
        assertNull(manager.getNotificationChannel(notification.channelId).sound)
        assertTrue(stored.getStringSet("pending_ids", emptySet()).orEmpty().isEmpty())
        manager.cancelAll()
        awaitNotificationCount(0)
        LessonNotificationReceiver().onReceive(context, intent)
        assertTrue(manager.activeNotifications.isEmpty())
    }

    @Test fun legacyBroadcastWithoutGenerationIsIgnored() {
        NotificationScheduler.rescheduleForGroup(context, group, enabled)
        LessonNotificationReceiver().onReceive(context, delivery(generation = null))
        assertTrue(manager.activeNotifications.isEmpty())
    }
}
