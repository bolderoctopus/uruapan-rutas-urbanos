package com.rico.omarw.rutasuruapan.Database

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = arrayOf(Routes::class, Points::class), version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun routesDAO(): RouteDAO



    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase?{
            if(INSTANCE == null){
                synchronized(AppDatabase::class){
                    INSTANCE = Room.databaseBuilder(context.applicationContext,  AppDatabase::class.java, "routes.db")
                            .addCallback(object: RoomDatabase.Callback(){
                                override fun onCreate(db: SupportSQLiteDatabase) {

                                }
                            })
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