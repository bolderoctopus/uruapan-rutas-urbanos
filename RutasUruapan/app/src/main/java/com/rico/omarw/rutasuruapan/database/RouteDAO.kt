package com.rico.omarw.rutasuruapan.database

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
    suspend fun getRoutes(): List<Routes>

    @Query("SELECT * FROM  Routes WHERE routeId IN (:routesIds)")
    suspend fun getRoutes(routesIds: List<Long>): List<Routes>

    @Query("DELETE FROM Points")
    fun deleteAllPoints()

    @Query("DELETE FROM Routes")
    fun deleteAllRoutes()

    @Query("SELECT distinct routeId FROM Points " +
            //"INNER JOIN Points ON Routes.routeId = Points.routeId " +
            "WHERE Points.lat BETWEEN (:latitude - :distance) AND (:latitude + :distance) " +
                "AND Points.lng BETWEEN (:longitude - :distance) AND (:longitude + :distance)")
    suspend fun getRoutesIntercepting(distance: Double, latitude: Double, longitude: Double): List<Long>
}