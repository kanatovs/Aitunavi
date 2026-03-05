package com.aitu.navigator.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aitu.navigator.features.atlas.AtlasScreen
import com.aitu.navigator.features.chronos.ChronosScreen
import com.aitu.navigator.features.search.SearchScreen
import com.aitu.navigator.features.settings.SettingsScreen
import com.aitu.navigator.features.today.TodayScreen

@Composable
fun AppNav(navController: NavHostController) {
    NavHost(navController = navController, startDestination = BottomScreen.Today.route) {
        composable(BottomScreen.Today.route) { TodayScreen() }
        composable(BottomScreen.Chronos.route) { ChronosScreen() }
        composable(BottomScreen.Atlas.route) { AtlasScreen() }
        composable(BottomScreen.Search.route) { SearchScreen() }
        composable(BottomScreen.Settings.route) { SettingsScreen() }
    }
}