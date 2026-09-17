package com.studyspace.timer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * App-wide Room database. Single table for now ([StudySessionEntity]) —
 * DataStore (Stage 8) handles settings/theme instead of a second table here.
 *
 * No migrations exist yet since this is the first schema version; a future
 * stage that changes [StudySessionEntity]'s columns must add a real
 * migration rather than relying on destructive fallback, since that would
 * silently delete the user's study history.
 */
@Database(entities = [StudySessionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studyspace_timer.db"
                ).build().also { instance = it }
            }
    }
}
