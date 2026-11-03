package com.example.re_pro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.re_pro.data.database.dao.CacheMetadataDao
import com.example.re_pro.data.database.entities.CacheMetadataEntity

@Database(
    entities = [CacheMetadataEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheMetadataDao(): CacheMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cache_metadata ADD COLUMN album TEXT DEFAULT '' NOT NULL")
                db.execSQL("ALTER TABLE cache_metadata ADD COLUMN genre TEXT DEFAULT '' NOT NULL")
                db.execSQL("ALTER TABLE cache_metadata ADD COLUMN contentUri TEXT DEFAULT '' NOT NULL")
            }
        }

        fun getMigration12(): Migration = MIGRATION_1_2

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "re_pro_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}