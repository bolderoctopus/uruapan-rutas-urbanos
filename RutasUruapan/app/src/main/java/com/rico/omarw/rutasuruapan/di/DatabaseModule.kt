package com.rico.omarw.rutasuruapan.di

import android.content.Context
import androidx.room.Room
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.database.AppDatabase.Companion.MIGRATION_1_2
import com.rico.omarw.rutasuruapan.database.AppDatabase.Companion.MIGRATION_2_3
import com.rico.omarw.rutasuruapan.database.AppDatabase.Companion.MIGRATION_3_4
import com.rico.omarw.rutasuruapan.database.RouteDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context.applicationContext,  AppDatabase::class.java, "routes_database")
            .createFromAsset("databases/pre_packaged_routes.db")
            .fallbackToDestructiveMigration(true)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideRouteDao(appDb: AppDatabase): RouteDAO = appDb.routesDAO()
}