package com.aitu.navigator.features.today

import com.aitu.navigator.data.model.Lesson
import org.junit.Assert.*
import org.junit.Test

class NormalizeLessonsTest {
    @Test fun duplicatesAndRoomVariantsAreMergedButDifferentClassesStaySeparate() {
        val lessons = listOf(
            Lesson(" Monday ", "09:00 - 10:00", " Algorithms ", "101", "Lecture", "Teacher A"),
            Lesson("monday", "09:00-10:00", "algorithms", " 102 ", "lecture", "Teacher B"),
            Lesson("Monday", "09:00-10:00", "Algorithms", "101", "Lecture", "Teacher A"),
            Lesson("Monday", "09:00-10:00", "Databases", "101", "Lecture", "Teacher A")
        )
        val slots = normalizeLessons(lessons)
        assertEquals(2, slots.size)
        val algorithms = slots.first { it.discipline == "Algorithms" }
        assertEquals("09:00-10:00", algorithms.time)
        assertEquals(listOf(Location("101", "Teacher A"), Location("102", "Teacher B")), algorithms.locations)
    }

    @Test fun emptyScheduleIsValid() {
        assertTrue(normalizeLessons(emptyList()).isEmpty())
    }
}
