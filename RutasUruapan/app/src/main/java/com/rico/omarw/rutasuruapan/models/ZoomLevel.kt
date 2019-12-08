package com.rico.omarw.rutasuruapan.models

import com.google.android.gms.maps.model.Marker

/**
 * Aux class to allocate directional markers across different levels of zoom on a map.
 */
data class ZoomLevel(val zoomLevel: Int, val distanceInterval: Int,var counter: Int = 0, var markers: ArrayList<Marker>)