package com.aitu.navigator.data.repository

import android.content.Context
import com.aitu.navigator.data.model.GroupSchedule
import com.aitu.navigator.data.parser.AssetJsonReader
import com.aitu.navigator.data.parser.MikoParser

class MikoRepository(private val context: Context) {
    private val reader = AssetJsonReader(context)
    private val parser = MikoParser()

    fun loadGroups(): Result<List<GroupSchedule>> = runCatching {
        val raw = reader.readText("Miko.json")
        parser.parseGroups(raw)
    }
}