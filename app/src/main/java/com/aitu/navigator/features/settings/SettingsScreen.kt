package com.aitu.navigator.features.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitu.navigator.R
import com.aitu.navigator.ui.components.IosSectionCard
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val state by vm.settings.collectAsState()
    val plannedCount by vm.plannedNotificationsCount.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge
            )

        }

        item {
            IosSectionCard(
                title = stringResource(R.string.settings_language),
                subtitle = "Choose interface language."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.language == "ru",
                        onClick = { vm.setLanguage("ru") },
                        label = { Text(stringResource(R.string.lang_ru)) }
                    )
                    FilterChip(
                        selected = state.language == "en",
                        onClick = { vm.setLanguage("en") },
                        label = { Text(stringResource(R.string.lang_en)) }
                    )
                }
            }
        }

        item {
            IosSectionCard(
                title = stringResource(R.string.settings_theme),
                subtitle = "Pick a visual style."
            ) {
                val themeOptions = listOf(
                    ThemeOption("classic", stringResource(R.string.theme_classic)),
                    ThemeOption("violet", stringResource(R.string.theme_violet)),
                    ThemeOption("obsidian", stringResource(R.string.theme_obsidian)),
                    ThemeOption("nebula", stringResource(R.string.theme_nebula)),
                    ThemeOption("rose_neon", stringResource(R.string.theme_rose_neon)),
                    ThemeOption("sunset", stringResource(R.string.theme_sunset)),
                    ThemeOption("mono", stringResource(R.string.theme_mono)),
                    ThemeOption("cyber_mint", stringResource(R.string.theme_cyber_mint)),
                    ThemeOption("deep_space", stringResource(R.string.theme_deep_space)),
                    ThemeOption("silver", stringResource(R.string.theme_silver)),
                    ThemeOption("gray", stringResource(R.string.theme_gray)),
                    ThemeOption("black", stringResource(R.string.theme_black))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themeOptions.forEach { option ->
                        FilterChip(
                            selected = state.theme == option.id,
                            onClick = { vm.setTheme(option.id) },
                            label = { Text(option.label) }
                        )
                    }
                }
            }
        }

        item {
            IosSectionCard(
                title = stringResource(R.string.settings_fx),
                subtitle = "Настройте интенсивность визуальных эффектов."
            ) {
                SmallSliderBlock(
                    title = "Master",
                    value = state.fxMaster,
                    onValueChange = vm::setFxMaster
                )

                SmallSliderBlock(
                    title = "Glow",
                    value = state.fxGlow,
                    onValueChange = vm::setFxGlow
                )

                SmallSliderBlock(
                    title = "Glass",
                    value = state.fxGlass,
                    onValueChange = vm::setFxGlass
                )
            }
        }

        item {
            IosSectionCard(
                title = stringResource(R.string.settings_performance),
                subtitle = "Выберите баланс между красотой и скоростью."
            ) {

            Text(
                    text = "Режим",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(6.dp))

                CompactChoiceRow(
                    options = listOf("Auto", "Full", "Balanced", "Saver"),
                    selected = state.performance,
                    onSelect = vm::setPerformance
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "FPS",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(6.dp))

                CompactChoiceRow(
                    options = listOf("Auto", "Prefer 120", "Cap 60"),
                    selected = state.fps,
                    onSelect = vm::setFps
                )
            }
        }

        item {
            IosSectionCard(
                title = "Виджет расписания",
                subtitle = "Отдельная настройка внешнего вида и контента виджета."
            ) {
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("Открыть настройки виджета")
                }
            }
        }

        item {
            IosSectionCard(
                title = stringResource(R.string.settings_notifications),
                subtitle = "Настройте уведомления перед началом пары."
            ) {
            CompactSwitchRow(
                    title = "Включить напоминания",
                    checked = state.notificationsEnabled,
                    onCheckedChange = vm::setNotificationsEnabled
                )
                Text(
                    text = "Запланировано уведомлений: $plannedCount",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Предупреждать за",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(6.dp))

                CompactChoiceRow(
                    options = listOf("5", "10", "15", "30", "45", "60"),
                    selected = state.leadMinutes.toString(),
                    onSelect = { vm.setLeadMinutes(it.toInt()) }
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Формат",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(6.dp))

                CompactChoiceRow(
                    options = listOf("TitleOnly", "Full"),
                    selected = state.notificationFormat,
                    onSelect = vm::setNotificationFormat
                )

                Spacer(Modifier.height(10.dp))

                CompactSwitchRow(
                    title = "Звук",
                    checked = state.soundEnabled,
                    onCheckedChange = vm::setSoundEnabled
                )

                CompactSwitchRow(
                    title = "Online-пары",
                    checked = state.includeOnline,
                    onCheckedChange = vm::setIncludeOnline
                )

                CompactSwitchRow(
                    title = "Лекции",
                    checked = state.includeLecture,
                    onCheckedChange = vm::setIncludeLecture
                )

                CompactSwitchRow(
                    title = "Показывать тип пары",
                    checked = state.showTypeInFull,
                    onCheckedChange = vm::setShowTypeInFull
                )

                Spacer(Modifier.height(10.dp))

                CompactSwitchRow(
                    title = "Тихие часы",
                    checked = state.quietHoursEnabled,
                    onCheckedChange = vm::setQuietHoursEnabled
                )

                if (state.quietHoursEnabled) {
                    Spacer(Modifier.height(10.dp))

                    Text("Начало", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
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

                    Text("Конец", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
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
                    Text("Системные настройки")
                }
            }
        }

        item {
            IosSectionCard(title = stringResource(R.string.settings_creator)) {

            Text(
                    text = "Sabyr",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            IosSectionCard(title = stringResource(R.string.settings_partners)) {
            Text(
                    text ="Mikosha",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) = IosSectionCard(title = title, content = content)

@Composable
private fun CompactChoiceRow(
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
private fun CompactSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SmallSliderBlock(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors()
            )
        }
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onPrevHour,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("-h")
            }
            Text(String.format("%02d", hour))
            TextButton(
                onClick = onNextHour,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("+h")
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onPrevMinute,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("-m")
            }
            Text(String.format("%02d", minute))
            TextButton(
                onClick = onNextMinute,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("+m")
            }
        }
    }
}
@Composable
private fun HorizontalThemeRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option) }
            )
        }
    }
}
private data class ThemeOption(
    val id: String,
    val label: String
)