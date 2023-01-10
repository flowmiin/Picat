package com.tumblers.picat.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ImageTable::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
//    abstract fun idDao(): IdDao
    abstract fun imageDao(): ImageDao


    companion object {
        private var instance: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context): AppDatabase? {
            if(instance == null) {
                synchronized(AppDatabase::class) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "database-contacts"
                    ).build()
                }
            }
            return instance
        }
    }

}