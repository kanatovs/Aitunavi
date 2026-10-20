package com.aitu.navigator.features.schedule.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationUtilsTest {

    private lateinit var application: Application
    private lateinit var manager: NotificationManager

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        manager = application.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    @Config(sdk = [26, 32, 33])
    fun showLessonNotificationPublishesContentAndCreatesChannel() {
        val title = "Linear algebra"
        val message = "Your lesson in room 402 starts in 10 minutes."

        NotificationUtils.showLessonNotification(application, 42, title, message)

        val notification = requireNotNull(shadowOf(manager).getNotification(42)) {
            "The lesson notification was not published"
        }
        assertEquals(1, shadowOf(manager).allNotifications.size)
        assertEquals(title, notification.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertEquals(message, notification.extras.getCharSequence(Notification.EXTRA_TEXT))
        assertEquals(message, notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        assertEquals(android.R.drawable.ic_dialog_info, notification.smallIcon.resId)
        assertNotEquals(0, notification.flags and Notification.FLAG_AUTO_CANCEL)
        assertEquals(NotificationUtils.CHANNEL_ID, notification.channelId)

        val channel = manager.getNotificationChannel(NotificationUtils.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals("Schedule notifications", channel.name.toString())
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }

    @Test
    fun showLessonNotificationDoesNotPublishWhenPermissionIsDenied() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        NotificationUtils.showLessonNotification(application, 42, "Lesson", "Starts soon")

        assertEquals(0, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun showLessonNotificationChecksPermissionAgainAfterRevocation() {
        NotificationUtils.showLessonNotification(application, 42, "First lesson", "Starts soon")
        assertNotNull(shadowOf(manager).getNotification(42))
        manager.cancelAll()
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        NotificationUtils.showLessonNotification(application, 43, "Next lesson", "Starts soon")

        assertEquals(0, shadowOf(manager).allNotifications.size)
    }

    @Test
    fun showLessonNotificationUpdatesAnExistingNotificationWithTheSameId() {
        NotificationUtils.showLessonNotification(application, 42, "Lesson", "Starts soon")

        NotificationUtils.showLessonNotification(application, 42, "Updated lesson", "Room changed")

        val notification = requireNotNull(shadowOf(manager).getNotification(42))
        assertEquals(1, shadowOf(manager).allNotifications.size)
        assertEquals("Updated lesson", notification.extras.getCharSequence(Notification.EXTRA_TITLE))
        assertEquals("Room changed", notification.extras.getCharSequence(Notification.EXTRA_TEXT))
    }
}
