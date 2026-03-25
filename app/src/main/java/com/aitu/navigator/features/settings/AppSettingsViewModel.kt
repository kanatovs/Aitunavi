package com.aitu.navigator.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitu.navigator.data.datastore.SettingsPrefs
import com.aitu.navigator.data.datastore.SettingsState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AppSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = SettingsPrefs(app.applicationContext)

    val settings: StateFlow<SettingsState> = prefs.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
    )
}