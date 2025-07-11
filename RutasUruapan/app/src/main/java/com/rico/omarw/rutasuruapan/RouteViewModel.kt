package com.rico.omarw.rutasuruapan

import android.util.Log
import androidx.lifecycle.ViewModel
import com.rico.omarw.rutasuruapan.database.RouteDAO
import com.rico.omarw.rutasuruapan.models.RouteModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val routeDAO: RouteDAO
) : ViewModel() {

    init {
        Log.d("DebugTag", "RouteViewModel init")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("DebugTag", "RouteViewModel onCleared")
    }

    suspend fun getRoutes(): List<RouteModel> {
        return withContext(Dispatchers.IO) {
            routeDAO.getRoutes().map { r ->
                RouteModel(r)
            }
        }
    }


}