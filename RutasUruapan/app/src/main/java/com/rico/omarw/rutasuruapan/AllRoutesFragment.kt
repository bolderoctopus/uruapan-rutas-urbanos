package com.rico.omarw.rutasuruapan

import android.content.Context
import android.content.DialogInterface
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rico.omarw.rutasuruapan.adapters.RouteListFilterableAdapter
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.models.RouteModel
import kotlinx.coroutines.*


class AllRoutesFragment : Fragment(), RouteListFilterableAdapter.DrawRouteListener{
    private val comparator = Comparator<RouteModel>{ routeModel1: RouteModel, routeModel2: RouteModel ->
        (routeModel1.color + routeModel1.name).compareTo((routeModel2.color + routeModel2.name))
    }
    private val queryTextListener = object : SearchView.OnQueryTextListener{
        override fun onQueryTextChange(query: String?): Boolean {
            if(query != null) {
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
    public lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private var interactionsListener: InteractionsInterface? = null
    public var onViewCreated: Runnable? = null
    private var drawnRoutes: ArrayList<RouteModel>? = null

    private var uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
        uiScope = CoroutineScope(Dispatchers.Main)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_all_routes, container, false)

        recyclerView = view.findViewById(R.id.recyclerView_all_routes)
        searchView = view.findViewById(R.id.searchview)
        searchView.setOnQueryTextListener(queryTextListener)
        removeSearchViewBackground()

        if ((activity as MainActivity).showInformativeDialog)
            addRecyclerViewLayoutListener()

        onViewCreated?.run()
        onViewCreated = null

        uiScope.launch {
            val routes = withContext(Dispatchers.IO) {getRoutes()}
            setAdapterRoutes(routes)
        }
        return view
    }

    private fun addRecyclerViewLayoutListener(){
        val listener = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                if((activity as MainActivity).showInformativeDialog && isVisible && top != 0 && v!= null){
                    InformativeDialog.show(v.context,
                            recyclerView.height, InformativeDialog.Style.Left, R.string.how_to_show_routes_message,
                            DialogInterface.OnDismissListener {(activity as MainActivity).informativeDialog1Shown()})
                    recyclerView.removeOnLayoutChangeListener(this)
                }
            }
        }
        recyclerView.addOnLayoutChangeListener(listener)
    }

    private suspend fun getRoutes(): List<RouteModel> {
        val routesList =  AppDatabase.getInstance(context!!)?.routesDAO()?.getRoutes()

        return arrayListOf<RouteModel>().apply {
            routesList?.forEach{add(RouteModel(it))}
        }
    }

    private fun removeSearchViewBackground(){
        try{
            val searchPlateId: Int = searchView.context.resources.getIdentifier("android:id/search_plate", null, null)
            val searchPlate = searchView.findViewById<View>(searchPlateId)
            searchPlate.setBackgroundColor(Color.TRANSPARENT)

        }catch (exception: Exception){
            Log.e(TAG, "Error on removeSearchViewBackground", exception)
        }
    }

    fun filter(models: List<RouteModel>, query: String): List<RouteModel>{
        val lowerCaseQuery = query.toLowerCase()
        val filteredList = ArrayList<RouteModel>()
        for(model in models){
            val name = model.name.toLowerCase()
            if(name.contains(lowerCaseQuery) || model.routeDb.shortName.contains(lowerCaseQuery))
                filteredList.add(model)
        }

        return filteredList
    }

    fun setAdapterRoutes(data: List<RouteModel>){
        routeModels = data
        recyclerView.setHasFixedSize(false)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RouteListFilterableAdapter(this, comparator).apply { add(routeModels) }
        recyclerView.adapter = adapter
    }

    fun setHeight(height: Int) {
        view?.layoutParams = RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is RouteListFilterableAdapter.DrawRouteListener) {
            interactionsListener = context as InteractionsInterface
        } else {
            throw RuntimeException(context.toString() + " must implement RouteListFilterableAdapter.DrawRouteListener")
        }
    }

    override fun onDetach() {
        clearDrawnRoutes()
        uiScope.cancel()
        interactionsListener = null
        super.onDetach()
    }

    override fun drawRoute(route: RouteModel) {//todo: don't they get added twice?
        if(drawnRoutes == null) drawnRoutes = ArrayList()
        drawnRoutes!!.add(route)
        interactionsListener?.drawRoute(route)
    }

    private fun clearDrawnRoutes() = drawnRoutes?.forEach{it.remove()}// todo: this might be no longer needed since the fragment remains attached the whole time?

    companion object {
        val TAG = "AllRoutesFragment"
        @JvmStatic
        fun newInstance() =AllRoutesFragment().apply {
                arguments = Bundle().apply {
                }
        }
    }

    interface InteractionsInterface : RouteListFilterableAdapter.DrawRouteListener

}
