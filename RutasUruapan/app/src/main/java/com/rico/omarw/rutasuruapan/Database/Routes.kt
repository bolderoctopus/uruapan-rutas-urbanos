package com.rico.omarw.rutasuruapan.Database

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey

@Entity
data class Routes (var name: String,
                   var line: String,
                   var color: String){
    @PrimaryKey (autoGenerate = true)
    var routeId: Long = 0

}