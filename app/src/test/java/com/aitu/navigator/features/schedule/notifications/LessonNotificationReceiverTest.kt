package com.aitu.navigator.features.schedule.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

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
    }

    @Test
    fun onReceivePublishesTheNotificationFromIntentExtras() {
        val intent = Intent(application, LessonNotificationReceiver::class.java)
            .putExtra(LessonNotificationReceiver.EXTRA_NOTIFICATION_ID, 73)
            .putExtra(LessonNotificationReceiver.EXTRA_TITLE, "Physics")
            .putExtra(LessonNotificationReceiver.EXTRA_MESSAGE, "Your lesson starts in 5 minutes.")

        LessonNotificationReceiver().onReceive(application, intent)

        val notification = requireNotNull(shadowOf(manager).getNotification(73)) {
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
    fun onReceivePublishesDefaultsWhenIntentExtrasAreMissing() {
        LessonNotificationReceiver().onReceive(application, Intent())

        val notification = requireNotNull(shadowOf(manager).getNotification(0)) {
            "The receiver did not publish its default notification"
        }
        assertEquals(1, shadowOf(manager).allNotifications.size)
        assertEquals("Upcoming lesson", notification.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertEquals(
            "Your lesson starts soon.",
            notification.extras.getCharSequence(Notification.EXTRA_TEXT)
        )
    }
}
