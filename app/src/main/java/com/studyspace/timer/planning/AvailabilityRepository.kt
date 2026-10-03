package com.studyspace.timer.planning

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.planningDataStore by preferencesDataStore(name = "planning_preferences")

/**
 * Phase 17 (Smart Study Planning). Persists the user's chosen recurring
 * weekly availability (see `SmartPlanScreen`'s preset chips) so "Suggest a
 * schedule" remembers the answer instead of asking every time. This is a
 * standing preference, not a one-off input to a single generated plan — the
 * generated suggestions themselves are never persisted here or anywhere
 * else; they only become real data once accepted into the planner as
 * ordinary [com.studyspace.timer.data.db.PlannedSessionEntity] rows.
 */
class AvailabilityRepository(private val context: Context) {

    private object Keys {
        val AVAILABILITY_JSON = stringPreferencesKey("availability_blocks_json")
    }

    val availability: Flow<List<AvailabilityBlock>> = context.planningDataStore.data.map { prefs ->
        prefs[Keys.AVAILABILITY_JSON]?.let(::parseAvailability) ?: emptyList()
    }

    suspend fun setAvailability(blocks: List<AvailabilityBlock>) {
        context.planningDataStore.edit { prefs -> prefs[Keys.AVAILABILITY_JSON] = buildAvailabilityJson(blocks) }
    }
}
