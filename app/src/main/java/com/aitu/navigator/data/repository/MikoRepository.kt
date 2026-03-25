package com.aitu.navigator.data.repository

import android.content.Context
import com.aitu.navigator.data.model.GroupSchedule
import com.aitu.navigator.data.parser.AssetJsonReader
import com.aitu.navigator.data.parser.MikoParser

class MikoRepository(private val context: Context) {
    private val reader = AssetJsonReader(context)
    private val parser = MikoParser()
    @Volatile
    private var cached: Result<List<GroupSchedule>>? = null

    fun loadGroups(): Result<List<GroupSchedule>> {
        cached?.let { return it }
        val loaded = runCatching {
            val raw = reader.readText("Miko.json")
            parser.parseGroups(raw)
        }
        cached = loaded
        return loaded
    }
}