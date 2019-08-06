package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rico.omarw.rutasuruapan.database.AppDatabase
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
    lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private var interactionsListener: InteractionsInterface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_all_routes, container, false)

        recyclerView = view.findViewById(R.id.recyclerView_all_routes)
        searchView = view.findViewById(R.id.searchview)
        searchView.setOnQueryTextListener(queryTextListener)
        searchView.setOnFocusChangeListener{ sender, hasFocus ->
            run {
                if (hasFocus)
                    interactionsListener?.onSearchGotFocus()
            }
        }

        AsyncTask.execute{
            if(context != null) {
                val routesList = AppDatabase.getInstance(context!!)?.routesDAO()?.getRoutes()
                val adapterItems = arrayListOf<RouteModel>()
                routesList?.forEach{
                    adapterItems.add(RouteModel(it))
           }
                activity?.runOnUiThread{setAdapterRoutes(adapterItems)}
            }
        }

        return view
    }

    fun filter(models: List<RouteModel> ,query: String): List<RouteModel>{
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
        super.onDetach()
        interactionsListener = null
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                AllRoutesFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }

    interface InteractionsInterface : RouteListFilterableAdapter.DrawRouteListener{
        fun onSearchGotFocus()
    }

}
