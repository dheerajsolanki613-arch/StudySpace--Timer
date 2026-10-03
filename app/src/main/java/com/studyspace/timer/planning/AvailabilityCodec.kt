package com.studyspace.timer.planning

import org.json.JSONArray
import org.json.JSONException
import java.time.DayOfWeek

/**
 * Pure JSON read/write for a list of [AvailabilityBlock], kept separate from
 * [AvailabilityRepository] (which needs DataStore/Context) so this part is
 * plain-JVM unit-testable — same split Phase 13 uses between
 * `StudyDataExport.kt`/`StudyDataImport.kt` (pure) and
 * `DataTransferRepository.kt` (Android/Room).
 */

fun buildAvailabilityJson(blocks: List<AvailabilityBlock>): String {
    val array = JSONArray()
    blocks.forEach { block ->
        array.put(
            org.json.JSONObject()
                .put("dayOfWeek", block.dayOfWeek.name)
                .put("startMinuteOfDay", block.startMinuteOfDay)
                .put("endMinuteOfDay", block.endMinuteOfDay)
        )
    }
    return array.toString()
}

/**
 * Parses [json] back into blocks, dropping any entry that's malformed or out
 * of range rather than failing the whole read — this is a small local
 * preference file the app itself always writes, but a corrupt or
 * hand-edited value should degrade to "fewer blocks" rather than crash
 * Settings/Planner on launch.
 */
fun parseAvailability(json: String): List<AvailabilityBlock> {
    val array = try {
        JSONArray(json)
    } catch (e: JSONException) {
        return emptyList()
    }
    return (0 until array.length()).mapNotNull { i ->
        val obj = array.optJSONObject(i) ?: return@mapNotNull null
        val day = DayOfWeek.values().firstOrNull { it.name == obj.optString("dayOfWeek", "") }
            ?: return@mapNotNull null
        val start = obj.optInt("startMinuteOfDay", -1)
        val end = obj.optInt("endMinuteOfDay", -1)
        if (start !in 0..1439 || end !in 1..1440 || end <= start) return@mapNotNull null
        AvailabilityBlock(day, start, end)
    }
}
