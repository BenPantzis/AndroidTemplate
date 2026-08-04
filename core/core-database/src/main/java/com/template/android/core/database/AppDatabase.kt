package com.template.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.template.android.core.database.entity.PlaceholderEntity

// Add @TypeConverters(Converters::class) here once Converters.kt has real converter methods.
@Database(
    entities = [PlaceholderEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        // Declare migrations here as the schema version increases:
        // val MIGRATION_1_2 = Migration(1, 2) { db -> db.execSQL("ALTER TABLE ...") }
    }
}
