package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rico.omarw.rutasuruapan.Database.AppDatabase


class ControlPanel : Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private lateinit var originTextView: TextView
    private lateinit var destinationTextView: TextView
    private lateinit var distanceEditText: EditText
    private lateinit var recyclerView: RecyclerView


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

        view.findViewById<Button>(R.id.button_find_routes).setOnClickListener { listener?.findRoute() }

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
        recyclerView.adapter = RouteListAdapter(data, listener)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    fun setOriginDestinationText(origin: String?, destination: String?){
        originTextView.text = origin
        destinationTextView.text = destination
    }

    fun getDistance(): Double?{
        return distanceEditText.text.toString().toDoubleOrNull()
    }

    interface OnFragmentInteractionListener {
        fun findRoute()
        fun drawRoute(route: RouteModel)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                ControlPanel().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}