package com.aitu.navigator.features.today

import com.aitu.navigator.data.model.Lesson

data class Location(
    val classroom: String,
    val lecturer: String
)

data class NormalizedSlot(
    val day: String,
    val time: String,
    val discipline: String,
    val type: String,
    val locations: List<Location>
)

fun normalizeLessons(lessons: List<Lesson>): List<NormalizedSlot> {
    fun norm(s: String) = s.trim().lowercase()

    return lessons
        .groupBy { l ->
            // Ключ: день + время + предмет + тип
            "${norm(l.day)}|${l.time.replace(" ", "")}|${norm(l.discipline)}|${norm(l.type)}"
        }
        .map { (_, group) ->
            val first = group.first()

            val locations = group
                .map { Location(classroom = it.classroom.trim(), lecturer = it.lecturer.trim()) }
                .distinctBy { it.classroom } // схлопнуть дубли аудиторий

            NormalizedSlot(
                day = first.day.trim(),
                time = first.time.replace(" ", ""),
                discipline = first.discipline.trim(),
                type = first.type.trim(),
                locations = locations
            )
        }
        .sortedBy { it.time } // сортировка по времени
}