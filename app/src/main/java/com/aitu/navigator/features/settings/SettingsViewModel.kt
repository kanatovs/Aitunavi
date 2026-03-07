package com.aitu.navigator.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitu.navigator.data.datastore.SettingsPrefs
import com.aitu.navigator.data.datastore.SettingsState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = SettingsPrefs(app.applicationContext)

    val settings: StateFlow<SettingsState> = prefs.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsState()
    )

    fun setLanguage(value: String) = viewModelScope.launch { prefs.setLanguage(value) }
    fun setTheme(value: String) = viewModelScope.launch { prefs.setTheme(value) }
    fun setFxMaster(value: Float) = viewModelScope.launch { prefs.setFxMaster(value) }
    fun setFxGlow(value: Float) = viewModelScope.launch { prefs.setFxGlow(value) }
    fun setFxGlass(value: Float) = viewModelScope.launch { prefs.setFxGlass(value) }
    fun setPerformance(value: String) = viewModelScope.launch { prefs.setPerformance(value) }
    fun setFps(value: String) = viewModelScope.launch { prefs.setFps(value) }

    fun setNotificationsEnabled(value: Boolean) =
        viewModelScope.launch { prefs.setNotificationsEnabled(value) }

    fun setLeadMinutes(value: Int) =
        viewModelScope.launch { prefs.setLeadMinutes(value) }

    fun setNotificationFormat(value: String) =
        viewModelScope.launch { prefs.setNotificationFormat(value) }

    fun setSoundEnabled(value: Boolean) =
        viewModelScope.launch { prefs.setSoundEnabled(value) }

    fun setIncludeOnline(value: Boolean) =
        viewModelScope.launch { prefs.setIncludeOnline(value) }

    fun setIncludeLecture(value: Boolean) =
        viewModelScope.launch { prefs.setIncludeLecture(value) }

    fun setShowTypeInFull(value: Boolean) =
        viewModelScope.launch { prefs.setShowTypeInFull(value) }

    fun setQuietHoursEnabled(value: Boolean) =
        viewModelScope.launch { prefs.setQuietHoursEnabled(value) }

    fun setQuietStart(hour: Int, minute: Int) =
        viewModelScope.launch { prefs.setQuietStart(hour, minute) }

    fun setQuietEnd(hour: Int, minute: Int) =
        viewModelScope.launch { prefs.setQuietEnd(hour, minute) }

    // Пока заглушка. Реальный расчёт количества уведомлений сделаем на следующем шаге.
    fun getPlannedNotificationsCount(): Int = if (settings.value.notificationsEnabled) 0 else 0
}