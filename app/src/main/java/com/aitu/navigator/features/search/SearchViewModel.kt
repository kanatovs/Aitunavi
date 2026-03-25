package com.aitu.navigator.features.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitu.navigator.data.model.GroupSchedule
import com.aitu.navigator.data.repository.MikoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import com.aitu.navigator.data.datastore.SearchPrefs
import kotlinx.coroutines.flow.combine
sealed class SearchUiState {
    data object Loading : SearchUiState()
    data class Error(val message: String) : SearchUiState()
    data class Ready(
        val query: String = "",
        val allTeachers: List<TeacherEntry> = emptyList(),
        val filteredTeachers: List<TeacherEntry> = emptyList(),
        val history: List<String> = emptyList(),
        val pinnedTeacherKeys: Set<String> = emptySet(),
        val selectedTeacher: TeacherEntry? = null
    ) : SearchUiState()
}

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MikoRepository(app.applicationContext)
    private val searchPrefs = SearchPrefs(app.applicationContext)
    private val _ui = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val ui: StateFlow<SearchUiState> = _ui
    private val _showPinned = MutableStateFlow(false)
    val showPinned = _showPinned.asStateFlow()

    private val _showHistory = MutableStateFlow(false)
    val showHistory = _showHistory.asStateFlow()
    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _ui.value = SearchUiState.Loading

            repo.loadGroups().fold(
                onSuccess = { groups ->
                    val teachers = buildTeacherIndex(groups)

                    combine(
                        searchPrefs.pinnedTeachersFlow,
                        searchPrefs.historyFlow
                    ) { pinnedKeys, history ->

                        val current = _ui.value as? SearchUiState.Ready

                        val query = current?.query.orEmpty()
                        val selectedTeacher = current?.selectedTeacher

                        val filtered = if (query.isBlank()) {
                            teachers
                        } else {
                            teachers.filter {
                                it.displayName.contains(query, ignoreCase = true) ||
                                        it.key.contains(query.lowercase())
                            }
                        }

                        SearchUiState.Ready(
                            query = query,
                            allTeachers = teachers,
                            filteredTeachers = filtered,
                            history = history,
                            pinnedTeacherKeys = pinnedKeys,
                            selectedTeacher = selectedTeacher
                        )
                    }.collect { readyState ->
                        _ui.value = readyState
                    }
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
            current.allTeachers
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
        viewModelScope.launch {
            searchPrefs.setHistory(newHistory)
        }
        _ui.value = current.copy(
            selectedTeacher = entry,
            history = newHistory.take(10)
        )
    }
    fun togglePinned(entry: TeacherEntry) {
        val current = _ui.value as? SearchUiState.Ready ?: return

        val newPinned = current.pinnedTeacherKeys.toMutableSet()

        if (newPinned.contains(entry.key)) {
            newPinned.remove(entry.key)
        } else {
            newPinned.add(entry.key)
        }

        viewModelScope.launch {
            searchPrefs.setPinnedTeachers(newPinned)
        }
    }
    fun getPinnedTeachers(): List<TeacherEntry> {
        val current = _ui.value as? SearchUiState.Ready ?: return emptyList()
        return current.allTeachers.filter { it.key in current.pinnedTeacherKeys }
    }
    fun closeTeacher() {
        val current = _ui.value as? SearchUiState.Ready ?: return
        _ui.value = current.copy(selectedTeacher = null)
    }

    fun useHistoryItem(text: String) {
        updateQuery(text)
    }

    fun clearHistory() {
        viewModelScope.launch {
            searchPrefs.setHistory(emptyList())
        }
    }
    fun openPinned() {
        _showPinned.value = true
    }

    fun closePinned() {
        _showPinned.value = false
    }

    fun openHistory() {
        _showHistory.value = true
    }

    fun closeHistory() {
        _showHistory.value = false
    }
    fun removeHistoryItem(item: String) {
        val current = _ui.value as? SearchUiState.Ready ?: return

        val newHistory = current.history.filter { it != item }

        viewModelScope.launch {
            searchPrefs.setHistory(newHistory)
        }
    }
}
