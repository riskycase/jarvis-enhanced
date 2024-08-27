package com.riskycase.jarvisEnhanced.database

import android.content.Context
import android.util.Log
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.riskycase.jarvisEnhanced.database.dao.FilterDao
import com.riskycase.jarvisEnhanced.database.dao.SnapDao
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.models.Snap
import com.riskycase.jarvisEnhanced.util.Constants
import java.util.concurrent.Executors

@Database(entities = [Snap::class, Filter::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun snapDao(): SnapDao
    abstract fun filterDao(): FilterDao

    companion object {

        @Volatile
        private var AppDatabaseInstance: AppDatabase? = null

        private var MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Filter ADD COLUMN packageName TEXT NOT NULL DEFAULT 'com.snapchat.android'")
            }
        }

        private fun buildDatabase(context: Context) = Room
            .databaseBuilder(context, AppDatabase::class.java, Constants.databaseName)
            .addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Executors
                        .newSingleThreadExecutor()
                        .execute {
                            Runnable {
                                val filterDao = getDatabase(context)
                                    .filterDao()
                                filterDao.reset()
                            }
                        }
                }
            })
            .addMigrations(MIGRATION_1_2)
            .build()

        fun getDatabase(context: Context): AppDatabase {
            return AppDatabaseInstance
                ?: synchronized(this) {
                    return buildDatabase(context).also { AppDatabaseInstance = it }
                }
        }

    }

}