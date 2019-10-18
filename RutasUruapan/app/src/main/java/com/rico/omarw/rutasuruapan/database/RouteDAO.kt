package com.rico.omarw.rutasuruapan.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RouteDAO{

    @Insert
    fun insertRoutes(routes: Array<Routes>)

    @Insert
    fun insertPoints(routes: List<Point>)

    @Insert
    fun insertRoute(route: Routes): Long

    @Query("SELECT * FROM Points WHERE routeId = :routeId ORDER BY pointId")
    fun getPointsFrom(routeId: Long): List<Point>

    @Query("SELECT * FROM  Routes")
    suspend fun getRoutes(): List<Routes>

    @Query("SELECT * FROM  Routes WHERE routeId IN (:routesIds)")
    suspend fun getRoutes(routesIds: List<Long>): List<Routes>

    @Query("DELETE FROM Points")
    fun deleteAllPoints()

    @Query("DELETE FROM Routes")
    fun deleteAllRoutes()

    @Query("SELECT distinct routeId FROM Points " +
            "WHERE Points.lat BETWEEN (:latitude - :distance) AND (:latitude + :distance) " +
                "AND Points.lng BETWEEN (:longitude - :distance) AND (:longitude + :distance)")
    suspend fun getRoutesIntercepting(distance: Double, latitude: Double, longitude: Double): List<Long>

    @Query("SELECT *" +
            "FROM Points " +
            "WHERE routeId = :rId " +
//            "ORDER BY (ABS(19.41402 - lat) + ABS( -102.0606177 - lng)) ASC")
            "ORDER BY (ABS(:latitude - lat) + ABS(:longitude - lng)) ASC ")
            //"LIMIT 1")
//    suspend fun getNearestPointTo(rId: Long): Point
    suspend fun getNearestPointTo(latitude: Double, longitude: Double, rId: Long): Point
}