package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rico.omarw.rutasuruapan.adapters.RouteListAdapter
import com.rico.omarw.rutasuruapan.models.RouteModel


class ControlPanelFragment : Fragment() {

    private var contolPanelListener: Listener? = null
    private var routesAdapterDrawRouteListener: RouteListAdapter.DrawRouteListener? = null
    private lateinit var originTextView: TextView
    private lateinit var destinationTextView: TextView
    private lateinit var distanceEditText: EditText
    lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var  buttonFindRoute: Button
    private lateinit var  buttonClear: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_control_panel, container, false)

        originTextView = view.findViewById(R.id.textview_origin)
        destinationTextView = view.findViewById(R.id.textView_destination)
        distanceEditText = view.findViewById(R.id.editText_distance)
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        buttonFindRoute = view.findViewById(R.id.button_find_routes)
        buttonClear = view.findViewById(R.id.button_clear)

        buttonFindRoute.setOnClickListener { contolPanelListener?.findRoute() }
        buttonClear.setOnClickListener { contolPanelListener?.clear() }

        activateLoadingMode(false)

//        AsyncTask.execute{
//            if(context != null) {
//                val routesList = AppDatabase.getInstance(context!!)?.routesDAO()?.getRoutes()
//                val adapterItems = arrayListOf<RouteModel>()
//                routesList?.forEach{
//                    adapterItems.add(RouteModel(it))
//           }
//                activity?.runOnUiThread{setAdapterRoutes(adapterItems)}
//            }
//        }

        return view
    }

    fun setAdapterRoutes(data: List<RouteModel>){
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RouteListAdapter(data, routesAdapterDrawRouteListener)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            contolPanelListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement DrawRouteListener")
        }

        if (context is RouteListAdapter.DrawRouteListener) {
            routesAdapterDrawRouteListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement routesAdapterDrawRouteListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        contolPanelListener = null
        routesAdapterDrawRouteListener = null
    }

    fun setOriginDestinationText(origin: String?, destination: String?){
        originTextView.text = origin
        destinationTextView.text = destination
    }

    fun getWalkingDistTolerance(): Double?{
        return distanceEditText.text.toString().toDoubleOrNull()
    }

    fun setDistanceToBusStop(distance: Double){
        distanceEditText.setText(distance.toString())
    }

    fun activateLoadingMode(activate: Boolean){
        if(activate){
            progressBar.visibility = View.VISIBLE
            buttonFindRoute.isEnabled = false
            distanceEditText.isEnabled = false
        }
        else{
            progressBar.visibility = View.GONE
            buttonFindRoute.isEnabled = true
            distanceEditText.isEnabled = true
        }
    }

    fun clearRoutes() {
        if(recyclerView.adapter  == null) return

        val currentItems = (recyclerView.adapter as RouteListAdapter).getItems()

        for(r in currentItems){
            r.polyline?.remove()
        }
    }

    interface Listener {
        fun findRoute()
        fun clear()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                ControlPanelFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
