package com.timer99.app

import com.timer99.app.data.Preset
import com.timer99.app.data.PresetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakePresetRepository : PresetRepository {

    private val _presets = MutableSharedFlow<List<Preset>>(replay = 1)
    override val presets: Flow<List<Preset>> = _presets

    val insertedPresets = mutableListOf<Preset>()
    val deletedPresets = mutableListOf<Preset>()

    fun sendPresets(presets: List<Preset>) {
        _presets.tryEmit(presets)
    }

    override suspend fun insert(preset: Preset) {
        insertedPresets += preset
        val current = _presets.replayCache.firstOrNull() ?: emptyList()
        _presets.tryEmit(current + preset)
    }

    override suspend fun delete(preset: Preset) {
        deletedPresets += preset
        val current = _presets.replayCache.firstOrNull() ?: emptyList()
        _presets.tryEmit(current - preset)
    }
}
