package com.rico.omarw.rutasuruapan.models

import com.google.android.gms.maps.model.Polyline
import com.rico.omarw.rutasuruapan.database.Routes

class RouteModel (private val routeDb : Routes){
    var isDrawed: Boolean = false
    val name: String = routeDb.name
    val color: String = routeDb.color
    val id: Long = routeDb.routeId
    var polyline: Polyline? = null
    var arrowPolylines: ArrayList<Polyline>? = null

    public fun showLines(show: Boolean){
        polyline?.isVisible = show
        arrowPolylines?.forEach {
            it.isVisible = show
        }
        isDrawed = show
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || javaClass != other.javaClass) return false
        val model = other as RouteModel

        if(id != model.id) return false;
        return name.equals(model.name) && color.equals(model.color)
    }

}