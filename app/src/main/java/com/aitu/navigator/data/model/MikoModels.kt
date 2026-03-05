package com.aitu.navigator.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupSchedule(
    val group_name: String,
    val schedule: List<Lesson>
)

@Serializable
data class Lesson(
    val day: String,
    val time: String,
    val discipline: String,
    val classroom: String,
    val type: String,
    val lecturer: String
)