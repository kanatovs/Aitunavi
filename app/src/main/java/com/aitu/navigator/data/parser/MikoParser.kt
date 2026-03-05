package com.aitu.navigator.data.parser

import com.aitu.navigator.data.model.GroupSchedule
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class MikoParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseGroups(raw: String): List<GroupSchedule> =
        json.decodeFromString(raw)
}