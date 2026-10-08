package com.aitu.navigator.features.today

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.aitu.navigator.R
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import androidx.compose.ui.graphics.Color
//полный вывод сегодня
@Composable
fun TodayScreen(
    vm: TodayViewModel = viewModel(),
    onOpenAnalytics: () -> Unit = {},
    onOpenAtlas: (String) -> Unit = {}
) {
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
                ) {
                    Text("Ввести другую группу")
                }
            }
        }

        is TodayUiState.Error -> ErrorBlock(s.message)

        is TodayUiState.Content -> TodayContent(
            s = s,
            vm = vm,
            onOpenAnalytics = onOpenAnalytics,
            onOpenAtlas = onOpenAtlas
        )
    }
}

@Composable
private fun TodayContent(
    s: TodayUiState.Content,
    vm: TodayViewModel,
    onOpenAnalytics: () -> Unit,
    onOpenAtlas: (String) -> Unit
) {
    val currentTime by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(60_000)
        }
    }

    val selectedDate by vm.selectedDateFlow.collectAsState()
    val effectiveDate = selectedDate ?: LocalDate.now()
    val isToday = effectiveDate == LocalDate.now()
    var showFullSchedule by remember { mutableStateOf(false) }

    val effectiveDay = effectiveDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val lessonsForDay = remember(s.groupData.schedule, effectiveDay) {
        s.groupData.schedule.filter {
            it.day.trim().equals(effectiveDay.trim(), ignoreCase = true)
        }
    }
    val slots = remember(lessonsForDay) { normalizeLessons(lessonsForDay) }

    var noteOpen by remember { mutableStateOf(false) }
    val noteText by vm.noteTextFlow(effectiveDate).collectAsState(initial = "")
    val noteImages by vm.noteImagesFlow(effectiveDate).collectAsState(initial = emptyList())

    var detailsSlot by remember { mutableStateOf<NormalizedSlot?>(null) }

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
                TodayTopSection(
                    date = effectiveDate,
                    group = s.group,
                    onPrev = { vm.shiftDay(-1) },
                    onNext = { vm.shiftDay(+1) },
                    onTodayClick = { vm.resetToToday() },
                    onGroupClick = { vm.changeGroup() }
                )
            }

            if (showFullSchedule) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showFullSchedule = false },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("◀")
                            }

                            Text(
                                text = "Полное расписание",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        FullScheduleInline(
                            allLessons = s.groupData.schedule
                        )
                    }
                }
            } else {
                item {
                    TodaySchedulePanel(
                        slots = slots,
                        now = currentTime,
                        dayDate = effectiveDate,
                        isToday = isToday,
                        onOpenNote = { noteOpen = true },
                        onOpenAnalytics = onOpenAnalytics,
                        onOpenFull = { showFullSchedule = true },
                        onOpenDetails = { detailsSlot = it },
                        onOpenAtlas = onOpenAtlas
                    )
                }
            }
        }
    }

    if (noteOpen) {
        NoteDialog(
            date = effectiveDate,
            initialText = noteText,
            initialImagePaths = noteImages,
            onSave = {
                vm.saveNote(
                    date = effectiveDate,
                    text = it.text,
                    keptImagePaths = it.keptImagePaths,
                    newImageUris = it.newUris
                )
                noteOpen = false
            },
            onDismiss = { noteOpen = false }
        )
    }

    detailsSlot?.let { slot ->
        LessonDetailsDialog(
            slot = slot,
            onDismiss = { detailsSlot = null },
            onOpenAtlas = onOpenAtlas
        )
    }
}
//первый верхний блок
@Composable
private fun TodayTopSection(
    date: LocalDate,
    group: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onTodayClick: () -> Unit,
    onGroupClick: () -> Unit
) {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val formattedDate = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Сегодня",
                style = MaterialTheme.typography.headlineSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                OutlinedButton(
                    onClick = onGroupClick,
                    modifier = Modifier.height(38.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
                        .height(42.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("◀")
                }

                Button(
                    onClick = onTodayClick,
                    modifier = Modifier
                        .weight(2f)
                        .height(42.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Сегодня",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                OutlinedButton(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(42.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("▶")
                }
            }
        }
    }
}
//расписание панель
@Composable
private fun TodaySchedulePanel(
    slots: List<NormalizedSlot>,
    now: LocalTime,
    dayDate: LocalDate,
    isToday: Boolean,
    onOpenNote: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenFull: () -> Unit,
    onOpenDetails: (NormalizedSlot) -> Unit,
    onOpenAtlas: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сегодня", style = MaterialTheme.typography.titleLarge)

                    if (slots.isNotEmpty()) {
                        Text(
                            text = "${slots.first().discipline} • ${slots.first().time}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenFull,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Полное")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isToday) {
                    OutlinedButton(
                        onClick = onOpenNote,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Заметка", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedButton(
                    onClick = onOpenAnalytics,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Аналитика", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (slots.isEmpty()) {
                Text("На выбранный день занятий нет")
            } else {
                slots.forEachIndexed { index, slot ->
                    TodayLessonRow(
                        slot = slot,
                        now = now,
                        dayDate = dayDate,
                        onOpenDetails = { onOpenDetails(slot) },
                        onOpenAtlas = onOpenAtlas
                    )

                    if (index != slots.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
//Уроки время и апдейт времени
@Composable
private fun TodayLessonRow(
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

    val statusText = when (status) {
        LessonStatus.NOW -> "NOW"
        LessonStatus.SOON -> "SOON"
        LessonStatus.COMPLETED -> "DONE"
        LessonStatus.UPCOMING -> ""
    }

    val statusColor = when (status) {
        LessonStatus.NOW -> MaterialTheme.colorScheme.primary
        LessonStatus.SOON -> MaterialTheme.colorScheme.tertiary
        LessonStatus.COMPLETED -> MaterialTheme.colorScheme.outline
        LessonStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val firstLocation = slot.locations.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = slot.time,
                modifier = Modifier.width(96.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { onOpenAtlas(loc.classroom) },
                            modifier = Modifier
                                .height(28.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = loc.classroom,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = loc.lecturer,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                        "›",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
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
private fun LessonDetailsDialog(
    slot: NormalizedSlot,
    onDismiss: () -> Unit,
    onOpenAtlas: (String) -> Unit
) {
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
                slot.locations.firstOrNull()?.classroom?.let { room ->
                    OutlinedButton(
                        onClick = { onOpenAtlas(room) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Открыть карту кабинетов")
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Text("Аудитории:")
                slot.locations.forEach {
                    Text("• ${it.classroom} — ${it.lecturer}")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun NoteDialog(
    date: LocalDate,
    initialText: String,
    initialImagePaths: List<String>,
    onSave: (NoteDialogResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var text by remember(initialText) { mutableStateOf(initialText) }
    var keptPaths by remember(initialImagePaths) { mutableStateOf(initialImagePaths) }
    var newUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var previewPath by remember { mutableStateOf<String?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) newUris = (newUris + uris).distinct().take((10 - keptPaths.size).coerceAtLeast(0))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Заметка (${date})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Введите заметку…") }
                )

                Text("Изображения: ${keptPaths.size + newUris.size}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { picker.launch("image/*") },
                        enabled = keptPaths.size + newUris.size < 10
                    ) { Text("Добавить") }
                    OutlinedButton(
                        enabled = keptPaths.size + newUris.size < 10,
                        onClick = {
                            val images = runCatching {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip
                                (0 until (clip?.itemCount ?: 0)).mapNotNull { index ->
                                    clip?.getItemAt(index)?.uri?.takeIf { uri ->
                                        uri.scheme == "content" &&
                                            context.contentResolver.getType(uri)?.startsWith("image/") == true
                                    }
                                }
                            }.getOrDefault(emptyList())
                            if (images.isEmpty()) {
                                Toast.makeText(context, R.string.note_clipboard_no_image, Toast.LENGTH_SHORT).show()
                            } else {
                                newUris = (newUris + images).distinct().take((10 - keptPaths.size).coerceAtLeast(0))
                            }
                        }
                    ) { Text("Вставить") }
                }

                val imageItems = keptPaths + newUris.map { it.toString() }
                if (imageItems.isEmpty()) {
                    Text("Пока нет изображений.")
                } else {
                    imageItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (item.startsWith("/")) File(item).name else "new image",
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (item.startsWith("/")) previewPath = item else previewUri = Uri.parse(item)
                                    },
                                maxLines = 1
                            )
                            TextButton(onClick = {
                                if (item.startsWith("/")) {
                                    keptPaths = keptPaths.filterNot { it == item }
                                } else {
                                    newUris = newUris.filterNot { it.toString() == item }
                                }
                            }) { Text("Удалить") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(NoteDialogResult(text, keptPaths, newUris)) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    if (previewPath != null || previewUri != null) {
        AlertDialog(
            onDismissRequest = {
                previewPath = null
                previewUri = null
            },
            title = { Text("Просмотр") },
            text = {
                when {
                    previewPath != null -> {
                        val bitmap = remember(previewPath) { BitmapFactory.decodeFile(previewPath) }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("Не удалось открыть файл")
                        }
                    }
                    previewUri != null -> {
                        val bitmap = remember(previewUri) {
                            context.contentResolver.openInputStream(previewUri!!)?.use {
                                BitmapFactory.decodeStream(it)
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("Не удалось открыть файл")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    previewPath = null
                    previewUri = null
                }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

private data class NoteDialogResult(
    val text: String,
    val keptImagePaths: List<String>,
    val newUris: List<Uri>
)

@Composable
private fun FullScheduleInline(
    allLessons: List<com.aitu.navigator.data.model.Lesson>
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    var selectedDay by remember {
        mutableStateOf(
            LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        )
    }

    val lessonsForDay = remember(allLessons, selectedDay) {
        allLessons.filter { it.day.trim().equals(selectedDay.trim(), ignoreCase = true) }
    }

    val slots = remember(lessonsForDay) {
        normalizeLessons(lessonsForDay)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.forEach { day ->
                FilterChip(
                    selected = selectedDay.equals(day, ignoreCase = true),
                    onClick = { selectedDay = day },
                    label = { Text(day.take(3)) }
                )
            }
        }

        if (slots.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Пусто", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("На этот день занятий нет")
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                slots.forEach { slot ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(slot.discipline, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Время: ${slot.time}")
                            Text("Тип: ${slot.type}")
                            Spacer(Modifier.height(6.dp))
                            slot.locations.forEach { loc ->
                                Text("• ${loc.classroom} — ${loc.lecturer}")
                            }
                        }
                    }
                }
            }
        }
    }
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
        ) {
            Text("Сохранить")
        }
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

private enum class LessonStatus {
    NOW, SOON, COMPLETED, UPCOMING
}
