package com.rico.omarw.rutasuruapan

import com.google.android.gms.maps.model.Polyline
import com.rico.omarw.rutasuruapan.database.Routes

class RouteModel (private val routeDb : Routes){
    var isDrawed: Boolean = false
    val name: String
    val color: String
    val id: Long
    var polyline: Polyline? = null

    init {
        name = routeDb.name;
        color = routeDb.color;
        id = routeDb.routeId
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || javaClass != other.javaClass) return false
        val model = other as RouteModel

        if(id != model.id) return false;
        return name.equals(model.name) && color.equals(model.color)
    }

}