package com.rico.omarw.rutasuruapan

import android.app.Application
import android.content.pm.PackageManager
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialized here so it's ready before fragments are restored after process death
        if(!Places.isInitialized()){
            val metaData = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
            Places.initializeWithNewPlacesApiEnabled(this, requireNotNull(metaData.getString("com.google.android.geo.API_KEY")))
        }
    }
}
