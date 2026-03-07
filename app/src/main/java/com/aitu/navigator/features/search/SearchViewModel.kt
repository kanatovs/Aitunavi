package com.aitu.navigator.features.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitu.navigator.data.model.GroupSchedule
import com.aitu.navigator.data.repository.MikoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    data object Loading : SearchUiState()
    data class Error(val message: String) : SearchUiState()
    data class Ready(
        val query: String = "",
        val allTeachers: List<TeacherEntry> = emptyList(),
        val filteredTeachers: List<TeacherEntry> = emptyList(),
        val history: List<String> = emptyList(),
        val selectedTeacher: TeacherEntry? = null
    ) : SearchUiState()
}

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MikoRepository(app.applicationContext)

    private val _ui = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val ui: StateFlow<SearchUiState> = _ui

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _ui.value = SearchUiState.Loading

            repo.loadGroups().fold(
                onSuccess = { groups ->
                    val teachers = buildTeacherIndex(groups)
                    _ui.value = SearchUiState.Ready(
                        allTeachers = teachers,
                        filteredTeachers = teachers.take(10)
                    )
                },
                onFailure = {
                    _ui.value = SearchUiState.Error("Ошибка загрузки преподавателей: ${it.message}")
                }
            )
        }
    }

    fun updateQuery(text: String) {
        val current = _ui.value as? SearchUiState.Ready ?: return

        val filtered = if (text.isBlank()) {
            current.allTeachers.take(10)
        } else {
            current.allTeachers.filter {
                it.displayName.contains(text, ignoreCase = true) ||
                        it.key.contains(text.lowercase())
            }
        }

        _ui.value = current.copy(
            query = text,
            filteredTeachers = filtered
        )
    }

    fun openTeacher(entry: TeacherEntry) {
        val current = _ui.value as? SearchUiState.Ready ?: return

        val newHistory = listOf(entry.displayName) +
                current.history.filterNot { it.equals(entry.displayName, ignoreCase = true) }

        _ui.value = current.copy(
            selectedTeacher = entry,
            history = newHistory.take(10)
        )
    }

    fun closeTeacher() {
        val current = _ui.value as? SearchUiState.Ready ?: return
        _ui.value = current.copy(selectedTeacher = null)
    }

    fun useHistoryItem(text: String) {
        updateQuery(text)
    }

    fun clearHistory() {
        val current = _ui.value as? SearchUiState.Ready ?: return
        _ui.value = current.copy(history = emptyList())
    }
}