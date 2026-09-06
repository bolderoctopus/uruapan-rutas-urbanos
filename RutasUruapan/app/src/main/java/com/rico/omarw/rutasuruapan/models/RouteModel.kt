package com.rico.omarw.rutasuruapan.models

import android.util.SparseArray
import com.google.android.gms.maps.model.*
import com.rico.omarw.rutasuruapan.database.Point
import com.rico.omarw.rutasuruapan.database.Route
import androidx.core.util.size

class RouteModel(val routeDb: Route) {
    var mainSegment: Polyline? = null
    var secondarySegment: Polyline? = null
    var isDrawn: Boolean = false
    val name: String = routeDb.name
    val color: String = routeDb.color
    val id: Long = routeDb.routeId
    var polyline: Polyline? = null
    var directionalMarkers: SparseArray<Iterable<Marker>>? = null

    var startPoint: Point? = null
    var endPoint: Point? = null
    var walkDist: Double? = null
    var totalDist: Double? = null

    @Deprecated("No longer needed since a route result now includes a startCap")
    var startMarker: Marker? = null

    @Deprecated("No longer needed since a route result now includes an endCap")
    var endMarker: Marker? = null
    var mainSegmentMarkers: List<Marker>? = null

    fun setVisibility(visible: Boolean) {
        startMarker?.isVisible = visible
        endMarker?.isVisible = visible
        mainSegment?.isVisible = visible
        secondarySegment?.isVisible = visible
        polyline?.isVisible = visible
        mainSegmentMarkers?.forEach { it.isVisible = visible }

        isDrawn = visible
    }

    fun remove() {
        startMarker?.remove()
        endMarker?.remove()
        mainSegment?.remove()
        secondarySegment?.remove()
        polyline?.remove()
        mainSegmentMarkers?.forEach { it.remove() }

        directionalMarkers?.let { markers ->
            for (x in 0 until markers.size) {
                markers.valueAt(x).forEach { it.remove() }
            }
        }

        isDrawn = false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val model = other as RouteModel

        if (id != model.id) return false
        return name == model.name && color == model.color
    }

    fun getMainSegment(points: List<Point>) =
        getRouteSegment(startPoint, endPoint, points)

    fun getSecondarySegment(points: List<Point>) =
        getRouteSegment(endPoint, startPoint, points)


    private fun getRouteSegment(start: Point?, end: Point?, points: List<Point>): Iterable<LatLng> {
        if (start == null || end == null) return emptyList()

        val startNumber = start.number
        val endNumber = end.number

        val segment = ArrayList<LatLng>()

        if (startNumber > endNumber) {
            val part1 = points.filter { it.number >= startNumber }.sortedBy { it.number }
            val part2 = points.filter { it.number <= endNumber }.sortedBy { it.number }
            (part1 + part2).forEach {
                segment.add(LatLng(it.lat, it.lng))
            }
        } else {
            points.filter { it.number in startNumber..endNumber }.sortedBy { it.number }.forEach {
                segment.add(LatLng(it.lat, it.lng))
            }
        }
        return segment
    }

    fun getMainSegmentPoints(points: List<Point>) =
        getRouteSegmentPoints(startPoint, endPoint, points)

    private fun getRouteSegmentPoints(start: Point?, end: Point?, points: List<Point>): List<Point> {
        if (start == null || end == null) return emptyList()

        val startNumber = start.number
        val endNumber = end.number


        return if (startNumber > endNumber) {
            val part1 = points.filter { it.number >= startNumber }.sortedBy { it.number }
            val part2 = points.filter { it.number <= endNumber }.sortedBy { it.number }
            (part1 + part2)
        } else {
            points.filter { it.number in startNumber..endNumber }.sortedBy { it.number }
        }
    }

    companion object {
        val dashedPattern: List<PatternItem> = ArrayList<PatternItem>().apply {
            add(Gap(40f))
            add(Dash(20f))
        }
    }

}