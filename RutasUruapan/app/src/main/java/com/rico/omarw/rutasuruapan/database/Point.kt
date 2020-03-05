package com.rico.omarw.rutasuruapan.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Entity(tableName = "Points", indices = [
    Index(name = "index_Points_latRouteId", value = ["lat","routeId"], unique = false),
    Index(name = "index_Points_routeIdLat", value = ["routeId","lat"], unique = false),
    Index(name = "index_Points_routeIdNumberDistNextPoint", value = ["routeId", "number", "distanceToNextPoint"], unique = false)])
data class Point(var routeId: Long,
                  var lat: Double,
                  var lng: Double,
                  var number: Int,
                  var distanceToNextPoint: Int){
    public constructor():this(0,0.0,0.0,0,0)

    // pointId, routeId, lat, lng, number, distanceToNextPoint
    public constructor(pointId: Int, routeId: Int, lat: Double, lng: Double, number: Int, distanceToNextPoint: Int)
            : this(routeId.toLong(), lat, lng, number, distanceToNextPoint.toInt()){
//        this.pointId = pointId
    }


    @PrimaryKey (autoGenerate = true)
    var pointId: Long = 0

    fun getLatLng() = LatLng(lat, lng)

    fun bearingTo(lat2: Double, lng2: Double): Float{
        val dLng = lng2 - lng
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat)*sin(lat2) - sin(lat)*cos(lat2)*cos(dLng)
        return ((Math.toDegrees(atan2(y, x))+360)%360).toFloat()
    }
}