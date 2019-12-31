package com.rico.omarw.rutasuruapan.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.rico.omarw.rutasuruapan.Constants.METER_IN_ANGULAR_LAT_LNG
import com.rico.omarw.rutasuruapan.Constants.WD_WEIGHT

@Dao
interface RouteDAO{

    @Insert
    fun insertRoutes(routes: Array<Route>)

    @Insert
    fun insertPoints(routes: List<Point>)

    @Insert
    fun insertRoute(route: Route): Long

    @Query("SELECT * FROM Points WHERE routeId = :routeId ORDER BY number")
    fun getPointsFrom(routeId: Long): List<Point>

    @Query("SELECT * FROM Routes")
    suspend fun getRoutes(): List<Route>

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
            " AND Points.lat BETWEEN (:latitude - :distance) AND (:latitude + :distance) " +
            "AND Points.lng BETWEEN (:longitude - :distance) AND (:longitude + :distance) " +
            "ORDER BY ((:latitude - lat)*(:latitude - lat) + (:longitude - lng)*(:longitude - lng)) ASC " +
            "LIMIT 1")
    suspend fun getNearestPointTo(rId: Long, latitude: Double, longitude: Double, distance: Double): Point

    @Query("SELECT * FROM Routes WHERE routeId = :rId")
    suspend fun getRoute(rId: Long): Route

    @Query("SELECT SUM(distanceToNextPoint) FROM Points WHERE routeId = :rId AND ( ((:startPoint > :endPoint) AND (number >= :startPoint OR number <= :endPoint)) OR ((:startPoint < :endPoint) AND (number BETWEEN :startPoint AND :endPoint)))")
    suspend  fun getRouteDist(rId: Long, startPoint: Int, endPoint: Int): Int

    @Query("INSERT INTO bestPoints " +
            "SELECT p1.pointId, p1.routeId, p1.lat, p1.lng, p1.number, p1.distanceToNextPoint, " +
            "((:latitude - p1.lat)*(:latitude - p1.lat) + (:longitude - p1.lng)*(:longitude - p1.lng))  [wd], " +
            "(SELECT SUM(p2.distanceToNextPoint)  " +
            "FROM Points p2 " +
            "WHERE p2.routeId = :rId " +
            "AND ( ((:startPoint > p1.number) AND (p2.number >= :startPoint OR p2.number <= p1.number))  " +
            "   OR ((:startPoint < p1.number) AND (p2.number BETWEEN :startPoint AND p1.number))) " +
            ") * $METER_IN_ANGULAR_LAT_LNG [rd], " +
            "null " +
            "FROM Points p1 " +
            "WHERE p1.routeId = :rId " +
            "AND p1.lat BETWEEN (:latitude - :distance) AND (:latitude + :distance) " +
            "AND p1.lng BETWEEN (:longitude - :distance) AND (:longitude + :distance) ")
    suspend fun fillResultsTableEndPoint(rId: Long, latitude: Double, longitude: Double, startPoint: Int, distance: Double)

    @Query("DELETE FROM bestPoints")
    suspend fun clearResultsTable()

    @Query("INSERT INTO bestPoints " +
            "SELECT p1.pointId, p1.routeId, p1.lat, p1.lng, p1.number, p1.distanceToNextPoint, " +
            "((:latitude - p1.lat)*(:latitude - p1.lat) + (:longitude - p1.lng)*(:longitude - p1.lng))  [wd], " +
            "(SELECT SUM(p2.distanceToNextPoint)  " +
            "FROM Points p2 " +
            "WHERE p2.routeId = :rId " +
            "AND ( ((p1.number > :endPoint) AND (p2.number >= p1.number OR p2.number <= :endPoint))  " +
            "   OR ((p1.number < :endPoint) AND (p2.number BETWEEN p1.number AND :endPoint))) " +
            ") * $METER_IN_ANGULAR_LAT_LNG [rd], " +
            "null " +
            "FROM Points p1 " +
            "WHERE p1.routeId = :rId " +
            "AND p1.lat BETWEEN (:latitude - :distance) AND (:latitude + :distance) " +
            "AND p1.lng BETWEEN (:longitude - :distance) AND (:longitude + :distance) ")
    suspend fun fillResultsTableStartPoint(rId: Long, latitude: Double, longitude: Double, endPoint: Int, distance: Double)

    @Query("UPDATE bestPoints SET rt =  (rd * rd) + (wd * $WD_WEIGHT)")
    suspend fun updateResultsTable()

    @Query("SELECT pointId, routeId, lat, lng, number, distanceToNextPoint  FROM bestPoints ORDER BY rt ASC LIMIT 1")
    suspend fun getBestPoint(): Point


    @Transaction
    suspend fun findBestStartPoint(rId: Long, latitude: Double, longitude: Double, endPoint: Int, distance: Double): Point{
        clearResultsTable()
        fillResultsTableStartPoint(rId, latitude, longitude, endPoint, distance)
        updateResultsTable()
        return getBestPoint()
    }

    @Transaction
    suspend fun findBestEndPoint(rId: Long, latitude: Double, longitude: Double, startPoint: Int, distance: Double): Point{
        clearResultsTable()
        fillResultsTableEndPoint(rId, latitude, longitude, startPoint, distance)
        updateResultsTable()
        return getBestPoint()
    }
}