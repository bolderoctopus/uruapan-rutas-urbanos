package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rico.omarw.rutasuruapan.adapters.RouteListFilterableAdapter
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.models.RouteModel
import kotlinx.coroutines.*
import java.lang.Runnable
import kotlin.Comparator


class AllRoutesFragment : Fragment(){
    private val comparator = Comparator<RouteModel>{ routeModel1: RouteModel, routeModel2: RouteModel ->
        routeModel1.name.compareTo(routeModel2.name)
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
    private var height: Int? = null
    public var onViewCreated: Runnable? = null

    private var uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            height = it.getInt(HEIGHT_KEY)
        }
        uiScope = CoroutineScope(Dispatchers.Main)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_all_routes, container, false)

        recyclerView = view.findViewById(R.id.recyclerView_all_routes)
        searchView = view.findViewById(R.id.searchview)
        searchView.setOnQueryTextListener(queryTextListener)

        onViewCreated?.run()
        onViewCreated = null

        uiScope.launch {
            val routes = async (Dispatchers.IO){getRoutes()}.await()
            setAdapterRoutes(routes)
        }

        view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, if(height == null) ViewGroup.LayoutParams.MATCH_PARENT else height!!)
        return view
    }

    private suspend fun getRoutes(): List<RouteModel> {
        val routesList =  AppDatabase.getInstance(context!!)?.routesDAO()?.getRoutes()

        return arrayListOf<RouteModel>().apply {
            routesList?.forEach{add(RouteModel(it))}
        }
    }


    fun filter(models: List<RouteModel>, query: String): List<RouteModel>{
        val lowerCaseQuery = query.toLowerCase()
        val filteredList = ArrayList<RouteModel>()
        for(model in models){
            val name = model.name.toLowerCase()
            if(name.contains(lowerCaseQuery))
                filteredList.add(model)
        }

        return filteredList
    }

    fun setAdapterRoutes(data: List<RouteModel>){
        routeModels = data
        recyclerView.setHasFixedSize(false)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RouteListFilterableAdapter(interactionsListener, comparator).apply { add(routeModels) }
        recyclerView.adapter = adapter
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
        uiScope.cancel()
        interactionsListener = null
        super.onDetach()
    }

    companion object {
        const val HEIGHT_KEY = "height"
        val TAG = "AllRoutesFragment"
        @JvmStatic
        fun newInstance(height: Int) =
                AllRoutesFragment().apply {
                    arguments = Bundle().apply {
                        putInt(HEIGHT_KEY, height)
                    }
                }
    }

    interface InteractionsInterface : RouteListFilterableAdapter.DrawRouteListener

}
