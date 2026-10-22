package com.aitu.navigator.features.schedule.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.aitu.navigator.data.datastore.SettingsState
import com.aitu.navigator.data.model.GroupSchedule
import com.aitu.navigator.data.model.Lesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LessonNotificationReceiverTest {

    private lateinit var application: Application
    private lateinit var manager: NotificationManager

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        manager = application.getSystemService(NotificationManager::class.java)
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationScheduler.cancelAll(application)
        manager.cancelAll()
    }

    @After
    fun cleanUp() {
        NotificationScheduler.cancelAll(application)
        manager.cancelAll()
    }

    private fun schedule(groupName: String = "TEST"): Intent {
        val group = GroupSchedule(groupName, listOf(
            Lesson(LocalDate.now().plusDays(1).dayOfWeek.name, "12:00-13:00", "Physics", "101", "Practice", "Teacher")
        ))
        NotificationScheduler.rescheduleForGroup(application, group, SettingsState(notificationsEnabled = true))
        return Intent(application, LessonNotificationReceiver::class.java)
            .putExtra(LessonNotificationReceiver.EXTRA_GENERATION, NotificationScheduler.currentGeneration(application))
            .putExtra(LessonNotificationReceiver.EXTRA_NOTIFICATION_ID, 1)
    }

    @Test
    fun onReceivePublishesTheNotificationFromIntentExtras() {
        val intent = schedule()
            .putExtra(LessonNotificationReceiver.EXTRA_TITLE, "Physics")
            .putExtra(LessonNotificationReceiver.EXTRA_MESSAGE, "Your lesson starts in 5 minutes.")

        LessonNotificationReceiver().onReceive(application, intent)

        val notification = requireNotNull(shadowOf(manager).getNotification(1)) {
            "The receiver did not publish the lesson notification"
        }
        assertEquals(1, shadowOf(manager).allNotifications.size)
        assertEquals("Physics", notification.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertEquals(
            "Your lesson starts in 5 minutes.",
            notification.extras.getCharSequence(Notification.EXTRA_TEXT)
        )
    }

    @Test
    fun onReceiveIgnoresLegacyIntentsWithoutGeneration() {
        schedule()
        LessonNotificationReceiver().onReceive(application, Intent())

        assertEquals(0, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun onReceiveConsumesCurrentDeliveryOnlyOnce() {
        val intent = schedule()
        LessonNotificationReceiver().onReceive(application, intent)
        assertEquals(1, shadowOf(manager).allNotifications.size)
        manager.cancelAll()

        LessonNotificationReceiver().onReceive(application, intent)

        assertEquals(0, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun changingGroupRejectsPreviouslyQueuedDelivery() {
        val previous = schedule("OLD")
        val current = schedule("NEW")

        LessonNotificationReceiver().onReceive(application, previous)
        assertEquals(0, shadowOf(manager).allNotifications.size)
        LessonNotificationReceiver().onReceive(application, current)
        assertEquals(1, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun cancellingInvalidatesDeliveryAndOlderRefresh() {
        val intent = schedule()
        val oldGeneration = NotificationScheduler.currentGeneration(application)
        NotificationScheduler.cancelAll(application)

        LessonNotificationReceiver().onReceive(application, intent)

        assertEquals(0, shadowOf(manager).allNotifications.size)
        assertFalse(NotificationScheduler.rescheduleIfUnchanged(
            application, null, SettingsState(notificationsEnabled = true), oldGeneration
        ))
        val stored = application.getSharedPreferences("scheduled_lesson_notifications", Context.MODE_PRIVATE)
        assertNull(stored.getStringSet("pending_ids", null))
    }
}
