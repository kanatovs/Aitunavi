package com.aitu.navigator.features.today

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
@Composable
fun TodayScreen(vm: TodayViewModel = viewModel()) {
    val state by vm.ui.collectAsState()

    when (val s = state) {
        TodayUiState.Loading -> CenterText("Загрузка…")

        is TodayUiState.NeedGroup -> GroupPicker(
            input = s.input,
            isFound = s.isFound,
            onInputChange = vm::onGroupInputChanged,
            onSave = vm::saveGroup
        )

        is TodayUiState.GroupNotFound -> {
            Column(Modifier.padding(16.dp)) {
                Text("Ошибка", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Группа не найдена: ${s.group}")
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { vm.changeGroup() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ввести другую группу") }
            }
        }

        is TodayUiState.Error -> ErrorBlock(s.message)

        is TodayUiState.Content -> TodayContent(s, vm)
    }
}

@Composable
private fun TodayContent(s: TodayUiState.Content, vm: TodayViewModel) {
    val currentTime by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(60_000)
        }
    }

    val selectedDate by vm.selectedDateFlow.collectAsState()
    val effectiveDate = selectedDate ?: LocalDate.now()
    val isToday = effectiveDate == LocalDate.now()

    val effectiveDay = effectiveDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) // Monday
    val lessonsForDay = remember(s.groupData.schedule, effectiveDay) {
        s.groupData.schedule.filter { it.day.trim().equals(effectiveDay.trim(), ignoreCase = true) }
    }
    val slots = remember(lessonsForDay) { normalizeLessons(lessonsForDay) }

    // Note dialog state
    var noteOpen by remember { mutableStateOf(false) }
    val noteText by vm.noteTextFlow(effectiveDate).collectAsState(initial = "")

    // Details dialog state
    var detailsSlot by remember { mutableStateOf<NormalizedSlot?>(null) }
    var atlasRoom by remember { mutableStateOf<String?>(null) }

    val headerInfo = remember(isToday, slots, currentTime, effectiveDate) {
        if (!isToday) null else calcHeaderInfo(slots, currentTime, effectiveDate)
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> total += dragAmount },
                    onDragEnd = {
                        if (abs(total) > 120f) {
                            if (total > 0) vm.shiftDay(-1) else vm.shiftDay(+1)
                        }
                        total = 0f
                    }
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TodayHeaderCard(
                    date = effectiveDate,
                    group = s.group,
                    headerInfo = headerInfo,
                    onPrev = { vm.shiftDay(-1) },
                    onNext = { vm.shiftDay(+1) },
                    onGroupClick = { vm.changeGroup() },
                    onTestNotifications = { vm.showTestNotificationNow() },
                    onNoteClick = { if (isToday) noteOpen = true },
                    showNote = isToday
                )
                Spacer(Modifier.height(8.dp))


                OutlinedButton(
                    onClick = { vm.openFullSchedule() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Полное расписание")
                }
            }

            if (slots.isEmpty()) {
                item {
                    EmptyDayCard(
                        onFullSchedule = { vm.openFullSchedule() }
                    )
                }
            } else {
                items(slots) { slot ->
                    SlotCard(
                        slot = slot,
                        now = currentTime,
                        dayDate = effectiveDate,
                        onOpenDetails = { detailsSlot = slot },
                        onOpenAtlas = { room -> atlasRoom = room }
                    )
                }
            }
        }
    }

    if (noteOpen) {
        NoteDialog(
            date = effectiveDate,
            initialText = noteText,
            onSave = { vm.saveNote(effectiveDate, it); noteOpen = false },
            onDismiss = { noteOpen = false }
        )
    }

    detailsSlot?.let { slot ->
        LessonDetailsDialog(
            slot = slot,
            onDismiss = { detailsSlot = null }
        )
    }

    atlasRoom?.let { room ->
        AlertDialog(
            onDismissRequest = { atlasRoom = null },
            title = { Text("Атлас (заглушка)") },
            text = { Text("Открыть Атлас для аудитории: $room") },
            confirmButton = {
                Button(onClick = { atlasRoom = null }) { Text("Ок") }
            }
        )
    }

    // Full schedule dialog screen
    if (vm.fullScheduleOpen.collectAsState().value) {
        FullScheduleDialog(
            group = s.group,
            allLessons = s.groupData.schedule,
            onDismiss = { vm.closeFullSchedule() }
        )
    }
}

@Composable
private fun TodayHeaderCard(
    date: LocalDate,
    group: String,
    headerInfo: HeaderInfo?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onGroupClick: () -> Unit,
    onTestNotifications: () -> Unit,
    onNoteClick: () -> Unit,
    showNote: Boolean
) {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val formattedDate = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))

    Card {
        Column(Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) { Text("◀") }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayName, style = MaterialTheme.typography.titleMedium)
                    Text(formattedDate, style = MaterialTheme.typography.bodySmall)
                }

                IconButton(onClick = onNext) { Text("▶") }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGroupClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Группа: $group (нажми, чтобы сменить)")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onTestNotifications,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Тест уведомления сейчас")
            }

            if (showNote) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onNoteClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Заметка")
                }
            }

            if (headerInfo != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(headerInfo.title, style = MaterialTheme.typography.labelLarge)
                    Text(headerInfo.subtitle, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun EmptyDayCard(onFullSchedule: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Пусто", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("На выбранный день пар нет.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onFullSchedule, modifier = Modifier.fillMaxWidth()) {
                Text("Полное расписание")
            }
        }
    }
}

@Composable
private fun SlotCard(
    slot: NormalizedSlot,
    now: LocalTime,
    dayDate: LocalDate,
    onOpenDetails: () -> Unit,
    onOpenAtlas: (String) -> Unit
) {
    val status = getLessonStatus(
        timeRange = slot.time,
        nowTime = now,
        dayDate = dayDate
    )

    val statusColor = when (status) {
        LessonStatus.NOW -> MaterialTheme.colorScheme.primary
        LessonStatus.SOON -> MaterialTheme.colorScheme.tertiary
        LessonStatus.COMPLETED -> MaterialTheme.colorScheme.outline
        LessonStatus.UPCOMING -> MaterialTheme.colorScheme.secondary
    }

    Card(onClick = onOpenDetails) {
        Column(Modifier.padding(16.dp)) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(slot.discipline, style = MaterialTheme.typography.titleMedium)
                Text(status.name, color = statusColor)
            }

            Spacer(Modifier.height(6.dp))
            Text("Время: ${slot.time}")
            Text("Тип: ${slot.type}")

            Spacer(Modifier.height(10.dp))
            Text("Аудитории:", style = MaterialTheme.typography.labelLarge)

            slot.locations.forEach { loc ->
                TextButton(
                    onClick = { onOpenAtlas(loc.classroom) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("• ${loc.classroom} — ${loc.lecturer}")
                }
            }
        }
    }
}

@Composable
private fun LessonDetailsDialog(slot: NormalizedSlot, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                slot.locations.forEach { Text("• ${it.classroom} — ${it.lecturer}") }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
private fun NoteDialog(
    date: LocalDate,
    initialText: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Заметка (${date})") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Введите заметку…") }
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text) }) { Text("Сохранить") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun FullScheduleDialog(
    group: String,
    allLessons: List<com.aitu.navigator.data.model.Lesson>,
    onDismiss: () -> Unit
) {
    val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")

    var selectedDay by remember {
        mutableStateOf(java.time.LocalDate.now().dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH))
    }

    val lessonsForDay = remember(allLessons, selectedDay) {
        allLessons.filter { it.day.trim().equals(selectedDay.trim(), ignoreCase = true) }
    }

    val slots = remember(lessonsForDay) { normalizeLessons(lessonsForDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Полное расписание:\n$group") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp, max = 520.dp) // <-- главное: контент не вылезает
            ) {

                // ✅ ДНИ НЕДЕЛИ С ГОРИЗОНТАЛЬНЫМ СКРОЛЛОМ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    days.forEach { d ->
                        FilterChip(
                            selected = selectedDay.equals(d, ignoreCase = true),
                            onClick = { selectedDay = d },
                            label = { Text(d.take(3)) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ✅ СПИСОК ПАР С ВЕРТИКАЛЬНЫМ СКРОЛЛОМ
                if (slots.isEmpty()) {
                    Text("Пусто")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(slots) { slot ->
                            Column {
                                Text("${slot.time} — ${slot.discipline}", style = MaterialTheme.typography.bodyMedium)
                                Text("(${slot.type})", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}
@Composable
private fun GroupPicker(
    input: String,
    isFound: Boolean?,
    onInputChange: (String) -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(input) }
    LaunchedEffect(input) { text = input }

    Column(Modifier.padding(16.dp)) {
        Text("Укажи группу", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { new ->
                text = new
                onInputChange(new)
            },
            label = { Text("Например: SE-2310") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        when (isFound) {
            null -> Text("Введите группу", style = MaterialTheme.typography.bodyMedium)
            true -> Text("Найдена ✅", color = MaterialTheme.colorScheme.primary)
            false -> Text("Не найдена ❌", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { onSave(text) },
            enabled = text.isNotBlank() && isFound == true,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Сохранить") }
    }
}

@Composable
private fun ErrorBlock(msg: String) {
    Column(Modifier.padding(16.dp)) {
        Text("Ошибка", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(msg)
    }
}

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}

private data class HeaderInfo(val title: String, val subtitle: String)

private fun calcHeaderInfo(
    slots: List<NormalizedSlot>,
    now: LocalTime,
    dayDate: LocalDate
): HeaderInfo? {
    if (dayDate != LocalDate.now()) return null

    val nowSlot = slots.firstOrNull {
        getLessonStatus(it.time, nowTime = now, dayDate = dayDate) == LessonStatus.NOW
    }
    if (nowSlot != null) {
        val end = runCatching { LocalTime.parse(nowSlot.time.substringAfter("-")) }.getOrNull()
        if (end != null) {
            val mins = java.time.Duration.between(now, end).toMinutes().coerceAtLeast(0)
            return HeaderInfo("NOW", "До конца $mins мин")
        }
        return HeaderInfo("NOW", "Идёт пара")
    }

    val next = slots
        .mapNotNull { s ->
            val start = runCatching { LocalTime.parse(s.time.substringBefore("-")) }.getOrNull()
                ?: return@mapNotNull null
            if (start.isAfter(now)) start else null
        }
        .minOrNull()

    if (next != null) {
        val mins = java.time.Duration.between(now, next).toMinutes().coerceAtLeast(0)
        val title = if (mins <= 60) "SOON" else "UPCOMING"
        return HeaderInfo(title, "До начала $mins мин")
    }

    return HeaderInfo("COMPLETED", "Все пары завершены")
}

private fun getLessonStatus(
    timeRange: String,
    nowTime: LocalTime,
    dayDate: LocalDate,
    todayDate: LocalDate = LocalDate.now()
): LessonStatus {
    when {
        dayDate.isAfter(todayDate) -> return LessonStatus.UPCOMING
        dayDate.isBefore(todayDate) -> return LessonStatus.COMPLETED
    }

    val parts = timeRange.replace(" ", "").split("-")
    if (parts.size != 2) return LessonStatus.UPCOMING

    val start = LocalTime.parse(parts[0])
    val end = LocalTime.parse(parts[1])

    return when {
        !nowTime.isBefore(start) && nowTime.isBefore(end) -> LessonStatus.NOW
        nowTime.isBefore(start) && java.time.Duration.between(nowTime, start).toMinutes() <= 15 -> LessonStatus.SOON
        !nowTime.isBefore(end) -> LessonStatus.COMPLETED
        else -> LessonStatus.UPCOMING
    }
}

private enum class LessonStatus { NOW, SOON, COMPLETED, UPCOMING }