package com.timer99.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Preset::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timer99.db",
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO presets (name, durationSeconds) VALUES ('Pomodoro', 1500)")
                        db.execSQL("INSERT INTO presets (name, durationSeconds) VALUES ('Short Break', 300)")
                        db.execSQL("INSERT INTO presets (name, durationSeconds) VALUES ('Gym Rest', 90)")
                    }
                }).build().also { INSTANCE = it }
            }
    }
}
