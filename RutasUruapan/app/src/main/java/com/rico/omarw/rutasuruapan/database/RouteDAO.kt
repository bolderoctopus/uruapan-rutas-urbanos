package com.rico.omarw.rutasuruapan.database

import androidx.room.*

@Dao
interface RouteDAO{

    @Insert
    fun insertRoutes(routes: Array<Route>)

    @Insert
    fun insertPoints(routes: List<Point>)

    @Insert
    fun insertRoute(route: Route): Long

    @Query("SELECT * FROM Points WHERE routeId = :routeId ORDER BY pointId")
    fun getPointsFrom(routeId: Long): List<Point>

    @Query("SELECT * FROM  Routes")
    suspend fun getRoutes(): List<Route>

    @Query("SELECT * FROM  Routes WHERE routeId IN (:routesIds)")
    suspend fun getRoutes(routesIds: List<Long>): List<Route>

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
            "ORDER BY ((:latitude - lat)*(:latitude - lat) + (:longitude - lng)*(:longitude - lng)) ASC " +
            "LIMIT 1")
    suspend fun getNearestPointTo(latitude: Double, longitude: Double, rId: Long): Point

    @Query("SELECT * FROM Routes WHERE routeId = :rId")
    suspend fun getRoute(rId: Long): Route

    @Transaction
    suspend  fun getRouteDist(rId: Long, startPoint: Int, endPoint: Int): Int{
        return if (startPoint > endPoint) routeDistA(rId, startPoint, endPoint)
                else routeDistA(rId, startPoint, endPoint)
    }

    @Query("SELECT SUM(distanceToNextPoint) FROM Points WHERE routeId = :rId AND (number >= :startPoint OR number <= :endPoint)")
    suspend fun routeDistA(rId: Long, startPoint: Int, endPoint: Int): Int

    @Query("SELECT SUM(distanceToNextPoint) FROM Points WHERE routeId = :rId AND number BETWEEN :startPoint AND :endPoint")
    suspend fun routeDistB(rId: Long, startPoint: Int, endPoint: Int): Int
}