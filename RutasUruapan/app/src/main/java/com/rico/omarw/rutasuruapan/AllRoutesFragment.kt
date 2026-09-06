package com.rico.omarw.rutasuruapan

import android.content.Context
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rico.omarw.rutasuruapan.adapters.RouteListFilterableAdapter
import com.rico.omarw.rutasuruapan.models.RouteModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllRoutesFragment : Fragment(), RouteListFilterableAdapter.DrawRouteListener {
    private val routeViewModel: RouteViewModel by activityViewModels()

    private val comparator =
        Comparator<RouteModel> { routeModel1: RouteModel, routeModel2: RouteModel ->
            (routeModel1.color + routeModel1.name).compareTo((routeModel2.color + routeModel2.name))
        }
    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextChange(query: String?): Boolean {
            query?.let { routeViewModel.filterRoutes(it) }
            return true
        }

        override fun onQueryTextSubmit(p0: String?) = false
    }

    private val adapter: RouteListFilterableAdapter = RouteListFilterableAdapter(this, comparator)
    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView

    private lateinit var interactionsListener: InteractionsInterface
    private var shouldDisplayHowToShowRouteDialog = true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is InteractionsInterface) {
            interactionsListener = context
        } else {
            throw RuntimeException("$context must implement InteractionsInterface")
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

        recyclerView.setHasFixedSize(false)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        shouldDisplayHowToShowRouteDialog =
            InformativeDialogs.shouldDisplayHowToShowRouteDialog(layoutInflater.context)

        if (shouldDisplayHowToShowRouteDialog)
            displayDialogWhenRecyclerShown()


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                routeViewModel.filterableRoutes.collect {
                    if (adapter.itemCount == 0) adapter.add(it)
                    else {
                        adapter.replaceAll(it)
                        recyclerView.scrollToPosition(0)
                    }
                }
            }
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
                if (shouldDisplayHowToShowRouteDialog && isVisible && top != 0 && v != null && InformativeDialogs.shouldDisplayHowToShowRouteDialog(v.context)) {
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

    fun setHeight(height: Int) {
        view?.layoutParams =
            RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
    }

    override fun onDetach() {
        clearDrawnRoutes()
        super.onDetach()
    }

    override fun drawRoute(route: RouteModel) {
        routeViewModel.addDrawnRoute(route)
        interactionsListener.drawRoute(route)
    }

    private fun clearDrawnRoutes() = routeViewModel.clearDrawnRoutes()

    companion object {
        const val TAG = "AllRoutesFragment"

        @JvmStatic
        fun newInstance() = AllRoutesFragment().apply {
            arguments = Bundle().apply {
            }
        }
    }

    interface InteractionsInterface : RouteListFilterableAdapter.DrawRouteListener

}
