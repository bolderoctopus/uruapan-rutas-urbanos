package com.rico.omarw.rutasuruapan

import com.google.android.gms.maps.model.Polyline
import com.rico.omarw.rutasuruapan.Database.Routes

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

}