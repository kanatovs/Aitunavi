package com.aitu.navigator.data.repository

import android.content.Context
import com.aitu.navigator.features.atlas.AtlasResult
import org.json.JSONArray

class AtlasRepository(private val context: Context) {

    // ⚠️ названия файлов в assets должны совпадать
    // положи туда: atlas_rooms.json и atlas_pois.json
    private val roomsJson by lazy { loadAsset("atlas_rooms.json") }
    private val poisJson by lazy { loadAsset("atlas_pois.json") }

    fun search(query: String): List<AtlasResult> {
        val q = query.trim().uppercase()
        val out = ArrayList<AtlasResult>()

        // Rooms
        roomsJson?.let {
            val arr = JSONArray(it)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val room = o.optString("room", o.optString("name", ""))
                val building = o.optString("building", o.optString("block", ""))
                val floor = o.optInt("floor", -1).takeIf { f -> f > 0 }
                val title = if (room.isNotBlank()) room else "Room"
                val subtitle = buildString {
                    if (building.isNotBlank()) append(building)
                    if (floor != null) append(" • floor $floor")
                }

                val hay = (room + " " + building + " " + subtitle).uppercase()
                if (hay.contains(q)) {
                    out += AtlasResult(
                        title = title,
                        subtitle = subtitle,
                        roomOrPoi = room.ifBlank { title },
                        building = building.ifBlank { null },
                        floor = floor
                    )
                }
            }
        }

        // POI
        poisJson?.let {
            val arr = JSONArray(it)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val name = o.optString("name", "")
                val building = o.optString("building", o.optString("block", ""))
                val floor = o.optInt("floor", -1).takeIf { f -> f > 0 }
                val type = o.optString("type", "POI")

                val hay = (name + " " + type + " " + building).uppercase()
                if (hay.contains(q)) {
                    out += AtlasResult(
                        title = name.ifBlank { type },
                        subtitle = listOf(type, building, floor?.let { "floor $it" }).filterNotNull().joinToString(" • "),
                        roomOrPoi = name.ifBlank { type },
                        building = building.ifBlank { null },
                        floor = floor
                    )
                }
            }
        }

        return out.take(50)
    }

    private fun loadAsset(name: String): String? {
        return try {
            context.assets.open(name).bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        }
    }
}