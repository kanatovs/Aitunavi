package com.aitu.navigator.data.parser

import android.content.Context

class AssetJsonReader(private val context: Context) {
    fun readText(fileName: String): String =
        context.assets.open(fileName).bufferedReader().use { it.readText() }
}