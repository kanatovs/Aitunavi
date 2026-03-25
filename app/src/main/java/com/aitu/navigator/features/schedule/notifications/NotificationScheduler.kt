package com.aitu.navigator.features.schedule.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aitu.navigator.data.datastore.SettingsState
import com.aitu.navigator.data.model.GroupSchedule
import com.aitu.navigator.data.model.Lesson
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object NotificationScheduler {

    fun cancelAll(context: Context) {
        // Временный безопасный вариант:
        // ничего массово не создаём и не отменяем,
        // чтобы не словить Too many PendingIntent.
    }

    fun rescheduleForGroup(
        context: Context,
        group: GroupSchedule,
        settings: SettingsState
    ) {
        cancelAll(context)

        if (!settings.notificationsEnabled) return

        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val upcomingLessons = buildUpcomingLessonsForNext7Days(group.schedule)

        var notificationId = 1

        for (lessonWithDate in upcomingLessons) {
            val lesson = lessonWithDate.lesson
            val lessonDate = lessonWithDate.date

            if (!shouldNotifyLesson(lesson, settings)) continue

            val startTime = parseStartTime(lesson.time) ?: continue
            val lessonStart = LocalDateTime.of(lessonDate, startTime)
            val notifyAt = lessonStart.minusMinutes(settings.leadMinutes.toLong())

            if (notifyAt.isBefore(now)) continue
            if (isInQuietHours(notifyAt.toLocalTime(), settings)) continue

            val title = when (settings.notificationFormat) {
                "TitleOnly" -> "Lesson soon"
                else -> "${lesson.discipline} in ${settings.leadMinutes} min"
            }

            val message = if (settings.notificationFormat == "TitleOnly") {
                lesson.discipline
            } else {
                buildString {
                    append("Time: ${lesson.time}")
                    if (settings.showTypeInFull) append(" • ${lesson.type}")
                    append(" • ${lesson.classroom}")
                }
            }

            val intent = Intent(context, LessonNotificationReceiver::class.java).apply {
                putExtra(LessonNotificationReceiver.EXTRA_TITLE, title)
                putExtra(LessonNotificationReceiver.EXTRA_MESSAGE, message)
                putExtra(LessonNotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMillis = notifyAt.atZone(zone).toInstant().toEpochMilli()

            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )

            notificationId++
        }
    }

    fun countPlannedForNext7Days(
        group: GroupSchedule,
        settings: SettingsState,
        now: LocalDateTime = LocalDateTime.now()
    ): Int {
        if (!settings.notificationsEnabled) return 0

        return buildUpcomingLessonsForNext7Days(group.schedule).count { item ->
            val lesson = item.lesson
            if (!shouldNotifyLesson(lesson, settings)) return@count false

            val startTime = parseStartTime(lesson.time) ?: return@count false
            val lessonStart = LocalDateTime.of(item.date, startTime)
            val notifyAt = lessonStart.minusMinutes(settings.leadMinutes.toLong())

            !notifyAt.isBefore(now) && !isInQuietHours(notifyAt.toLocalTime(), settings)
        }
    }

    private fun shouldNotifyLesson(
        lesson: Lesson,
        settings: SettingsState
    ): Boolean {
        val isOnline = lesson.classroom.contains("online", ignoreCase = true)
        val isLecture = lesson.type.contains("lecture", ignoreCase = true)

        if (!settings.includeOnline && isOnline) return false
        if (!settings.includeLecture && isLecture) return false

        return true
    }

    private fun parseStartTime(timeRange: String): LocalTime? {
        return try {
            val parts = timeRange.replace(" ", "").split("-")
            if (parts.size != 2) return null
            LocalTime.parse(parts[0])
        } catch (_: Exception) {
            null
        }
    }

    private fun isInQuietHours(
        time: LocalTime,
        settings: SettingsState
    ): Boolean {
        if (!settings.quietHoursEnabled) return false

        val start = LocalTime.of(settings.quietStartHour, settings.quietStartMinute)
        val end = LocalTime.of(settings.quietEndHour, settings.quietEndMinute)

        return if (start <= end) {
            !time.isBefore(start) && !time.isAfter(end)
        } else {
            !time.isBefore(start) || !time.isAfter(end)
        }
    }

    private fun buildUpcomingLessonsForNext7Days(
        lessons: List<Lesson>
    ): List<LessonWithDate> {
        val today = LocalDate.now()
        val result = mutableListOf<LessonWithDate>()

        for (offset in 0..6) {
            val date = today.plusDays(offset.toLong())
            val dayName = date.dayOfWeek.toEnglishDayName()

            lessons
                .filter { it.day.equals(dayName, ignoreCase = true) }
                .forEach { lesson ->
                    result += LessonWithDate(date, lesson)
                }
        }

        return result
    }

    private fun DayOfWeek.toEnglishDayName(): String {
        return when (this) {
            DayOfWeek.MONDAY -> "Monday"
            DayOfWeek.TUESDAY -> "Tuesday"
            DayOfWeek.WEDNESDAY -> "Wednesday"
            DayOfWeek.THURSDAY -> "Thursday"
            DayOfWeek.FRIDAY -> "Friday"
            DayOfWeek.SATURDAY -> "Saturday"
            DayOfWeek.SUNDAY -> "Sunday"
        }
    }

    private data class LessonWithDate(
        val date: LocalDate,
        val lesson: Lesson
    )
}