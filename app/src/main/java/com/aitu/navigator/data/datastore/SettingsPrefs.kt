package com.aitu.navigator.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class SettingsState(
    val language: String = "ru",
    val theme: String = "classic",
    val fxMaster: Float = 0.5f,
    val fxGlow: Float = 0.5f,
    val fxGlass: Float = 0.5f,
    val performance: String = "Balanced",
    val fps: String = "Auto",

    // Notifications
    val notificationsEnabled: Boolean = false,
    val leadMinutes: Int = 15,
    val notificationFormat: String = "Full", // TitleOnly / Full
    val soundEnabled: Boolean = true,
    val includeOnline: Boolean = true,
    val includeLecture: Boolean = true,
    val showTypeInFull: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 23,
    val quietStartMinute: Int = 0,
    val quietEndHour: Int = 7,
    val quietEndMinute: Int = 0
)

class SettingsPrefs(private val context: Context) {

    private val KEY_LANGUAGE = stringPreferencesKey("language")
    private val KEY_THEME = stringPreferencesKey("theme")
    private val KEY_FX_MASTER = floatPreferencesKey("fx_master")
    private val KEY_FX_GLOW = floatPreferencesKey("fx_glow")
    private val KEY_FX_GLASS = floatPreferencesKey("fx_glass")
    private val KEY_PERFORMANCE = stringPreferencesKey("performance")
    private val KEY_FPS = stringPreferencesKey("fps")

    private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val KEY_LEAD_MINUTES = intPreferencesKey("lead_minutes")
    private val KEY_NOTIFICATION_FORMAT = stringPreferencesKey("notification_format")
    private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    private val KEY_INCLUDE_ONLINE = booleanPreferencesKey("include_online")
    private val KEY_INCLUDE_LECTURE = booleanPreferencesKey("include_lecture")
    private val KEY_SHOW_TYPE_IN_FULL = booleanPreferencesKey("show_type_in_full")
    private val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    private val KEY_QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
    private val KEY_QUIET_START_MINUTE = intPreferencesKey("quiet_start_minute")
    private val KEY_QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
    private val KEY_QUIET_END_MINUTE = intPreferencesKey("quiet_end_minute")

    val settingsFlow: Flow<SettingsState> = context.settingsDataStore.data.map { prefs ->
        SettingsState(
            language = prefs[KEY_LANGUAGE] ?: "ru",
            theme = prefs[KEY_THEME] ?: "classic",
            fxMaster = prefs[KEY_FX_MASTER] ?: 0.5f,
            fxGlow = prefs[KEY_FX_GLOW] ?: 0.5f,
            fxGlass = prefs[KEY_FX_GLASS] ?: 0.5f,
            performance = prefs[KEY_PERFORMANCE] ?: "Balanced",
            fps = prefs[KEY_FPS] ?: "Auto",

            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: false,
            leadMinutes = prefs[KEY_LEAD_MINUTES] ?: 15,
            notificationFormat = prefs[KEY_NOTIFICATION_FORMAT] ?: "Full",
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            includeOnline = prefs[KEY_INCLUDE_ONLINE] ?: true,
            includeLecture = prefs[KEY_INCLUDE_LECTURE] ?: true,
            showTypeInFull = prefs[KEY_SHOW_TYPE_IN_FULL] ?: true,
            quietHoursEnabled = prefs[KEY_QUIET_HOURS_ENABLED] ?: false,
            quietStartHour = prefs[KEY_QUIET_START_HOUR] ?: 23,
            quietStartMinute = prefs[KEY_QUIET_START_MINUTE] ?: 0,
            quietEndHour = prefs[KEY_QUIET_END_HOUR] ?: 7,
            quietEndMinute = prefs[KEY_QUIET_END_MINUTE] ?: 0
        )
    }

    suspend fun setLanguage(value: String) {
        context.settingsDataStore.edit { it[KEY_LANGUAGE] = value }
    }

    suspend fun setTheme(value: String) {
        context.settingsDataStore.edit { it[KEY_THEME] = value }
    }

    suspend fun setFxMaster(value: Float) {
        context.settingsDataStore.edit { it[KEY_FX_MASTER] = value }
    }

    suspend fun setFxGlow(value: Float) {
        context.settingsDataStore.edit { it[KEY_FX_GLOW] = value }
    }

    suspend fun setFxGlass(value: Float) {
        context.settingsDataStore.edit { it[KEY_FX_GLASS] = value }
    }

    suspend fun setPerformance(value: String) {
        context.settingsDataStore.edit { it[KEY_PERFORMANCE] = value }
    }

    suspend fun setFps(value: String) {
        context.settingsDataStore.edit { it[KEY_FPS] = value }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = value }
    }

    suspend fun setLeadMinutes(value: Int) {
        context.settingsDataStore.edit { it[KEY_LEAD_MINUTES] = value }
    }

    suspend fun setNotificationFormat(value: String) {
        context.settingsDataStore.edit { it[KEY_NOTIFICATION_FORMAT] = value }
    }

    suspend fun setSoundEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_SOUND_ENABLED] = value }
    }

    suspend fun setIncludeOnline(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_INCLUDE_ONLINE] = value }
    }

    suspend fun setIncludeLecture(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_INCLUDE_LECTURE] = value }
    }

    suspend fun setShowTypeInFull(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_SHOW_TYPE_IN_FULL] = value }
    }

    suspend fun setQuietHoursEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_QUIET_HOURS_ENABLED] = value }
    }

    suspend fun setQuietStart(hour: Int, minute: Int) {
        context.settingsDataStore.edit {
            it[KEY_QUIET_START_HOUR] = hour
            it[KEY_QUIET_START_MINUTE] = minute
        }
    }

    suspend fun setQuietEnd(hour: Int, minute: Int) {
        context.settingsDataStore.edit {
            it[KEY_QUIET_END_HOUR] = hour
            it[KEY_QUIET_END_MINUTE] = minute
        }
    }
}