package com.timer99.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.timer99.app.model.Palette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsStore by preferencesDataStore(name = "user_prefs")

private val KEY_PALETTE = stringPreferencesKey("palette")

val Context.selectedPaletteFlow: Flow<Palette>
    get() = userPrefsStore.data.map { prefs ->
        val name = prefs[KEY_PALETTE]
        Palette.entries.find { it.name == name } ?: Palette.DEFAULT_PALETTE
    }

suspend fun Context.savePalette(palette: Palette) {
    userPrefsStore.edit { prefs -> prefs[KEY_PALETTE] = palette.name }
}
