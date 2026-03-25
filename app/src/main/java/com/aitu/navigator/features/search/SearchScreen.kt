package com.aitu.navigator.features.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitu.navigator.features.today.normalizeLessons
import com.aitu.navigator.ui.components.IosSectionCard
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.compose.material3.IconButton
@Composable
fun SearchScreen(vm: SearchViewModel = viewModel()) {
    val state by vm.ui.collectAsState()
    val showPinned by vm.showPinned.collectAsState()
    val showHistory by vm.showHistory.collectAsState()

    when (val s = state) {
        SearchUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
                    onClearHistory = vm::clearHistory,
                    onOpenPinned = vm::openPinned,
                    onOpenHistory = vm::openHistory,
                    onTogglePinned = vm::togglePinned
                )
            }
        }
    }

    val readyState = state as? SearchUiState.Ready

    if (showPinned && readyState != null) {
        PinnedTeachersDialog(
            teachers = vm.getPinnedTeachers(),
            onClose = vm::closePinned,
            onTeacherClick = {
                vm.closePinned()
                vm.openTeacher(it)
            }
        )
    }

    if (showHistory && readyState != null) {
        HistoryDialog(
            history = readyState.history,
            onClose = vm::closeHistory,
            onClick = {
                vm.useHistoryItem(it)
                vm.closeHistory()
            },
            onClearHistory = vm::clearHistory,
            onRemoveItem = vm::removeHistoryItem
        )
    }
}
@Composable
private fun SearchHome(
    state: SearchUiState.Ready,
    onQueryChange: (String) -> Unit,
    onTeacherClick: (TeacherEntry) -> Unit,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenPinned: () -> Unit,
    onOpenHistory: () -> Unit,
    onTogglePinned: (TeacherEntry) -> Unit
) {
    val displayTeachers = if (state.query.isBlank()) {
        state.allTeachers.sortedByDescending{it.key in state.pinnedTeacherKeys}
    } else {
        state.filteredTeachers
    }
    val pinnedTeachers = state.allTeachers.filter { it.key in state.pinnedTeacherKeys }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            IosSectionCard(
                title = "Поиск",
                subtitle = "Найди преподавателя по имени или фамилии."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onOpenPinned) { Text("Закрепленные") }
                    OutlinedButton(onClick = onOpenHistory) { Text("История") }
                }
            }
        }

        item {
            SearchInputCard(
                query = state.query,
                onQueryChange = onQueryChange
            )
        }
        if (state.query.isBlank() && pinnedTeachers.isNotEmpty()) {
            item {
                Text(
                    text = "Закреплённые",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                PinnedTeachersSection(
                    teachers = pinnedTeachers,
                    onTeacherClick = onTeacherClick,
                    onTogglePinned = onTogglePinned
                )
            }
        }

        if (state.query.isBlank()) {
            item {
                Text(
                    text = "Все преподаватели",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(displayTeachers) { teacher ->
                SearchTeacherCard(
                    teacher = teacher,
                    isPinned = teacher.key in state.pinnedTeacherKeys,
                    onClick = { onTeacherClick(teacher) },
                    onTogglePinned = { onTogglePinned(teacher) }
                )
            }

            if (state.history.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "История",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    SearchHistorySection(
                        history = state.history,
                        onHistoryClick = onHistoryClick,
                        onClearHistory = onClearHistory
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "Результаты",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (displayTeachers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Ничего не найдено")
                        }
                    }
                }
            } else {
                items(displayTeachers) { teacher ->
                    SearchTeacherCard(
                        teacher = teacher,
                        isPinned = teacher.key in state.pinnedTeacherKeys,
                        onClick = { onTeacherClick(teacher) },
                        onTogglePinned = { onTogglePinned(teacher) }
                    )
                }
            }
        }
    }
}
@Composable
private fun SearchInputCard(
    query: String,
    onQueryChange: (String) -> Unit
) {
    IosSectionCard(title = "Преподаватель") {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Введите имя или фамилию") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("История", style = MaterialTheme.typography.titleMedium)

                TextButton(
                    onClick = onClearHistory,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Очистить")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                history.forEach { item ->
                    AssistChip(
                        onClick = { onHistoryClick(item) },
                        label = {
                            Text(
                                text = item,
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun SearchTeacherCard(
    teacher: TeacherEntry,
    isPinned: Boolean,
    onClick: () -> Unit,
    onTogglePinned: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // круглая иконка слева
            Box(
                modifier = Modifier
                    .size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("•")
                    }
                }
            }

            // Дисплей имен и количество пар преподов
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = teacher.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${teacher.lessonCount} пар",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Стрелка подробнее и пин
            Text(
                text = "›",
                style = MaterialTheme.typography.titleMedium
            )

            // pin
            TextButton(
                onClick = onTogglePinned,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(30.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Text(if (isPinned) "Закреплён" else "Закрепить")
            }
        }
    }
}
@Composable
private fun TeacherScheduleScreen(
    teacher: TeacherEntry,
    onBack: () -> Unit
) {
    val currentTime by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(60_000)
        }
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val selectedDay = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
    val isToday = selectedDate == LocalDate.now()

    val lessonsForDay = remember(teacher.lessons, selectedDay) {
        teacher.lessons.filter { it.day.equals(selectedDay, ignoreCase = true) }
    }

    val slots = remember(lessonsForDay) {
        normalizeLessons(lessonsForDay)
    }

    var detailsSlot by remember { mutableStateOf<com.aitu.navigator.features.today.NormalizedSlot?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TeacherTopSection(
            teacherName = teacher.displayName,
            date = selectedDate,
            onBack = onBack,
            onPrev = { selectedDate = selectedDate.minusDays(1) },
            onNext = { selectedDate = selectedDate.plusDays(1) },
            onToday = { selectedDate = LocalDate.now() }
        )

        TeacherDayTabs(
            selectedDay = selectedDay,
            onSelect = { day ->
                val today = LocalDate.now()
                val dayOrder = listOf(
                    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
                )
                val targetIndex = dayOrder.indexOf(day)
                if (targetIndex >= 0) {
                    var candidate = today
                    while (candidate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) != day) {
                        candidate = candidate.plusDays(1)
                    }
                    selectedDate = candidate
                }
            }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "$selectedDay • $formattedDate",
                    style = MaterialTheme.typography.titleMedium
                )

                if (slots.isEmpty()) {
                    Text("На этот день занятий нет")
                } else {
                    slots.forEachIndexed { index, slot ->
                        TeacherLessonRow(
                            slot = slot,
                            now = currentTime,
                            dayDate = selectedDate,
                            showStatus = isToday,
                            onOpenDetails = { detailsSlot = slot }
                        )

                        if (index != slots.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    detailsSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { detailsSlot = null },
            title = { Text("Детали пары") },
            text = {
                Column {
                    Text(slot.discipline, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("День: ${slot.day}")
                    Text("Время: ${slot.time}")
                    Text("Тип: ${slot.type}")
                    Spacer(Modifier.height(10.dp))
                    Text("Аудитории:")
                    slot.locations.forEach {
                        Text("• ${it.classroom} — ${it.lecturer}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { detailsSlot = null }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
private fun TeacherTopSection(
    teacherName: String,
    date: LocalDate,
    onBack: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val formattedDate = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("◀")
                }

                Text(
                    text = teacherName,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column {
                Text(dayName, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(formattedDate, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrev,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("◀")
                }

                Button(
                    onClick = onToday,
                    modifier = Modifier
                        .weight(2f)
                        .height(40.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Сегодня", style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedButton(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("▶")
                }
            }
        }
    }
}

@Composable
private fun TeacherDayTabs(
    selectedDay: String,
    onSelect: (String) -> Unit
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            FilterChip(
                selected = selectedDay.equals(day, ignoreCase = true),
                onClick = { onSelect(day) },
                label = { Text(day.take(3)) }
            )
        }
    }
}
@Composable
private fun TeacherLessonRow(
    slot: com.aitu.navigator.features.today.NormalizedSlot,
    now: LocalTime,
    dayDate: LocalDate,
    showStatus: Boolean,
    onOpenDetails: () -> Unit
) {
    val status = getTeacherLessonStatus(
        timeRange = slot.time,
        nowTime = now,
        dayDate = dayDate
    )

    val statusText = when {
        !showStatus -> ""
        status == TeacherLessonStatus.NOW -> "NOW"
        status == TeacherLessonStatus.SOON -> "SOON"
        status == TeacherLessonStatus.COMPLETED -> "DONE"
        else -> ""
    }

    val statusColor = when (status) {
        TeacherLessonStatus.NOW -> MaterialTheme.colorScheme.primary
        TeacherLessonStatus.SOON -> MaterialTheme.colorScheme.tertiary
        TeacherLessonStatus.COMPLETED -> MaterialTheme.colorScheme.outline
        TeacherLessonStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val firstLocation = slot.locations.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = slot.time,
            modifier = Modifier.width(104.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = slot.discipline,
                style = MaterialTheme.typography.titleMedium
            )

            firstLocation?.let { loc ->
                Text(
                    text = "${loc.classroom} • ${loc.lecturer}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (statusText.isNotBlank()) {
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            TextButton(
                onClick = onOpenDetails,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    ">",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun getTeacherLessonStatus(
    timeRange: String,
    nowTime: LocalTime,
    dayDate: LocalDate,
    todayDate: LocalDate = LocalDate.now()
): TeacherLessonStatus {
    when {
        dayDate.isAfter(todayDate) -> return TeacherLessonStatus.UPCOMING
        dayDate.isBefore(todayDate) -> return TeacherLessonStatus.COMPLETED
    }

    val parts = timeRange.replace(" ", "").split("-")
    if (parts.size != 2) return TeacherLessonStatus.UPCOMING

    val start = LocalTime.parse(parts[0])
    val end = LocalTime.parse(parts[1])

    return when {
        !nowTime.isBefore(start) && nowTime.isBefore(end) -> TeacherLessonStatus.NOW
        nowTime.isBefore(start) && java.time.Duration.between(nowTime, start).toMinutes() <= 15 -> TeacherLessonStatus.SOON
        !nowTime.isBefore(end) -> TeacherLessonStatus.COMPLETED
        else -> TeacherLessonStatus.UPCOMING
    }
}
//класс отметки предмета
private enum class TeacherLessonStatus {
    NOW, SOON, COMPLETED, UPCOMING
}
@Composable
private fun PinnedTeachersDialog(
    teachers: List<TeacherEntry>,
    onClose: () -> Unit,
    onTeacherClick: (TeacherEntry) -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {},
        title = { Text("Закрепленные") },
        text = {
            Column {
                if (teachers.isEmpty()) {
                    Text("Пока пусто")
                } else {
                    teachers.forEach { teacher ->
                        TextButton(
                            onClick = { onTeacherClick(teacher) }
                        ) {
                            Text(teacher.displayName)
                        }
                    }
                }
            }
        }
    )
}
@Composable
private fun HistoryDialog(
    history: List<String>,
    onClose: () -> Unit,
    onClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveItem: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("История поиска") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (history.isEmpty()) {
                    Text("История пуста")
                } else {
                    history.forEach { item ->

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            TextButton(
                                onClick = { onClick(item) }
                            ) {
                                Text(item)
                            }

                            TextButton(
                                onClick = { onRemoveItem(item) }
                            ) {
                                Text("✕")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClearHistory) {
                    Text("Очистить")
                }

                TextButton(onClick = onClose) {
                    Text("Закрыть")
                }
            }
        }
    )
}
@Composable
private fun PinnedTeachersSection(
    teachers: List<TeacherEntry>,
    onTeacherClick: (TeacherEntry) -> Unit,
    onTogglePinned: (TeacherEntry) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        teachers.forEach { teacher ->
            Card(
                onClick = { onTeacherClick(teacher) },
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .width(180.dp)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📌")
                        TextButton(
                            onClick = { onTogglePinned(teacher) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("✕")
                        }
                    }

                    Text(
                        text = teacher.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${teacher.lessonCount} пар",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}