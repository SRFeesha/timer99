package com.timer99.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val durationSeconds: Int,
)
