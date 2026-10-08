package com.aitu.navigator.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitu.navigator.data.datastore.AppPrefs
import com.aitu.navigator.data.datastore.SettingsPrefs
import com.aitu.navigator.data.datastore.SettingsState
import com.aitu.navigator.data.repository.MikoRepository
import com.aitu.navigator.data.model.GroupSchedule
import com.aitu.navigator.features.schedule.notifications.NotificationScheduler
import com.aitu.navigator.features.schedule.notifications.NotificationRefreshWorker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = SettingsPrefs(app.applicationContext)
    private val appPrefs = AppPrefs(app.applicationContext)
    private val mikoRepo = MikoRepository(app.applicationContext)
    private var cachedGroups: List<GroupSchedule> = emptyList()

    val settings: StateFlow<SettingsState> = prefs.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsState()
    )

    private val _plannedNotificationsCount = MutableStateFlow(0)
    val plannedNotificationsCount: StateFlow<Int> = _plannedNotificationsCount

    init {
        cachedGroups = mikoRepo.loadGroups().getOrDefault(emptyList())
        observePlannedNotificationsCount()
    }

    fun setLanguage(value: String) = viewModelScope.launch {
        prefs.setLanguage(value)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(value)
        )
    }
    fun setTheme(value: String) = viewModelScope.launch { prefs.setTheme(value) }
    fun setFxMaster(value: Float) = viewModelScope.launch { prefs.setFxMaster(value) }
    fun setFxGlow(value: Float) = viewModelScope.launch { prefs.setFxGlow(value) }
    fun setFxGlass(value: Float) = viewModelScope.launch { prefs.setFxGlass(value) }
    fun setPerformance(value: String) = viewModelScope.launch { prefs.setPerformance(value) }
    fun setFps(value: String) = viewModelScope.launch { prefs.setFps(value) }

    fun setNotificationsEnabled(value: Boolean) =
        viewModelScope.launch {
            prefs.setNotificationsEnabled(value)
            if (!value) NotificationScheduler.cancelAll(getApplication<Application>().applicationContext)
            requestNotificationRefresh()
        }

    fun setLeadMinutes(value: Int) =
        viewModelScope.launch {
            prefs.setLeadMinutes(value)
            requestNotificationRefresh()
        }

    fun setNotificationFormat(value: String) =
        viewModelScope.launch {
            prefs.setNotificationFormat(value)
            requestNotificationRefresh()
        }

    fun setSoundEnabled(value: Boolean) =
        viewModelScope.launch {
            prefs.setSoundEnabled(value)
            requestNotificationRefresh()
        }

    fun setIncludeOnline(value: Boolean) =
        viewModelScope.launch {
            prefs.setIncludeOnline(value)
            requestNotificationRefresh()
        }

    fun setIncludeLecture(value: Boolean) =
        viewModelScope.launch {
            prefs.setIncludeLecture(value)
            requestNotificationRefresh()
        }

    fun setShowTypeInFull(value: Boolean) =
        viewModelScope.launch {
            prefs.setShowTypeInFull(value)
            requestNotificationRefresh()
        }

    fun setQuietHoursEnabled(value: Boolean) =
        viewModelScope.launch {
            prefs.setQuietHoursEnabled(value)
            requestNotificationRefresh()
        }

    fun setQuietStart(hour: Int, minute: Int) =
        viewModelScope.launch {
            prefs.setQuietStart(hour, minute)
            requestNotificationRefresh()
        }

    fun setQuietEnd(hour: Int, minute: Int) =
        viewModelScope.launch {
            prefs.setQuietEnd(hour, minute)
            requestNotificationRefresh()
        }

    private fun requestNotificationRefresh() {
        NotificationRefreshWorker.requestRefresh(getApplication<Application>().applicationContext)
    }

    private fun observePlannedNotificationsCount() {
        viewModelScope.launch {
            combine(prefs.settingsFlow, appPrefs.activeGroup) { settingsState, activeGroup ->
                settingsState to activeGroup
            }.collect { (settingsState, activeGroupRaw) ->
                val activeGroup = activeGroupRaw?.trim().orEmpty()
                if (activeGroup.isBlank()) {
                    _plannedNotificationsCount.value = 0
                    return@collect
                }

                val group = cachedGroups.firstOrNull {
                    it.group_name.equals(activeGroup, ignoreCase = true)
                }

                _plannedNotificationsCount.value = if (group == null) {
                    0
                } else {
                    NotificationScheduler.countPlannedForNext7Days(group, settingsState)
                }
            }
        }
    }

}
