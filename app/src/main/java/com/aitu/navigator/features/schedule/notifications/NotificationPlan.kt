package com.aitu.navigator.features.schedule.notifications

import com.aitu.navigator.data.datastore.SettingsState
import com.aitu.navigator.data.model.GroupSchedule
import com.aitu.navigator.features.today.normalizeLessons
import java.time.LocalDateTime
import java.time.LocalTime

data class PlannedNotification(
    val id: Int,
    val notifyAt: LocalDateTime,
    val title: String,
    val message: String,
    val soundEnabled: Boolean
)

/** Pure planning logic. The same plan drives alarms and the count shown in settings. */
object NotificationPlan {
    const val MAX_ALARMS = 60

    fun settingsKey(settings: SettingsState): List<Any> = with(settings) {
        listOf(language, notificationsEnabled, leadMinutes, notificationFormat, soundEnabled,
            includeOnline, includeLecture, showTypeInFull, quietHoursEnabled,
            quietStartHour, quietStartMinute, quietEndHour, quietEndMinute)
    }

    fun build(group: GroupSchedule, settings: SettingsState, now: LocalDateTime): List<PlannedNotification> {
        if (!settings.notificationsEnabled) return emptyList()
        val lessons = group.schedule.filter { lesson ->
            val online = lesson.classroom.contains("online", true) || lesson.classroom.contains("онлайн", true)
            val lecture = lesson.type.contains("lecture", true) || lesson.type.contains("лекц", true)
            (settings.includeOnline || !online) && (settings.includeLecture || !lecture)
        }
        val slots = normalizeLessons(lessons)
        val leadMinutes = settings.leadMinutes.coerceIn(0, 120)
        val result = mutableListOf<PlannedNotification>()
        for (offset in 0..6) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            for (slot in slots.filter { it.day.equals(date.dayOfWeek.name, true) }) {
                val range = slot.time.replace('–', '-').replace('—', '-').split('-')
                if (range.size != 2) continue
                val startTime = runCatching { LocalTime.parse(range[0].trim()) }.getOrNull() ?: continue
                val notifyAt = LocalDateTime.of(date, startTime).minusMinutes(leadMinutes.toLong())
                if (notifyAt.isBefore(now) || isInQuietHours(notifyAt.toLocalTime(), settings)) continue
                val titleOnly = settings.notificationFormat == "TitleOnly"
                val russian = settings.language == "ru"
                val title = if (titleOnly) {
                    if (russian) "Скоро занятие" else "Lesson soon"
                } else {
                    if (russian) "${slot.discipline} через $leadMinutes мин" else "${slot.discipline} in $leadMinutes min"
                }
                val message = if (titleOnly) slot.discipline else buildString {
                    append(if (russian) "Время: " else "Time: ")
                    append(slot.time)
                    if (settings.showTypeInFull) append(" • ${slot.type}")
                    append(" • ${slot.locations.joinToString(" · ") { it.classroom }}")
                }
                result += PlannedNotification(0, notifyAt, title, message, settings.soundEnabled)
            }
        }
        return result.sortedBy { it.notifyAt }.take(MAX_ALARMS).mapIndexed { index, item -> item.copy(id = index + 1) }
    }

    internal fun isInQuietHours(time: LocalTime, settings: SettingsState): Boolean {
        if (!settings.quietHoursEnabled) return false
        val start = LocalTime.of(settings.quietStartHour.coerceIn(0, 23), settings.quietStartMinute.coerceIn(0, 59))
        val end = LocalTime.of(settings.quietEndHour.coerceIn(0, 23), settings.quietEndMinute.coerceIn(0, 59))
        // Equal boundaries mean no quiet interval; otherwise the end is exclusive.
        return when {
            start == end -> false
            start < end -> !time.isBefore(start) && time.isBefore(end)
            else -> !time.isBefore(start) || time.isBefore(end)
        }
    }
}
