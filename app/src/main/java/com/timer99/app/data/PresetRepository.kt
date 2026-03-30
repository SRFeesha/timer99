package com.timer99.app.data

import kotlinx.coroutines.flow.Flow

interface PresetRepository {
    val presets: Flow<List<Preset>>
    suspend fun insert(preset: Preset)
    suspend fun delete(preset: Preset)
}

class DefaultPresetRepository(private val dao: PresetDao) : PresetRepository {
    override val presets: Flow<List<Preset>> = dao.getAll()
    override suspend fun insert(preset: Preset) = dao.insert(preset)
    override suspend fun delete(preset: Preset) = dao.delete(preset)
}
