package com.rico.omarw.rutasuruapan.Database

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class Points(var routeId: Long,
                  var lat: Double,
                  var lng: Double){

    @PrimaryKey (autoGenerate = true)
    var pointId: Long = 0
}