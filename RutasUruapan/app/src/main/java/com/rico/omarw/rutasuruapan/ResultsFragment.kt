package com.rico.omarw.rutasuruapan

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.rico.omarw.rutasuruapan.Constants.DEBUG_TAG
import com.rico.omarw.rutasuruapan.adapters.RouteListAdapter
import com.rico.omarw.rutasuruapan.database.AppDatabase
import com.rico.omarw.rutasuruapan.database.Point
import com.rico.omarw.rutasuruapan.database.Route
import com.rico.omarw.rutasuruapan.models.RouteModel
import kotlinx.coroutines.*
import kotlin.math.sqrt

class ResultsFragment : Fragment(), RouteListAdapter.DrawRouteListener{

    lateinit var recyclerView: RecyclerView
    private lateinit var groupWalkMessage: Group
    private var listener: OnFragmentInteractionListener? = null
    private var height: Int? = null
    public var onViewCreated: Runnable? = null
    private lateinit var originLatLng: LatLng
    private lateinit var destinationLatLng: LatLng
    private var drawnRoutes: ArrayList<RouteModel>? = null

    private var uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            height = it.getInt(HEIGHT_KEY)
            originLatLng = it.getParcelable(ORIGIN_LATLNG_KEY)
            destinationLatLng = it.getParcelable(DESTINATION_LATLNG_KEY)
        }

        if(!uiScope.isActive)
            uiScope = CoroutineScope(Dispatchers.Main)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_results, container, false)
        groupWalkMessage = view.findViewById(R.id.group_walk_message)
        recyclerView = view.findViewById(R.id.recyclerView_results)
        view.findViewById<ImageButton>(R.id.imagebutton_back).setOnClickListener{
            backButtonPressed()
        }
        if(height != null)
            view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height!!)

        onViewCreated?.run()
        onViewCreated = null

        findRoutesAsync(originLatLng, destinationLatLng, getWalkDistLimit())

        return view
    }

    fun backButtonPressed(){
        clearDrawnRoutes()
        listener?.onBackFromResults()
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
        clearDrawnRoutes()
        uiScope.cancel()
        listener = null
        super.onDetach()
    }

    //todo: take in consideration streets
    private fun distanceBetweenPoints(p1: LatLng, p2: LatLng): Double {
        val d1 = (p1.latitude - p2.latitude)
        val d2 = (p1.longitude - p2.longitude)
        return sqrt(d1 * d1 + d2 * d2)
    }

    /**
     *
     * @param originLatLng Start point
     * @param destinationLatLng End point
     * @param walkDistLimit How much the user is willing to walk between the route start/end point and the given origin/destination respectively
     */
    private fun findRoutesAsync(originLatLng: LatLng, destinationLatLng: LatLng, walkDistLimit: Double){
//        if(walkDistLimit < 1) throw Exception("walkDistLimit must be a greater than 1")
        listener?.drawSquares(walkDistLimit)
        val walkDistToDest = distanceBetweenPoints(originLatLng, destinationLatLng)

        uiScope.launch {
            val routesDao = AppDatabase.getInstance(context!!)?.routesDAO()
            var commonRoutesIds: Set<Long>? = null

            val routesNearOrigin = async(Dispatchers.IO) { routesDao?.getRoutesIntercepting(walkDistLimit, originLatLng.latitude, originLatLng.longitude)}
            val routesNearDest = async(Dispatchers.IO) { routesDao?.getRoutesIntercepting(walkDistLimit, destinationLatLng.latitude, destinationLatLng.longitude)}
            awaitAll(routesNearDest, routesNearOrigin)

            //if(routesNearDest.await().isNullOrEmpty() || routesNearOrigin.await().isNullOrEmpty())// todo: suggest to increase WalkDistLimit?
            commonRoutesIds = routesNearOrigin.await()!!.intersect(routesNearDest.await()!!)

            if(commonRoutesIds.isNullOrEmpty()){
                 showNoResultsMessage()// todo: suggest to increase WalkDistLimit?
            }
            else{
                val results = arrayListOf<RouteModel>()
                for(routeId in commonRoutesIds){
                    val route: Route? = withContext(Dispatchers.IO){ routesDao?.getRoute(routeId)}

                    var startPoint: Point? = withContext(Dispatchers.IO){routesDao?.getNearestPointTo(routeId, originLatLng.latitude, originLatLng.longitude, walkDistLimit)}
                    val endPoint: Point? = withContext(Dispatchers.IO){routesDao?.getNearestPointTo(routeId, destinationLatLng.latitude, destinationLatLng.longitude, walkDistLimit)}

                    val betterEndPoint: Point? = withContext(Dispatchers.IO){routesDao?.findBestEndPoint(routeId, destinationLatLng.latitude, destinationLatLng.longitude, startPoint!!.number, walkDistLimit)}
                    val betterStartPoint  = withContext(Dispatchers.IO){routesDao?.findBestStartPoint(routeId, originLatLng.latitude, originLatLng.longitude, endPoint!!.number, walkDistLimit)}

                    val routeDist = withContext(Dispatchers.IO){ routesDao?.getRouteDist(routeId, betterStartPoint!!.number, betterEndPoint!!.number)} ?: 0

                    val routeModel = RouteModel(route!!)
                    routeModel.startPoint = betterStartPoint
                    routeModel.endPoint = betterEndPoint
                    routeModel.walkDist = distanceBetweenPoints(originLatLng, betterStartPoint!!.getLatLng()) + distanceBetweenPoints(destinationLatLng, betterEndPoint!!.getLatLng())
                    routeModel.totalDist = routeModel.walkDist!! + routeDist
                    results.add(routeModel)

                    Log.d(DEBUG_TAG,"__________")
                    Log.d(DEBUG_TAG, "route: ${route.name}, routeId: ${route.routeId}")
                    Log.d(DEBUG_TAG, "origin: latLng= ${originLatLng.latitude}, ${originLatLng.longitude}")
                    Log.d(DEBUG_TAG, "destination: latLng= ${destinationLatLng.latitude}, ${destinationLatLng.longitude}")
                    Log.d(DEBUG_TAG, "startPoint: #${startPoint!!.number} ,latLng= ${startPoint.lat}, ${startPoint.lng}")
                    Log.d(DEBUG_TAG, "endPoint: #${endPoint!!.number} ,latLng= ${endPoint.lat}, ${endPoint.lng}")
                    Log.d(DEBUG_TAG , "betterStartPoint: ${betterStartPoint?.number}")
                    Log.d(DEBUG_TAG , "betterEndPoint: ${betterEndPoint?.number}")
                    Log.d(DEBUG_TAG, "routeWalkDist: ${routeModel.walkDist}")
                    Log.d(DEBUG_TAG, "routeTotalDist: ${routeModel.totalDist}")
//                    break
                }
                results.sortBy {
                    it.walkDist
                }

                displayRoutes(results)
            }

        }
    }

    private fun clearDrawnRoutes() = drawnRoutes?.forEach{it.remove()}

    private fun displayRoutes(results: ArrayList<RouteModel>){
        groupWalkMessage.visibility = View.GONE

        recyclerView.visibility = View.VISIBLE
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RouteListAdapter(results, this)
    }

    private fun showNoResultsMessage(){
        recyclerView.visibility = View.GONE
        groupWalkMessage.visibility = View.VISIBLE
    }

    override fun drawRouteResult(route: RouteModel) {
        if(drawnRoutes == null) drawnRoutes = ArrayList()
        drawnRoutes!!.add(route)
        listener?.drawRouteResult(route)
    }

    fun startUpdate(){
        recyclerView.visibility = View.INVISIBLE
        clearDrawnRoutes()
    }

    fun endUpdate(origin: LatLng, destination: LatLng){
        findRoutesAsync(origin, destination, getWalkDistLimit())
    }


    fun getWalkDistLimit() : Double {
        val s = PreferenceManager.getDefaultSharedPreferences(context).getString("walk_dist_limit", Constants.WALK_DIST_LIMIT_DEFFAULT.toString())
        return s?.toDouble() ?: Constants.WALK_DIST_LIMIT_DEFFAULT

    }


    interface OnFragmentInteractionListener {
        fun onBackFromResults()
        fun drawRouteResult(route: RouteModel)
        fun drawSquares(walkingDistance: Double)
    }

    companion object{
        private const val HEIGHT_KEY = "height"
        private const val ORIGIN_LATLNG_KEY = "originlatlng"
        private const val DESTINATION_LATLNG_KEY = "destinationlatlng"
        val TAG = "ResultsFragment"
        @JvmStatic
        fun newInstance(height: Int, originLatLng: LatLng, destinationLatLng: LatLng) = ResultsFragment().apply {
                arguments = Bundle().apply{
                    putInt(HEIGHT_KEY, height)
                    putParcelable(ORIGIN_LATLNG_KEY, originLatLng)
                    putParcelable(DESTINATION_LATLNG_KEY, destinationLatLng)
                }
//            enterTransition = Slide(Gravity.RIGHT)
//            exitTransition = Slide(Gravity.LEFT)
        }
    }

}