package com.aitu.navigator.features.schedule.notifications

import com.aitu.navigator.data.datastore.SettingsState
import com.aitu.navigator.data.model.GroupSchedule
import com.aitu.navigator.data.model.Lesson
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class NotificationPlanTest {
    private val now = LocalDateTime.of(2026, 10, 5, 8, 0) // Monday, deliberately independent of the host date.
    private val enabled = SettingsState(notificationsEnabled = true, leadMinutes = 15)

    private fun lesson(day: String = "Monday", time: String = "09:00-10:00", room: String = "C1.101",
                       type: String = "Practice", name: String = "Algorithms") =
        Lesson(day, time, name, room, type, "Teacher")

    private fun plan(vararg lessons: Lesson, settings: SettingsState = enabled, at: LocalDateTime = now) =
        NotificationPlan.build(GroupSchedule("CS-101", lessons.toList()), settings, at)

    @Test fun disabledNotificationsProduceNoAlarms() {
        assertTrue(plan(lesson(), settings = enabled.copy(notificationsEnabled = false)).isEmpty())
    }

    @Test fun cosmeticChangesDoNotInvalidateTheReminderSettingsKey() {
        assertEquals(NotificationPlan.settingsKey(enabled),
            NotificationPlan.settingsKey(enabled.copy(theme = "mono", fxMaster = 1f, fxGlow = 0f, fps = "120")))
        assertNotEquals(NotificationPlan.settingsKey(enabled),
            NotificationPlan.settingsKey(enabled.copy(leadMinutes = 30)))
    }

    @Test fun leadTimeUsesTheInjectedDateAndIncludesToday() {
        val item = plan(lesson()).single()
        assertEquals(now.withHour(8).withMinute(45), item.notifyAt)
        assertEquals(1, item.id)
    }

    @Test fun pastReminderIsSkippedEvenWhenTheLessonHasNotStarted() {
        assertTrue(plan(lesson(time = "08:10-09:00")).isEmpty())
    }

    @Test fun exactlyNowCanBeScheduled() {
        assertEquals(now, plan(lesson(time = "08:15-09:15")).single().notifyAt)
    }

    @Test fun windowIncludesSixthDayAndDoesNotRepeatTodayNextWeek() {
        val result = plan(lesson(), lesson(day = "Sunday"))
        assertEquals(2, result.size)
        assertEquals(now.toLocalDate().plusDays(6), result.last().notifyAt.toLocalDate())
    }

    @Test fun leadTimeCanCrossMidnight() {
        val item = plan(lesson(day = "Tuesday", time = "00:05-01:00")).single()
        assertEquals(LocalDateTime.of(2026, 10, 5, 23, 50), item.notifyAt)
    }

    @Test fun invalidTimeIsSkippedAndDashVariantsAreAccepted() {
        assertEquals(1, plan(lesson(time = "bad"), lesson(time = "25:00-26:00"),
            lesson(time = "09:00 – 10:00")).size)
    }

    @Test fun onlineAndLectureFiltersRecognizeBothLanguages() {
        assertTrue(plan(lesson(room = "ONLINE"), lesson(room = "Онлайн"),
            settings = enabled.copy(includeOnline = false)).isEmpty())
        assertTrue(plan(lesson(type = "Lecture"), lesson(type = "Лекция"),
            settings = enabled.copy(includeLecture = false)).isEmpty())
    }

    @Test fun quietHoursCrossMidnightAndEndIsExclusive() {
        val quiet = enabled.copy(quietHoursEnabled = true)
        assertTrue(NotificationPlan.isInQuietHours(LocalTime.of(23, 0), quiet))
        assertTrue(NotificationPlan.isInQuietHours(LocalTime.of(6, 59), quiet))
        assertFalse(NotificationPlan.isInQuietHours(LocalTime.of(7, 0), quiet))
        assertFalse(NotificationPlan.isInQuietHours(LocalTime.NOON, quiet))
    }

    @Test fun daytimeQuietHoursAndEqualBoundariesAreSupported() {
        val quiet = enabled.copy(quietHoursEnabled = true, quietStartHour = 8, quietEndHour = 10)
        assertTrue(plan(lesson(), settings = quiet).isEmpty())
        assertFalse(NotificationPlan.isInQuietHours(LocalTime.of(10, 0), quiet))
        assertFalse(NotificationPlan.isInQuietHours(LocalTime.of(8, 0), quiet.copy(quietEndHour = 8)))
    }

    @Test fun duplicateAndMultiRoomRowsBecomeOneReminder() {
        val item = plan(lesson(), lesson(), lesson(room = "C1.102")).single()
        assertTrue(item.message.contains("C1.101"))
        assertTrue(item.message.contains("C1.102"))
    }

    @Test fun titleOnlyAndSoundSettingsAreCarriedIntoThePlan() {
        val item = plan(lesson(), settings = enabled.copy(notificationFormat = "TitleOnly", soundEnabled = false)).single()
        assertEquals("Algorithms", item.message)
        assertFalse(item.soundEnabled)
    }

    @Test fun capSelectsTheEarliestRemindersAndIdsAreUnique() {
        val lessons = (0..79).map { lesson(time = "${(9 + it / 60).toString().padStart(2, '0')}:${(it % 60).toString().padStart(2, '0')}-12:00", name = "Subject $it") }
        val result = plan(*lessons.reversed().toTypedArray())
        assertEquals(NotificationPlan.MAX_ALARMS, result.size)
        assertEquals((1..60).toList(), result.map { it.id })
        assertEquals(result.sortedBy { it.notifyAt }, result)
        assertEquals(LocalTime.of(9, 44), result.last().notifyAt.toLocalTime())
    }
}
