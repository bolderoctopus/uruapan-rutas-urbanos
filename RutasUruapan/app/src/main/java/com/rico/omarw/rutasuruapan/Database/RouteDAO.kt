package com.rico.omarw.rutasuruapan.Database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

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

    @Query("SELECT * FROM Routes " +
            "INNER JOIN Points ON Routes.routeId = Points.routeId " +
            "WHERE Points.lat BETWEEN (:latitude - :distance) AND (:latitude + :distance) " +
                "AND Points.lng BETWEEN (:longitude - :distance) AND (:longitude + :distance)")
    fun getRoutesIntercepting(distance: Double, latitude: Double, longitude: Double): List<Routes>
}