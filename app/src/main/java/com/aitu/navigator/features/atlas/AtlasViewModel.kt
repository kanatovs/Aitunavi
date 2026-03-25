package com.aitu.navigator.features.atlas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitu.navigator.data.repository.AtlasRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AtlasResult(
    val title: String,
    val subtitle: String,
    val roomOrPoi: String,
    val building: String?,
    val floor: Int?
)

class AtlasViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AtlasRepository(app.applicationContext)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<AtlasResult>>(emptyList())
    val results: StateFlow<List<AtlasResult>> = _results

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history

    private val _selectedBuilding = MutableStateFlow("C1") // 3 кнопки корпусов
    val selectedBuilding: StateFlow<String> = _selectedBuilding

    private val _selectedFloor = MutableStateFlow(1) // 1/2/3
    val selectedFloor: StateFlow<Int> = _selectedFloor

    private val _mapUrl = MutableStateFlow(DEFAULT_MAP_URL)
    val mapUrl: StateFlow<String> = _mapUrl

    init {
        viewModelScope.launch {
            AtlasNavigationBridge.pendingRoom.collect { room ->
                if (room.isNullOrBlank()) return@collect
                _query.value = room
                search()
                _results.value.firstOrNull()?.let { pickResult(it) }
                AtlasNavigationBridge.consume()
            }
        }
    }

    fun setQuery(text: String) { _query.value = text }

    fun search() {
        val q = _query.value.trim()
        if (q.isBlank()) {
            _results.value = emptyList()
            return
        }
        _results.value = repo.search(q)

        // история (без дублей)
        if (!_history.value.contains(q)) {
            _history.value = (listOf(q) + _history.value).take(20)
        }
    }

    fun pickResult(r: AtlasResult) {
        // при выборе результата — выставляем корпус/этаж и чистим результаты
        r.building?.let { _selectedBuilding.value = it }
        r.floor?.let { _selectedFloor.value = it }
        _query.value = r.roomOrPoi
        _results.value = emptyList()
    }

    fun setBuilding(b: String) { _selectedBuilding.value = b }
    fun setFloor(f: Int) { _selectedFloor.value = f }

    private val _reloadTick = MutableStateFlow(0)
    val reloadTick: StateFlow<Int> = _reloadTick

    fun refreshMap() { _reloadTick.value += 1 }

    fun resetToDefaultUrl() {
        _mapUrl.value = DEFAULT_MAP_URL
    }

    companion object {
        const val DEFAULT_MAP_URL = "https://yuujiso.github.io/aitumap/"
    }
}