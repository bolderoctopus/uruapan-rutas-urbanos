package com.rico.omarw.rutasuruapan.models

import android.util.SparseArray
import com.google.android.gms.maps.model.*
import com.rico.omarw.rutasuruapan.database.Point
import com.rico.omarw.rutasuruapan.database.Route

class RouteModel (val routeDb : Route){
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

    fun setVisibility(visible: Boolean){
        startMarker?.isVisible = visible
        endMarker?.isVisible = visible
        mainSegment?.isVisible = visible
        secondarySegment?.isVisible = visible
        polyline?.isVisible = visible
        mainSegmentMarkers?.forEach { it.isVisible = visible }

        isDrawn = visible
    }

    fun remove(){
        startMarker?.remove()
        endMarker?.remove()
        mainSegment?.remove()
        secondarySegment?.remove()
        polyline?.remove()
        mainSegmentMarkers?.forEach { it.remove() }

        if(directionalMarkers != null) {
            for (x in 0 until directionalMarkers!!.size()){
                directionalMarkers!!.valueAt(x).forEach { it.remove() }
            }
        }

        isDrawn = false
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || javaClass != other.javaClass) return false
        val model = other as RouteModel

        if(id != model.id) return false
        return name.equals(model.name) && color.equals(model.color)
    }

    fun getMainSegment(points: List<Point>) = getRouteSegment(startPoint!!.number, endPoint!!.number, points)
    fun getSecondarySegment(points: List<Point>) = getRouteSegment(endPoint!!.number, startPoint!!.number, points)


    private fun getRouteSegment(start: Int, end: Int, points: List<Point>): Iterable<LatLng>{
        val segment = ArrayList<LatLng>()
        if(start > end){
            val part1 = points.filter {it.number >= start}.sortedBy {it.number}
            val part2 = points.filter {it.number <= end }.sortedBy {it.number}
            (part1 + part2).forEach {
                segment.add(LatLng(it.lat, it.lng))
            }
        }else{
            points.filter {it.number in start..end}.sortedBy {it.number}.forEach{
                segment.add(LatLng(it.lat, it.lng))
            }
        }
        return segment
    }

    fun getMainSegmentPoints(points: List<Point>) = getRouteSegmentPoints(startPoint!!.number, endPoint!!.number, points)
    private fun getRouteSegmentPoints(start: Int, end: Int, points: List<Point>): List<Point>{
        return if(start > end){
            val part1 = points.filter {it.number >= start}.sortedBy {it.number}
            val part2 = points.filter {it.number <= end }.sortedBy {it.number}
            (part1 + part2)
        }else{
            points.filter {it.number in start..end}.sortedBy {it.number}
        }
    }

    companion object{
        val dashedPatter: List<PatternItem> = ArrayList<PatternItem>().apply{
            add(Gap(40f))
            add(Dash(20f))
        }
    }

}