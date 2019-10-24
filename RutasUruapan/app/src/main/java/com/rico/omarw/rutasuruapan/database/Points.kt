package com.rico.omarw.rutasuruapan.database

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "Points")
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
}