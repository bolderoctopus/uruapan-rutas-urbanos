package com.rico.omarw.rutasuruapan

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rico.omarw.rutasuruapan.adapters.RouteListFilterableAdapter
import com.rico.omarw.rutasuruapan.models.RouteModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale.getDefault

@AndroidEntryPoint
class AllRoutesFragment : Fragment(), RouteListFilterableAdapter.DrawRouteListener {
    private val routeViewModel: RouteViewModel by activityViewModels()

    private val comparator =
        Comparator<RouteModel> { routeModel1: RouteModel, routeModel2: RouteModel ->
            (routeModel1.color + routeModel1.name).compareTo((routeModel2.color + routeModel2.name))
        }
    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextChange(query: String?): Boolean {
            if (query != null) {
                val filteredList = filter(routeModels, query)
                adapter.replaceAll(filteredList)
                recyclerView.scrollToPosition(0)
            }
            return true
        }

        override fun onQueryTextSubmit(p0: String?) = false
    }

    private lateinit var adapter: RouteListFilterableAdapter
    private lateinit var routeModels: List<RouteModel>
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView

    private lateinit var interactionsListener: InteractionsInterface
    private var shouldDisplayHowToShowRouteDialog = true

    //todo: pasar esto al viewmodel?? se recupera bien luego de una recreacion de la actividad? fragmento?
    private var drawnRoutes: MutableSet<RouteModel> = mutableSetOf()

    fun enableNestedScrolling(enable: Boolean) {//todo: is his really necessary?
        recyclerView.isNestedScrollingEnabled = enable
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_routes, container, false)

        recyclerView = view.findViewById(R.id.recyclerView_all_routes)
        searchView = view.findViewById(R.id.searchview)
        searchView.setOnQueryTextListener(queryTextListener)
        removeSearchViewBackground()

        shouldDisplayHowToShowRouteDialog =//todo: review initialization, if it showsFirst in Results fragments you see it again here
            InformativeDialogs.shouldDisplayHowToShowRouteDialog(layoutInflater.context)

        if (shouldDisplayHowToShowRouteDialog)
            displayDialogWhenRecyclerShown()


        lifecycleScope.launch {
            val routes = routeViewModel.getRoutes()
            setAdapterRoutes(routes)
        }
        return view
    }

    private fun displayDialogWhenRecyclerShown() {
        val listener = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                if (shouldDisplayHowToShowRouteDialog && isVisible && top != 0 && v != null) {
                    var verticalOffset = recyclerView.height
                    verticalOffset -= resources.getDimension(R.dimen.collapsed_panel_height).toInt()
                    verticalOffset -= resources.getDimension(R.dimen.toolbar_height).toInt()

                    InformativeDialogs.displayHowToShowRouteDialog(v.context, verticalOffset) {
                        InformativeDialogs.howToShowRouteDialogDisplayed(v.context)
                        shouldDisplayHowToShowRouteDialog = false
                    }
                    recyclerView.removeOnLayoutChangeListener(this)
                }
            }
        }
        recyclerView.addOnLayoutChangeListener(listener)
    }

    private fun removeSearchViewBackground() {
        try {
            val searchPlateId: Int =
                searchView.context.resources.getIdentifier("android:id/search_plate", null, null)
            val searchPlate = searchView.findViewById<View>(searchPlateId)
            searchPlate.setBackgroundColor(Color.TRANSPARENT)

        } catch (exception: Exception) {
            Log.e(TAG, "Error on removeSearchViewBackground", exception)
        }
    }

    private fun filter(models: List<RouteModel>, query: String): List<RouteModel> {
        val lowerCaseQuery = query.lowercase(getDefault())
        val filteredList = ArrayList<RouteModel>()
        for (model in models) {
            val name = model.name.lowercase(getDefault())
            if (name.contains(lowerCaseQuery) || model.routeDb.shortName.contains(lowerCaseQuery))
                filteredList.add(model)
        }

        return filteredList
    }

    private fun setAdapterRoutes(data: List<RouteModel>) {
        routeModels = data
        recyclerView.setHasFixedSize(false)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RouteListFilterableAdapter(this, comparator).apply { add(routeModels) }
        recyclerView.adapter = adapter
    }

    fun setHeight(height: Int) {
        view?.layoutParams =
            RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
    }

    override fun onDetach() {
        clearDrawnRoutes()
        super.onDetach()
    }

    override fun drawRoute(route: RouteModel) {
        drawnRoutes.add(route)
        interactionsListener.drawRoute(route)
    }

    private fun clearDrawnRoutes() = drawnRoutes.forEach { it.remove() }

    companion object {
        const val TAG = "AllRoutesFragment"

        @JvmStatic
        fun newInstance(interactionListener: InteractionsInterface) = AllRoutesFragment().apply {
            interactionsListener = interactionListener
            arguments = Bundle().apply {
            }
        }
    }

    interface InteractionsInterface : RouteListFilterableAdapter.DrawRouteListener

}
