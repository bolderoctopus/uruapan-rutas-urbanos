package com.rico.omarw.rutasuruapan.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Points(var routeId: Long,
                  var lat: Double,
                  var lng: Double){

    @PrimaryKey (autoGenerate = true)
    var pointId: Long = 0
}