package com.aitu.navigator.features.schedule.notifications

import android.content.Context
import com.aitu.navigator.data.datastore.AppPrefs
import com.aitu.navigator.data.datastore.SettingsPrefs
import com.aitu.navigator.data.repository.MikoRepository
import com.aitu.navigator.data.model.GroupSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes refreshes and reads persisted state instead of a possibly stale UI StateFlow. */
object NotificationCoordinator {
    private val refreshMutex = Mutex()
    private var cachedGroups: List<GroupSchedule>? = null

    suspend fun refreshFromPreferences(context: Context) = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            val appContext = context.applicationContext
            val expectedGeneration = NotificationScheduler.currentGeneration(appContext)
            val settingsPrefs = SettingsPrefs(appContext)
            val appPrefs = AppPrefs(appContext)
            val initialSettings = settingsPrefs.settingsFlow.first()
            val initialGroup = appPrefs.activeGroup.first()?.trim()
            if (!initialSettings.notificationsEnabled || initialGroup.isNullOrBlank()) {
                NotificationScheduler.cancelAll(appContext)
                return@withLock
            }
            val groups = cachedGroups ?: MikoRepository(appContext).loadGroups().getOrThrow().also { cachedGroups = it }
            // Cold JSON loading may take time. Do not apply the snapshot read before it.
            val settings = settingsPrefs.settingsFlow.first()
            val groupName = appPrefs.activeGroup.first()?.trim()
            val group = groups.firstOrNull { it.group_name.equals(groupName, true) }
            NotificationScheduler.rescheduleIfUnchanged(appContext, group, settings, expectedGeneration)
        }
    }
}
