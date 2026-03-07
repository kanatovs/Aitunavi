package com.aitu.navigator.features.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.ColumnScope

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val state by vm.settings.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Настройки", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            SettingsSection("Язык") {
                ChoiceRow(
                    options = listOf("RU", "EN"),
                    selected = state.language,
                    onSelect = vm::setLanguage
                )
            }
        }

        item {
            SettingsSection("Тема") {
                ChoiceRow(
                    options = listOf("System", "Light", "Dark"),
                    selected = state.theme,
                    onSelect = vm::setTheme
                )
            }
        }

        item {
            SettingsSection("FX") {
                Text("Master: ${(state.fxMaster * 100).toInt()}%")
                Slider(
                    value = state.fxMaster,
                    onValueChange = vm::setFxMaster,
                    valueRange = 0f..1f
                )

                Text("Glow: ${(state.fxGlow * 100).toInt()}%")
                Slider(
                    value = state.fxGlow,
                    onValueChange = vm::setFxGlow,
                    valueRange = 0f..1f
                )

                Text("Glass: ${(state.fxGlass * 100).toInt()}%")
                Slider(
                    value = state.fxGlass,
                    onValueChange = vm::setFxGlass,
                    valueRange = 0f..1f
                )
            }
        }

        item {
            SettingsSection("Производительность") {
                ChoiceRow(
                    options = listOf("Auto", "Full", "Balanced", "Saver"),
                    selected = state.performance,
                    onSelect = vm::setPerformance
                )

                Spacer(Modifier.height(12.dp))

                ChoiceRow(
                    options = listOf("Auto", "Prefer 120", "Cap 60"),
                    selected = state.fps,
                    onSelect = vm::setFps
                )
            }
        }

        item {
            SettingsSection("Уведомления") {
                SwitchRow(
                    title = "Включить уведомления",
                    checked = state.notificationsEnabled,
                    onCheckedChange = vm::setNotificationsEnabled
                )

                Spacer(Modifier.height(8.dp))

                Text("Предупреждать за")
                ChoiceRow(
                    options = listOf("5", "10", "15", "30", "45", "60"),
                    selected = state.leadMinutes.toString(),
                    onSelect = { vm.setLeadMinutes(it.toInt()) }
                )

                Spacer(Modifier.height(8.dp))

                Text("Формат")
                ChoiceRow(
                    options = listOf("TitleOnly", "Full"),
                    selected = state.notificationFormat,
                    onSelect = vm::setNotificationFormat
                )

                Spacer(Modifier.height(8.dp))

                SwitchRow(
                    title = "Звук",
                    checked = state.soundEnabled,
                    onCheckedChange = vm::setSoundEnabled
                )

                SwitchRow(
                    title = "Включать online-пары",
                    checked = state.includeOnline,
                    onCheckedChange = vm::setIncludeOnline
                )

                SwitchRow(
                    title = "Включать лекции",
                    checked = state.includeLecture,
                    onCheckedChange = vm::setIncludeLecture
                )

                SwitchRow(
                    title = "Показывать тип пары (в Full)",
                    checked = state.showTypeInFull,
                    onCheckedChange = vm::setShowTypeInFull
                )

                Spacer(Modifier.height(8.dp))

                SwitchRow(
                    title = "Тихие часы",
                    checked = state.quietHoursEnabled,
                    onCheckedChange = vm::setQuietHoursEnabled
                )

                if (state.quietHoursEnabled) {
                    Spacer(Modifier.height(8.dp))

                    Text("Начало тихих часов")
                    TimeChoiceRow(
                        hour = state.quietStartHour,
                        minute = state.quietStartMinute,
                        onPrevHour = {
                            val newHour = if (state.quietStartHour == 0) 23 else state.quietStartHour - 1
                            vm.setQuietStart(newHour, state.quietStartMinute)
                        },
                        onNextHour = {
                            val newHour = if (state.quietStartHour == 23) 0 else state.quietStartHour + 1
                            vm.setQuietStart(newHour, state.quietStartMinute)
                        },
                        onPrevMinute = {
                            val newMinute = if (state.quietStartMinute == 0) 45 else state.quietStartMinute - 15
                            vm.setQuietStart(state.quietStartHour, newMinute)
                        },
                        onNextMinute = {
                            val newMinute = if (state.quietStartMinute == 45) 0 else state.quietStartMinute + 15
                            vm.setQuietStart(state.quietStartHour, newMinute)
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("Конец тихих часов")
                    TimeChoiceRow(
                        hour = state.quietEndHour,
                        minute = state.quietEndMinute,
                        onPrevHour = {
                            val newHour = if (state.quietEndHour == 0) 23 else state.quietEndHour - 1
                            vm.setQuietEnd(newHour, state.quietEndMinute)
                        },
                        onNextHour = {
                            val newHour = if (state.quietEndHour == 23) 0 else state.quietEndHour + 1
                            vm.setQuietEnd(newHour, state.quietEndMinute)
                        },
                        onPrevMinute = {
                            val newMinute = if (state.quietEndMinute == 0) 45 else state.quietEndMinute - 15
                            vm.setQuietEnd(state.quietEndHour, newMinute)
                        },
                        onNextMinute = {
                            val newMinute = if (state.quietEndMinute == 45) 0 else state.quietEndMinute + 15
                            vm.setQuietEnd(state.quietEndHour, newMinute)
                        }
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text("Запланировано уведомлений: ${vm.getPlannedNotificationsCount()}")

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Открыть системные настройки")
                }
            }
        }

        item {
            SettingsSection("Информация") {
                Text("Создатель")
                Text("Sabyr")
                Spacer(Modifier.height(8.dp))
                Text("Партнёры")
                Text("Mikosha")
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun ChoiceRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        label = { Text(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun TimeChoiceRow(
    hour: Int,
    minute: Int,
    onPrevHour: () -> Unit,
    onNextHour: () -> Unit,
    onPrevMinute: () -> Unit,
    onNextMinute: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onPrevHour) { Text("-h") }
            Text(String.format("%02d", hour))
            TextButton(onClick = onNextHour) { Text("+h") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onPrevMinute) { Text("-m") }
            Text(String.format("%02d", minute))
            TextButton(onClick = onNextMinute) { Text("+m") }
        }
    }
}