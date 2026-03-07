package com.aitu.navigator.features.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitu.navigator.features.today.normalizeLessons
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SearchScreen(vm: SearchViewModel = viewModel()) {
    val state by vm.ui.collectAsState()

    when (val s = state) {
        SearchUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Загрузка преподавателей…")
        }

        is SearchUiState.Error -> {
            Column(Modifier.padding(16.dp)) {
                Text("Ошибка", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(s.message)
            }
        }

        is SearchUiState.Ready -> {
            if (s.selectedTeacher != null) {
                TeacherScheduleScreen(
                    teacher = s.selectedTeacher,
                    onBack = vm::closeTeacher
                )
            } else {
                SearchHome(
                    state = s,
                    onQueryChange = vm::updateQuery,
                    onTeacherClick = vm::openTeacher,
                    onHistoryClick = vm::useHistoryItem,
                    onClearHistory = vm::clearHistory
                )
            }
        }
    }
}

@Composable
private fun SearchHome(
    state: SearchUiState.Ready,
    onQueryChange: (String) -> Unit,
    onTeacherClick: (TeacherEntry) -> Unit,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = { Text("Поиск преподавателя") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        if (state.query.isBlank()) {
            if (state.history.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("История", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onClearHistory) {
                        Text("Очистить")
                    }
                }

                Spacer(Modifier.height(8.dp))

                state.history.forEach { item ->
                    AssistChip(
                        onClick = { onHistoryClick(item) },
                        label = { Text(item) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(8.dp))
            }

            Text("Популярные преподаватели", style = MaterialTheme.typography.titleMedium)
        } else {
            Text("Результаты", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.filteredTeachers) { teacher ->
                Card(
                    onClick = { onTeacherClick(teacher) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(teacher.displayName, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Занятий: ${teacher.lessonCount}")
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherScheduleScreen(
    teacher: TeacherEntry,
    onBack: () -> Unit
) {
    var selectedDay by remember {
        mutableStateOf(
            LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        )
    }

    val lessonsForDay = remember(teacher.lessons, selectedDay) {
        teacher.lessons.filter { it.day.equals(selectedDay, ignoreCase = true) }
    }

    val slots = remember(lessonsForDay) {
        normalizeLessons(lessonsForDay)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Назад")
            }
            Text(teacher.displayName, style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(12.dp))
        Text("Занятий: ${teacher.lessonCount}")

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday").forEach { day ->
                AssistChip(
                    onClick = { selectedDay = day },
                    label = { Text(day.take(3)) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (slots.isEmpty()) {
            Text("На $selectedDay занятий нет")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(slots) { slot ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(slot.discipline, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Время: ${slot.time}")
                            Text("Тип: ${slot.type}")
                            Spacer(Modifier.height(6.dp))
                            slot.locations.forEach {
                                Text("• ${it.classroom} — ${it.lecturer}")
                            }
                        }
                    }
                }
            }
        }
    }
}