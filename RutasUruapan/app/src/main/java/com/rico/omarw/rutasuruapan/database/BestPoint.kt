package com.rico.omarw.rutasuruapan.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bestPoints")
data class BestPoint(
        @PrimaryKey(autoGenerate = false)var pointId: Long,
                    var routeId: Long,
                     var lat: Double,
                     var lng: Double,
                     var number: Int,
                     var distanceToNextPoint: Int,
                     var wd: Double?,
                     var rd: Double?,
                     var rt: Double?)