package com.aitu.navigator.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.aitu.navigator.R

sealed class BottomScreen(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object Today : BottomScreen("today", R.string.nav_today, Icons.Filled.Home)
    data object Chronos : BottomScreen("chronos", R.string.nav_chronos, Icons.Filled.Event)
    data object Atlas : BottomScreen("atlas", R.string.nav_atlas, Icons.Filled.Map)
    data object Search : BottomScreen("search", R.string.nav_search, Icons.Filled.Search)
    data object Settings : BottomScreen("settings", R.string.nav_settings, Icons.Filled.Settings)
}

val bottomScreens = listOf(
    BottomScreen.Today,
    BottomScreen.Chronos,
    BottomScreen.Atlas,
    BottomScreen.Search,
    BottomScreen.Settings
)