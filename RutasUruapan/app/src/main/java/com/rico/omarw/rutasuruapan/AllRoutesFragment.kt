package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rico.omarw.rutasuruapan.database.AppDatabase


class AllRoutesFragment : Fragment() {

//    private var listener: Listener? = null
    private lateinit var recyclerView: RecyclerView
    private var adapterListener: RouteListAdapter.Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_all_routes, container, false)

        recyclerView = view.findViewById(R.id.recyclerView_all_routes)

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

    fun setAdapterRoutes(data: List<RouteModel>){
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RouteListAdapter(data, adapterListener)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is RouteListAdapter.Listener) {
            adapterListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement RouteListAdapter.Listener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        adapterListener = null
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                AllRoutesFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
