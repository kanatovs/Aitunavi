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
    private val NOTE_IMAGES_PREFIX = "note_images_" // note_images_2026-03-03

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

    fun noteImagesFlow(dateIso: String): Flow<List<String>> {
        val key = stringPreferencesKey(NOTE_IMAGES_PREFIX + dateIso)
        return context.dataStore.data.map { prefs ->
            prefs[key]
                .orEmpty()
                .split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }

    suspend fun setNoteImages(dateIso: String, imagePaths: List<String>) {
        val key = stringPreferencesKey(NOTE_IMAGES_PREFIX + dateIso)
        context.dataStore.edit { prefs ->
            if (imagePaths.isEmpty()) {
                prefs.remove(key)
            } else {
                prefs[key] = imagePaths.joinToString("|")
            }
        }
    }

    suspend fun clearNote(dateIso: String) {
        val noteKey = stringPreferencesKey(NOTE_PREFIX + dateIso)
        val imagesKey = stringPreferencesKey(NOTE_IMAGES_PREFIX + dateIso)
        context.dataStore.edit { prefs ->
            prefs.remove(noteKey)
            prefs.remove(imagesKey)
        }
    }
}