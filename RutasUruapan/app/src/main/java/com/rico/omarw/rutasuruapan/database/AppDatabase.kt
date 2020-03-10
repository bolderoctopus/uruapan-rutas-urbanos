package com.rico.omarw.rutasuruapan.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Route::class, Point::class,BestPoint::class], version = 4)
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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .build()
                }
            }
            return INSTANCE
        }

        private val MIGRATION_1_2 = object: Migration(1,2){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE bestPoints( " +
                        "pointId INTEGER PRIMARY KEY NOT NULL, routeId INTEGER NOT NULL, lat REAL NOT NULL, lng REAL NOT NULL, number INTEGER NOT NULL, distanceToNextPoint INTEGER NOT NULL, " +
                        "wd REAL NULL, rd REAL NULL, rt REAL NULL);")
            }
        }

        private val MIGRATION_2_3 = object: Migration(2,3){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE \"new_Routes\" ( `routeId` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL, `color` TEXT NOT NULL, `shortName` TEXT NOT NULL )")
                database.execSQL("INSERT INTO new_Routes (routeId, name, color, shortName)  SELECT routeId, name, color, shortName FROM Routes")
                database.execSQL("DROP TABLE Routes")
                database.execSQL("ALTER TABLE new_Routes RENAME TO Routes")
            }
        }

        private val MIGRATION_3_4 = object: Migration(3, 4){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX `index_Points_latRouteId` on `Points` (lat, routeId)")
                database.execSQL("create index `index_Points_routeIdLat` on `Points` (routeId, lat)")
                database.execSQL("CREATE INDEX `index_Points_routeIdNumberDistNextPoint` on `Points` (routeId, number, distanceToNextPoint) ")
            }
        }

    }
}