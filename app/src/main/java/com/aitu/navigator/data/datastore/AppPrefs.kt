package com.aitu.navigator.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "prefs")

class AppPrefs(private val context: Context) {

    private val KEY_GROUP = stringPreferencesKey("active_group")
    private val NOTE_PREFIX = "note_" // note_2026-03-03

    val activeGroup: Flow<String?> = context.dataStore.data.map { it[KEY_GROUP] }

    suspend fun setActiveGroup(group: String) {
        context.dataStore.edit { it[KEY_GROUP] = group }
    }

    suspend fun clearActiveGroup() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_GROUP)
        }
    }

    // ✅ Notes (по дате ISO: "2026-03-03")
    fun noteFlow(dateIso: String): Flow<String> {
        val key = stringPreferencesKey(NOTE_PREFIX + dateIso)
        return context.dataStore.data.map { it[key].orEmpty() }
    }

    suspend fun setNote(dateIso: String, text: String) {
        val key = stringPreferencesKey(NOTE_PREFIX + dateIso)
        context.dataStore.edit { prefs -> prefs[key] = text }
    }
}