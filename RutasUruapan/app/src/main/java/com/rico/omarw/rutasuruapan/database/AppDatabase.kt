package com.rico.omarw.rutasuruapan.database

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room

@Database(entities = [Routes::class, Point::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun routesDAO(): RouteDAO



    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase?{
            if(INSTANCE == null){
                synchronized(AppDatabase::class){
                    INSTANCE = Room.databaseBuilder(context.applicationContext,  AppDatabase::class.java, "routes_database")
                            .createFromAsset("databases/pre_packaged_routes.db")
                            .fallbackToDestructiveMigration()
                            .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstace() {
            INSTANCE = null
        }
    }
}