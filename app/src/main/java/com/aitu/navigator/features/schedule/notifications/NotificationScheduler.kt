package com.aitu.navigator.features.schedule.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aitu.navigator.data.datastore.SettingsState
import com.aitu.navigator.data.model.GroupSchedule
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

object NotificationScheduler {
    private const val PREFS = "scheduled_lesson_notifications"
    private const val IDS = "pending_ids"
    private const val GENERATION = "generation"
    private const val LEGACY_CLEANED = "legacy_cleaned"
    private const val ACTION = "com.aitu.navigator.LESSON_REMINDER"

    @Synchronized
    fun currentGeneration(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(GENERATION, null)

    @Synchronized
    fun rescheduleIfUnchanged(context: Context, group: GroupSchedule?, settings: SettingsState, expectedGeneration: String?): Boolean {
        // A user may cancel while an older refresh is reading/parsing the schedule.
        if (currentGeneration(context) != expectedGeneration) return false
        rescheduleForGroup(context, group, settings)
        return true
    }

    @Synchronized
    fun cancelAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(IDS, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }
        // Persist invalidation first: a broadcast already queued by Android must also be ignored.
        check(prefs.edit().remove(IDS).putString(GENERATION, UUID.randomUUID().toString()).commit())
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ids.forEach { cancelExisting(context, manager, it, ACTION) }
        if (!prefs.getBoolean(LEGACY_CLEANED, false)) {
            // Previous versions did not track alarms. NO_CREATE never allocates new PendingIntents.
            (1..512).forEach { cancelExisting(context, manager, it, null) }
            prefs.edit().putBoolean(LEGACY_CLEANED, true).apply()
        }
    }

    private fun cancelExisting(context: Context, manager: AlarmManager, id: Int, action: String?) {
        val intent = Intent(context, LessonNotificationReceiver::class.java).apply { this.action = action }
        PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?.let { pending ->
                manager.cancel(pending)
                pending.cancel()
            }
    }

    @Synchronized
    fun rescheduleForGroup(context: Context, group: GroupSchedule?, settings: SettingsState) {
        cancelAll(context)
        if (group == null) return
        val plan = NotificationPlan.build(group, settings, LocalDateTime.now())
        if (plan.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val generation = prefs.getString(GENERATION, null) ?: return
        // Record before creation so an interrupted scheduling pass can be cleaned up on restart.
        check(prefs.edit().putStringSet(IDS, plan.map { it.id.toString() }.toSet()).commit())
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val zone = ZoneId.systemDefault()
        try {
            plan.forEach { item ->
                val intent = Intent(context, LessonNotificationReceiver::class.java).apply {
                    action = ACTION
                    putExtra(LessonNotificationReceiver.EXTRA_TITLE, item.title)
                    putExtra(LessonNotificationReceiver.EXTRA_MESSAGE, item.message)
                    putExtra(LessonNotificationReceiver.EXTRA_NOTIFICATION_ID, item.id)
                    putExtra(LessonNotificationReceiver.EXTRA_SOUND_ENABLED, item.soundEnabled)
                    putExtra(LessonNotificationReceiver.EXTRA_GENERATION, generation)
                }
                val pending = PendingIntent.getBroadcast(
                    context, item.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.notifyAt.atZone(zone).toInstant().toEpochMilli(), pending)
            }
        } catch (error: Exception) {
            cancelAll(context)
            throw error
        }
    }

    @Synchronized
    fun deliverIfCurrent(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val generation = intent.getStringExtra(LessonNotificationReceiver.EXTRA_GENERATION) ?: return
        val id = intent.getIntExtra(LessonNotificationReceiver.EXTRA_NOTIFICATION_ID, 0)
        val ids = prefs.getStringSet(IDS, emptySet()).orEmpty()
        if (generation != prefs.getString(GENERATION, null) || id.toString() !in ids) return
        prefs.edit().putStringSet(IDS, ids - id.toString()).apply()
        NotificationUtils.showLessonNotification(
            context, id,
            intent.getStringExtra(LessonNotificationReceiver.EXTRA_TITLE) ?: "Lesson soon",
            intent.getStringExtra(LessonNotificationReceiver.EXTRA_MESSAGE).orEmpty(),
            intent.getBooleanExtra(LessonNotificationReceiver.EXTRA_SOUND_ENABLED, true)
        )
    }

    fun countPlannedForNext7Days(group: GroupSchedule, settings: SettingsState, now: LocalDateTime = LocalDateTime.now()): Int =
        NotificationPlan.build(group, settings, now).size
}
