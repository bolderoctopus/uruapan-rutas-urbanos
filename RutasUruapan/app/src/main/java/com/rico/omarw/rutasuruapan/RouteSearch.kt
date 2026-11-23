package com.rico.omarw.rutasuruapan

import com.google.android.gms.maps.model.LatLng
import com.rico.omarw.rutasuruapan.database.RouteDAO
import com.rico.omarw.rutasuruapan.models.RouteModel
import kotlin.math.sqrt
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object RouteSearch {

    /**
     * Finds the routes that pass near both [origin] and [destination], sorted by walk distance.
     *
     * @param walkDistLimit How much the user is willing to walk between the route start/end point
     * and the given origin/destination respectively
     */
    suspend fun findRoutes(
        routesDao: RouteDAO?,
        origin: LatLng,
        destination: LatLng,
        walkDistLimit: Double
    ): List<RouteModel> {
        if (routesDao == null) return emptyList()

        val commonRoutesIds = coroutineScope {
            val routesNearOrigin = async {
                routesDao.getRoutesIntercepting(walkDistLimit, origin.latitude, origin.longitude)
            }
            val routesNearDest = async {
                routesDao.getRoutesIntercepting(walkDistLimit, destination.latitude, destination.longitude)
            }
            routesNearOrigin.await().intersect(routesNearDest.await())
        }

        return commonRoutesIds
            .mapNotNull { routeId -> resolveRoute(routesDao, routeId, origin, destination, walkDistLimit) }
            .sortedBy { it.walkDist }
    }

    /**
     * Builds the result for one route, or returns null when the route or any of its start and end
     * points can't be found, so the route is left out of the results.
     */
    private suspend fun resolveRoute(
        routesDao: RouteDAO,
        routeId: Long,
        origin: LatLng,
        destination: LatLng,
        walkDistLimit: Double
    ): RouteModel? {
        val route = routesDao.getRoute(routeId)
        val startPoint = routesDao.getNearestPointTo(routeId, origin.latitude, origin.longitude, walkDistLimit)
        val endPoint =
            routesDao.getNearestPointTo(routeId, destination.latitude, destination.longitude, walkDistLimit)
        val betterEndPoint = startPoint?.let {
            routesDao.findBestEndPoint(routeId, destination.latitude, destination.longitude, it.number, walkDistLimit)
        }
        val betterStartPoint = endPoint?.let {
            routesDao.findBestStartPoint(routeId, origin.latitude, origin.longitude, it.number, walkDistLimit)
        }
        if (route == null || betterStartPoint == null || betterEndPoint == null) return null

        val routeDist = routesDao.getRouteDist(routeId, betterStartPoint.number, betterEndPoint.number)
        val walkDist =
            distanceBetweenPoints(origin, betterStartPoint.getLatLng()) +
                distanceBetweenPoints(destination, betterEndPoint.getLatLng())

        return RouteModel(route).apply {
            this.startPoint = betterStartPoint
            this.endPoint = betterEndPoint
            this.walkDist = walkDist
            totalDist = walkDist + routeDist
        }
    }

    private fun distanceBetweenPoints(p1: LatLng, p2: LatLng): Double {
        val d1 = (p1.latitude - p2.latitude)
        val d2 = (p1.longitude - p2.longitude)
        return sqrt(d1 * d1 + d2 * d2)
    }
}
