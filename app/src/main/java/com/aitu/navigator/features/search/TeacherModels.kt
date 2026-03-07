package com.aitu.navigator.features.search

import com.aitu.navigator.data.model.Lesson

data class TeacherEntry(
    val key: String,
    val displayName: String,
    val lessonCount: Int,
    val lessons: List<Lesson>
)

private fun normalizeTeacherKey(name: String): String {
    return name
        .lowercase()
        .replace(Regex("[()\\-,.—.]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun buildTeacherIndex(groups: List<com.aitu.navigator.data.model.GroupSchedule>): List<TeacherEntry> {
    val allLessons = groups.flatMap { it.schedule }

    val grouped = allLessons
        .filter { it.lecturer.isNotBlank() }
        .groupBy { normalizeTeacherKey(it.lecturer) }

    return grouped.map { (key, lessons) ->
        val displayName = lessons
            .groupingBy { it.lecturer.trim() }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: lessons.first().lecturer

        TeacherEntry(
            key = key,
            displayName = displayName,
            lessonCount = lessons.size,
            lessons = lessons
        )
    }.sortedByDescending { it.lessonCount }
}