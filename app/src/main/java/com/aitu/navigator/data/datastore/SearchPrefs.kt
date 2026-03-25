package com.aitu.navigator.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.searchDataStore by preferencesDataStore(name = "search_prefs")
private val KEY_SEARCH_HISTORY = stringPreferencesKey("search_history")
class SearchPrefs(private val context: Context) {

    private val KEY_PINNED_TEACHERS = stringPreferencesKey("pinned_teachers")

    val pinnedTeachersFlow: Flow<Set<String>> =
        context.searchDataStore.data.map { prefs ->
            prefs[KEY_PINNED_TEACHERS]
                .orEmpty()
                .split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
        }
    val historyFlow: Flow<List<String>> =
        context.searchDataStore.data.map { prefs ->
            prefs[KEY_SEARCH_HISTORY]
                .orEmpty()
                .split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
    suspend fun setPinnedTeachers(keys: Set<String>) {
        context.searchDataStore.edit { prefs ->
            prefs[KEY_PINNED_TEACHERS] = keys.joinToString("|")
        }
    }
    suspend fun setHistory(history: List<String>) {
        context.searchDataStore.edit { prefs ->
            prefs[KEY_SEARCH_HISTORY] = history.distinct().joinToString("|")
        }
    }
}