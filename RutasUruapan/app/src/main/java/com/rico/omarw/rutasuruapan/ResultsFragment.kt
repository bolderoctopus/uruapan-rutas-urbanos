package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.rico.omarw.rutasuruapan.adapters.RouteListAdapter
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.models.RouteModel
import kotlin.math.sqrt

class ResultsFragment : Fragment(){

    lateinit var recyclerView: RecyclerView
    private var listener: OnFragmentInteractionListener? = null
    private var height: Int? = null
    public var onViewCreated: Runnable? = null
    private lateinit var originLatLng: LatLng
    private lateinit var destinationLatLng: LatLng
    private var tolerance: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            height = it.getInt(HEIGHT_KEY)
            originLatLng = it.getParcelable(ORIGIN_LATLNG_KEY)
            destinationLatLng = it.getParcelable(DESTINATION_LATLNG_KEY)
            tolerance = it.getDouble(TOLERANCE_KEY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_results, container, false)
        recyclerView = view.findViewById(R.id.recyclerView_results)
        view.findViewById<ImageButton>(R.id.imagebutton_back).setOnClickListener{run{listener?.onBackFromResults()}}
        view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, if(this.height == null) ViewGroup.LayoutParams.MATCH_PARENT else this.height!!)

        onViewCreated?.run()
        onViewCreated = null

        if(context != null)
            findRouteAsync(originLatLng, destinationLatLng, tolerance!!, context!!, this::displayRoutes)

        return view
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

    // my methods
    //todo: take in consideration streets
    private fun distanceBetweenPoints(from: LatLng, to: LatLng): Double {
        val d1 = (from.latitude - to.latitude)
        val d2 = (from.longitude - to.longitude)
        return sqrt(d1 * d1 + d2 * d2)
    }

    //todo: if the distance that you need to walk to the bus stop is greater or equal than the distance, to the other point then maybe you should walk
    private fun findRouteAsync(originLatLng: LatLng, destinationLatLng: LatLng, tolerance: Double, c: Context, onCompletion: (results: ArrayList<RouteModel>) -> Unit){
        var walkingDistanceTolerance = tolerance

        if(walkingDistanceTolerance < 0) throw Exception("")

        val walkingDistToDest = distanceBetweenPoints(originLatLng, destinationLatLng)
        AsyncTask.execute{
            val routesDao = AppDatabase.getInstance(c)?.routesDAO()
            var mutualRoutes: Set<Long>? = null
            while(walkingDistToDest > walkingDistanceTolerance * 2){
                Log.d("findRoute", "walkingDistToDest: $walkingDistToDest walkingDistanceTolerance: $walkingDistanceTolerance")
                val routesNearOrigin = routesDao?.getRoutesIntercepting(walkingDistanceTolerance, originLatLng.latitude, originLatLng.longitude)
                val routesNearDest = routesDao?.getRoutesIntercepting(walkingDistanceTolerance, destinationLatLng.latitude, destinationLatLng.longitude)
                walkingDistanceTolerance += WALKING_DISTANCE_INCREMENT

                if(routesNearDest.isNullOrEmpty() || routesNearOrigin.isNullOrEmpty()) continue
                mutualRoutes = routesNearOrigin.intersect(routesNearDest)
                if(mutualRoutes.size > MAX_AMOUNT_ROUTES) break
            }

            val results = arrayListOf<RouteModel>()
            if(!mutualRoutes.isNullOrEmpty()){
                val routesInfo = routesDao?.getRoutes(mutualRoutes.toList())
                routesInfo?.forEach{results.add(RouteModel(it))}
            }
            //todo: find a better way to call it
            view?.post {

                onCompletion.invoke(results)
            }
        }
    }

    private fun displayRoutes(results: ArrayList<RouteModel>){
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RouteListAdapter(results, null)
    }

    interface OnFragmentInteractionListener{
        fun onBackFromResults()
    }

    companion object{
        private const val HEIGHT_KEY = "height"
        private const val ORIGIN_LATLNG_KEY = "originlatlng"
        private const val DESTINATION_LATLNG_KEY = "destinationlatlng"
        private const val TOLERANCE_KEY = "tolerance"
        val TAG = "ResultsFragment"
        @JvmStatic
        fun newInstance(height: Int, originLatLng: LatLng, destinationLatLng: LatLng, tolerance: Double) = ResultsFragment().apply {
                arguments = Bundle().apply{
                    putInt(HEIGHT_KEY, height)
                    putParcelable(ORIGIN_LATLNG_KEY, originLatLng)
                    putParcelable(DESTINATION_LATLNG_KEY, destinationLatLng)
                    putDouble(TOLERANCE_KEY, tolerance)
                }
//            enterTransition = Slide(Gravity.RIGHT)
//            exitTransition = Slide(Gravity.LEFT)
        }
    }

}