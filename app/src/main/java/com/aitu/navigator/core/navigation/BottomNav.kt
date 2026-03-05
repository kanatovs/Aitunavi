package com.aitu.navigator.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomScreen(val route: String, val title: String, val icon: ImageVector) {
    data object Today : BottomScreen("today", "Today", Icons.Filled.Home)
    data object Chronos : BottomScreen("chronos", "Chronos", Icons.Filled.Event)
    data object Atlas : BottomScreen("atlas", "Atlas", Icons.Filled.Map)
    data object Search : BottomScreen("search", "Search", Icons.Filled.Search)
    data object Settings : BottomScreen("settings", "Settings", Icons.Filled.Settings)
}

val bottomScreens = listOf(
    BottomScreen.Today,
    BottomScreen.Chronos,
    BottomScreen.Atlas,
    BottomScreen.Search,
    BottomScreen.Settings
)