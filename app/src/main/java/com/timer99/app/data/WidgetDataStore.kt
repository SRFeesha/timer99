package com.timer99.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "timer99_widget")

object WidgetKeys {
    val IS_RUNNING = booleanPreferencesKey("is_running")
    val IS_ALERTING = booleanPreferencesKey("is_alerting")   // true while alarm is ringing
    val REMAINING_MILLIS = longPreferencesKey("remaining_millis")
    val TOTAL_MILLIS = longPreferencesKey("total_millis")
    val PRESET_NAME = stringPreferencesKey("preset_name")
    // Serialised preset list: "Name1|300,Name2|1500". Written by MainActivity.
    val PRESETS_JSON = stringPreferencesKey("presets_json")
    // Index into PRESETS_JSON chosen via widget < > buttons.
    val SELECTED_PRESET_INDEX = intPreferencesKey("selected_preset_index")
    // URI string of the chosen alarm sound. Empty = system TYPE_ALARM default.
    val ALARM_SOUND_URI = stringPreferencesKey("alarm_sound_uri")
}

/** Encode a Preset list into the compact DataStore format. */
fun encodePresets(presets: List<Preset>): String =
    presets.joinToString(",") { "${it.name}|${it.durationSeconds}" }

/** Decode a compact DataStore string back into (name, durationSeconds) pairs. */
fun decodePresets(encoded: String): List<Pair<String, Int>> {
    if (encoded.isBlank()) return emptyList()
    return encoded.split(",").mapNotNull { entry ->
        val parts = entry.split("|")
        if (parts.size == 2) Pair(parts[0], parts[1].toIntOrNull() ?: return@mapNotNull null)
        else null
    }
}
