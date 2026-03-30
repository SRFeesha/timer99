package com.timer99.app.data

import kotlinx.coroutines.flow.Flow

class PresetRepository(private val dao: PresetDao) {
    val presets: Flow<List<Preset>> = dao.getAll()

    suspend fun insert(preset: Preset) = dao.insert(preset)

    suspend fun delete(preset: Preset) = dao.delete(preset)
}
