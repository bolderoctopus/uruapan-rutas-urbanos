package com.rico.omarw.rutasuruapan.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Routes (var name: String,
                   var line: String,
                   var color: String,
                   var shortName: String){
    @PrimaryKey (autoGenerate = true)
    var routeId: Long = 0

}