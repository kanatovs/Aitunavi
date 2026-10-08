package com.aitu.navigator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aitu.navigator.core.navigation.AppNav
import com.aitu.navigator.core.navigation.bottomScreens
import com.aitu.navigator.features.schedule.notifications.NotificationUtils
import com.aitu.navigator.ui.theme.AITUStudentNavigatorTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitu.navigator.features.settings.AppSettingsViewModel
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.aitu.navigator.data.datastore.SettingsPrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.aitu.navigator.data.datastore.AppPrefs
import com.aitu.navigator.features.schedule.notifications.NotificationCoordinator
import com.aitu.navigator.features.schedule.notifications.NotificationRefreshWorker
import com.aitu.navigator.features.schedule.notifications.NotificationPlan
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationUtils.createChannel(this)
        requestNotificationPermissionIfNeeded()
        NotificationRefreshWorker.ensurePeriodicRefresh(applicationContext)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    SettingsPrefs(applicationContext).settingsFlow,
                    AppPrefs(applicationContext).activeGroup
                ) { settings, group -> NotificationPlan.settingsKey(settings) to group }
                    .distinctUntilChanged().collect {
                    try {
                        NotificationCoordinator.refreshFromPreferences(applicationContext)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        android.util.Log.w("LessonReminders", "Unable to refresh lesson reminders", error)
                        NotificationRefreshWorker.requestRefresh(applicationContext)
                    }
                }
            }
        }
        lifecycleScope.launch {
            val settings = SettingsPrefs(applicationContext).settingsFlow.first()

            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(settings.language)
            )
        }
        setContent {
            val appSettingsVm: AppSettingsViewModel = viewModel()
            val settings by appSettingsVm.settings.collectAsState()

            AITUStudentNavigatorTheme(
                themeName = settings.theme
            ) {
                AppRoot()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomScreens.forEach { screen ->
                        val selected = currentRoute == screen.route

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = stringResource(screen.titleRes),
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )

                            Text(
                                text = stringResource(screen.titleRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            AppNav(navController)
        }
    }
}
