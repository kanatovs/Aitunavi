package com.aitu.navigator.features.today

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aitu.navigator.data.datastore.AppPrefs
import com.aitu.navigator.data.model.GroupSchedule
import com.aitu.navigator.data.repository.MikoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlinx.coroutines.flow.map
import com.aitu.navigator.data.datastore.SettingsPrefs
import com.aitu.navigator.features.schedule.notifications.NotificationScheduler
import com.aitu.navigator.features.schedule.notifications.NotificationUtils
import kotlinx.coroutines.flow.first
import java.io.File
sealed class TodayUiState {
    data object Loading : TodayUiState()
    data class Error(val message: String) : TodayUiState()

    data class NeedGroup(
        val input: String = "",
        val isFound: Boolean? = null
    ) : TodayUiState()

    data class GroupNotFound(val group: String) : TodayUiState()

    data class Content(
        val group: String,
        val groupData: GroupSchedule
    ) : TodayUiState()
}

class TodayViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MikoRepository(app.applicationContext)
    private val prefs = AppPrefs(app.applicationContext)
    private val settingsPrefs = SettingsPrefs(app.applicationContext)//testim
    // Загружаем Miko.json один раз
    private val groupsResult = MutableStateFlow<Result<List<GroupSchedule>>?>(null)

    // Текущее введённое значение (для экрана выбора группы)
    private val groupInput = MutableStateFlow("")

    // Выбранный день (null = сегодня). UI читает и фильтрует по нему.
    private val selectedDate = MutableStateFlow<java.time.LocalDate?>(null) // null = today
    val selectedDateFlow: StateFlow<java.time.LocalDate?> = selectedDate

    fun resetToToday() {
        selectedDate.value = null
    }

    fun shiftDay(deltaDays: Long) {
        val base = selectedDate.value ?: java.time.LocalDate.now()
        selectedDate.value = base.plusDays(deltaDays)
    }

    private val _ui = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val ui: StateFlow<TodayUiState> = _ui
    // --- Full schedule open flag (для кнопки "Полное расписание") ---
    private val _fullScheduleOpen = MutableStateFlow(false)
    val fullScheduleOpen: StateFlow<Boolean> = _fullScheduleOpen

    fun openFullSchedule() { _fullScheduleOpen.value = true }
    fun closeFullSchedule() { _fullScheduleOpen.value = false }
    init {
        loadGroups()
        bind()
    }

    private fun loadGroups() {
        _ui.value = TodayUiState.Loading
        groupsResult.value = repo.loadGroups()
    }

    private fun bind() {
        viewModelScope.launch {
            combine(prefs.activeGroup, groupsResult, groupInput) { savedGroup, groupsRes, input ->
                Triple(savedGroup, groupsRes, input)
            }.collect { (savedGroupRaw, groupsRes, inputRaw) ->

                val res = groupsRes ?: run {
                    _ui.value = TodayUiState.Loading
                    return@collect
                }

                res.fold(
                    onSuccess = { groups ->
                        val groupsIndex = buildIndex(groups)

                        val savedGroup = normalizeGroup(savedGroupRaw)
                        val input = inputRaw

                        // Если группа сохранена — открываем контент (или not found)
                        if (!savedGroup.isNullOrBlank()) {
                            val found = groupsIndex[savedGroup]
                            _ui.value = if (found == null) {
                                TodayUiState.GroupNotFound(savedGroup)
                            } else {
                                TodayUiState.Content(found.group_name, found)
                            }
                            return@fold
                        }

                        // Если нет сохраненной группы — показываем ввод
                        val normalizedInput = normalizeGroup(input)
                        val isFound = when {
                            normalizedInput.isNullOrBlank() -> null
                            else -> groupsIndex.containsKey(normalizedInput)
                        }

                        _ui.value = TodayUiState.NeedGroup(
                            input = input,
                            isFound = isFound
                        )
                    },
                    onFailure = { e ->
                        _ui.value = TodayUiState.Error("Ошибка чтения Miko.json: ${e.message}")
                    }
                )
            }
        }
    }

    fun onGroupInputChanged(text: String) {
        groupInput.value = text
    }

    fun saveGroup(raw: String) {
        val normalized = normalizeGroup(raw).orEmpty()
        if (normalized.isBlank()) return

        // Проверяем, загружены ли группы
        val res = groupsResult.value
        if (res == null) return

        res.fold(
            onSuccess = { groups ->
                val index = buildIndex(groups)
                if (!index.containsKey(normalized)) {
                    // НЕ сохраняем неправильную группу — просто показываем ввод снова
                    groupInput.value = raw
                    _ui.value = TodayUiState.NeedGroup(input = raw, isFound = false)
                    return
                }

                // Если группа существует — сохраняем
                viewModelScope.launch {
                    prefs.setActiveGroup(normalized)
                }
            },
            onFailure = {
                _ui.value = TodayUiState.Error("Не удалось проверить группу: ${it.message}")
            }
        )
    }

    fun changeGroup() {
        viewModelScope.launch {
            prefs.clearActiveGroup()
        }
        groupInput.value = ""
        resetToToday()
    }
    // --- Notes (Заметка на дату) ---
    fun noteTextFlow(date: LocalDate) = prefs.noteFlow(date.toString())
    fun noteImagesFlow(date: LocalDate) = prefs.noteImagesFlow(date.toString())

    fun saveNote(
        date: LocalDate,
        text: String,
        keptImagePaths: List<String>,
        newImageUris: List<Uri>
    ) {
        viewModelScope.launch {
            val savedNewPaths = newImageUris.mapNotNull { copyImageToNotesDir(it) }
            val allPaths = (keptImagePaths + savedNewPaths).distinct()

            if (text.isBlank() && allPaths.isEmpty()) {
                prefs.clearNote(date.toString())
            } else {
                prefs.setNote(date.toString(), text)
                prefs.setNoteImages(date.toString(), allPaths)
            }
        }
    }
    // ---------- helpers ----------

    private fun buildIndex(groups: List<GroupSchedule>): Map<String, GroupSchedule> {
        val map = LinkedHashMap<String, GroupSchedule>(groups.size)
        for (g in groups) {
            val key = normalizeGroup(g.group_name) ?: continue
            if (!map.containsKey(key)) map[key] = g
        }
        return map
    }

    private fun normalizeGroup(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return text
            .trim()
            .uppercase()
            .replace(Regex("\\s+"), "")
            .replace("_", "-")
            .replace(Regex("[^A-Z0-9-]"), "")
            .replace(Regex("-+"), "-")
            .trim('-')
            .takeIf { it.isNotBlank() }
    }

    private fun copyImageToNotesDir(uri: Uri): String? {
        return try {
            val context = getApplication<Application>().applicationContext
            val notesDir = File(context.filesDir, "notes_images").apply { mkdirs() }
            val ext = when (context.contentResolver.getType(uri)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val file = File(notesDir, "note_${System.currentTimeMillis()}_${(1000..9999).random()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
