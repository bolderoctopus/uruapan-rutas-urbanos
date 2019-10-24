package com.rico.omarw.rutasuruapan.database

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Route::class, Point::class,BestPoint::class], version = 2)
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
                            .addMigrations(MIGRATION_1_2)
                            .build()
                }
            }
            return INSTANCE
        }

        private val MIGRATION_1_2 = object: Migration(1,2){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE bestPoints( " +
                        "pointId INTEGER PRIMARY KEY NOT NULL, routeId INTEGER NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, number INTEGER NOT NULL, distanceToNextPoint INTEGER NOT NULL, " +
                        "wd REAL NULL, rd REAL NULL, betterness REAL NULL);")
            }
        }

        fun destroyInstace() {
            INSTANCE = null
        }
    }
}