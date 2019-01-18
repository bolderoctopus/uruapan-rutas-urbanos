package com.rico.omarw.rutasuruapan.Database

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface RouteDAO{

    @Insert
    fun insertRoutes(routes: Array<Routes>)

    @Insert
    fun insertPoints(routes: List<Points>)

    @Insert
    fun insertRoute(route: Routes): Long

    @Query("SELECT * FROM Points WHERE routeId = :routeId ORDER BY pointId")
    fun getPointsFrom(routeId: Long): List<Points>

    @Query("SELECT * FROM  Routes")
    fun getRoutes(): List<Routes>

    @Query("DELETE FROM Points")
    fun deleteAllPoints()

    @Query("DELETE FROM Routes")
    fun deleteAllRoutes()
}