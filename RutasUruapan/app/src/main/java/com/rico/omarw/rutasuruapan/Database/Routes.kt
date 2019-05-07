package com.rico.omarw.rutasuruapan.Database

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class Routes (var name: String,
                   var line: String,
                   var color: String){
    @PrimaryKey (autoGenerate = true)
    var routeId: Long = 0

}