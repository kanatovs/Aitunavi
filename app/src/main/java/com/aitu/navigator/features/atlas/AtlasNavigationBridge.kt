package com.aitu.navigator.features.atlas

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AtlasNavigationBridge {
    private val _pendingRoom = MutableStateFlow<String?>(null)
    val pendingRoom: StateFlow<String?> = _pendingRoom.asStateFlow()

    fun openRoom(room: String) {
        _pendingRoom.value = room
    }

    fun consume() {
        _pendingRoom.value = null
    }
}
