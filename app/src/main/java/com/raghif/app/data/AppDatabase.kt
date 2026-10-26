package com.raghif.app.data

import android.content.Context
import androidx.room.*

@Database(entities=[Bakery::class, DailyRecord::class], version=1, exportSchema=false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bakeryDao(): BakeryDao
    abstract fun dailyDao(): DailyDao
    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "raghif.db").build().also { instance = it }
        }
    }
}
