package com.rico.omarw.rutasuruapan.Database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.huma.room_for_asset.RoomAsset

@Database(entities = [Routes::class, Points::class], version = 2)
abstract class AppDatabase: RoomDatabase() {
    abstract fun routesDAO(): RouteDAO



    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase?{
            if(INSTANCE == null){
                synchronized(AppDatabase::class){
                    INSTANCE = RoomAsset.databaseBuilder(context.applicationContext,  AppDatabase::class.java, "routes.db")
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