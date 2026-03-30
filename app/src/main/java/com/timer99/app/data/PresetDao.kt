package com.timer99.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY id ASC")
    fun getAll(): Flow<List<Preset>>

    @Insert
    suspend fun insert(preset: Preset)

    @Delete
    suspend fun delete(preset: Preset)
}
