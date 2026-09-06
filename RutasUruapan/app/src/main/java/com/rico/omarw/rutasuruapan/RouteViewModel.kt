package com.rico.omarw.rutasuruapan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rico.omarw.rutasuruapan.database.RouteDAO
import com.rico.omarw.rutasuruapan.models.RouteModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale.getDefault

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val routeDAO: RouteDAO
) : ViewModel() {

    private var routes: List<RouteModel> = emptyList()
    private val _filterableRoutes: MutableStateFlow<List<RouteModel>> = MutableStateFlow(routes)
    val filterableRoutes: StateFlow<List<RouteModel>> = _filterableRoutes

    private val _drawnRoutes: MutableStateFlow<Set<RouteModel>> = MutableStateFlow(emptySet())
    val drawnRoutes: StateFlow<Set<RouteModel>> = _drawnRoutes

    init {
        viewModelScope.launch {
            fetchRoutes()
        }
    }

    private suspend fun fetchRoutes() {
        withContext(Dispatchers.IO) {
            routes = routeDAO.getRoutes().map { r ->
                RouteModel(r)
            }
            _filterableRoutes.value = routes
        }
    }

    fun filterRoutes(query: String) {
        viewModelScope.launch {
            _filterableRoutes.value = routes.filter {
                val name = it.name.lowercase(getDefault())
                name.contains(query) || it.routeDb.shortName.contains(query)
            }

        }
    }

    fun addDrawnRoute(route: RouteModel) {
        _drawnRoutes.value = _drawnRoutes.value + route
    }

    fun removeDrawnRoute(route: RouteModel) {
        _drawnRoutes.value = _drawnRoutes.value - route
    }

    fun clearDrawnRoutes() {
        _drawnRoutes.value.forEach { it.remove() }
        _drawnRoutes.value = emptySet()
    }


}